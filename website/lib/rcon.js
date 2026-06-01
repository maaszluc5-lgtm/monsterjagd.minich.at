const net = require('net');

class RconClient {
  constructor(host, port, password) {
    this.host = host;
    this.port = port;
    this.password = password;
    this.socket = null;
    this.requestId = 1;
    this.connected = false;
    this.authenticated = false;
    this._callbacks = new Map();
    this._buffer = Buffer.alloc(0);
  }

  connect() {
    return new Promise((resolve, reject) => {
      this.socket = new net.Socket();
      this.socket.setTimeout(10000);

      this.socket.on('data', (data) => {
        this._buffer = Buffer.concat([this._buffer, data]);
        this._processBuffer();
      });

      this.socket.on('error', (err) => {
        reject(err);
      });

      this.socket.on('timeout', () => {
        this.socket.destroy();
        reject(new Error('RCON connection timed out'));
      });

      this.socket.on('close', () => {
        this.connected = false;
        this.authenticated = false;
      });

      this.socket.connect(this.port, this.host, () => {
        this.connected = true;
        resolve();
      });
    });
  }

  _processBuffer() {
    while (this._buffer.length >= 4) {
      const length = this._buffer.readInt32LE(0);
      // total packet = 4 (length field) + length bytes
      if (this._buffer.length < 4 + length) break;

      const requestId = this._buffer.readInt32LE(4);
      const type = this._buffer.readInt32LE(8);
      // payload is between byte 12 and length+4-2 (excluding two null terminators)
      const payload = this._buffer.slice(12, 4 + length - 2).toString('utf8');

      this._buffer = this._buffer.slice(4 + length);

      const cb = this._callbacks.get(requestId);
      if (cb) {
        this._callbacks.delete(requestId);
        cb(null, { requestId, type, payload });
      }
    }
  }

  _sendPacket(type, payload) {
    return new Promise((resolve, reject) => {
      const id = this.requestId++;
      const payloadBuf = Buffer.from(payload, 'utf8');
      // length = 4 (id) + 4 (type) + payload + 2 null bytes
      const length = 4 + 4 + payloadBuf.length + 2;
      const buf = Buffer.alloc(4 + length);
      buf.writeInt32LE(length, 0);
      buf.writeInt32LE(id, 4);
      buf.writeInt32LE(type, 8);
      payloadBuf.copy(buf, 12);
      buf.writeUInt8(0, 12 + payloadBuf.length);
      buf.writeUInt8(0, 13 + payloadBuf.length);

      this._callbacks.set(id, (err, packet) => {
        if (err) return reject(err);
        resolve(packet);
      });

      this.socket.write(buf);

      // Timeout per packet
      setTimeout(() => {
        if (this._callbacks.has(id)) {
          this._callbacks.delete(id);
          reject(new Error('RCON packet timed out'));
        }
      }, 8000);
    });
  }

  async authenticate() {
    // Type 3 = SERVERDATA_AUTH
    const packet = await this._sendPacket(3, this.password);
    // On failed auth, server returns requestId = -1
    if (packet.requestId === -1) {
      throw new Error('RCON authentication failed: wrong password');
    }
    this.authenticated = true;
  }

  async sendCommand(cmd) {
    if (!this.connected) throw new Error('Not connected to RCON');
    if (!this.authenticated) throw new Error('Not authenticated with RCON');
    // Type 2 = SERVERDATA_EXECCOMMAND
    const packet = await this._sendPacket(2, cmd);
    return packet.payload;
  }

  disconnect() {
    if (this.socket) {
      this.socket.destroy();
      this.socket = null;
    }
    this.connected = false;
    this.authenticated = false;
  }
}

async function executeRconCommands(commands) {
  const host = process.env.MC_RCON_HOST || 'localhost';
  const port = parseInt(process.env.MC_RCON_PORT || '25575', 10);
  const password = process.env.MC_RCON_PASSWORD || '';

  const client = new RconClient(host, port, password);
  const results = [];

  try {
    await client.connect();
    await client.authenticate();
    for (const cmd of commands) {
      const response = await client.sendCommand(cmd);
      results.push({ cmd, response, success: true });
    }
  } catch (err) {
    results.push({ error: err.message, success: false });
  } finally {
    client.disconnect();
  }

  return results;
}

module.exports = { RconClient, executeRconCommands };
