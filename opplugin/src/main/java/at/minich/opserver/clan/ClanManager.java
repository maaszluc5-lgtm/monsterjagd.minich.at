package at.minich.opserver.clan;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class ClanManager {

    private final OpServerPlugin plugin;
    private final File clanFile;
    private YamlConfiguration cfg;

    // In-memory: clanName (lowercase) -> Clan
    private final Map<String, Clan> clans = new HashMap<>();
    // Invites: target uuid -> clan name (lowercase)
    private final Map<UUID, String> pendingInvites = new HashMap<>();

    public ClanManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.clanFile = new File(plugin.getDataFolder(), "clans.yml");
        load();
    }

    public void load() {
        clans.clear();
        if (!clanFile.exists()) {
            cfg = new YamlConfiguration();
            return;
        }
        cfg = YamlConfiguration.loadConfiguration(clanFile);
        if (!cfg.contains("clans")) return;

        for (String key : cfg.getConfigurationSection("clans").getKeys(false)) {
            String path = "clans." + key;
            String name = cfg.getString(path + ".name", key);
            UUID leader = UUID.fromString(cfg.getString(path + ".leader"));
            String createdAt = cfg.getString(path + ".created-at", Instant.now().toString());

            List<UUID> members = new ArrayList<>();
            for (String m : cfg.getStringList(path + ".members")) {
                try { members.add(UUID.fromString(m)); } catch (IllegalArgumentException ignored) {}
            }
            clans.put(key.toLowerCase(), new Clan(name, leader, members, createdAt));
        }
    }

    public void save() {
        cfg = new YamlConfiguration();
        for (Map.Entry<String, Clan> entry : clans.entrySet()) {
            String path = "clans." + entry.getKey().toLowerCase();
            Clan clan = entry.getValue();
            cfg.set(path + ".name", clan.getName());
            cfg.set(path + ".leader", clan.getLeader().toString());
            cfg.set(path + ".created-at", clan.getCreatedAt());
            List<String> memberStrings = new ArrayList<>();
            for (UUID m : clan.getMembers()) memberStrings.add(m.toString());
            cfg.set(path + ".members", memberStrings);
        }
        try {
            cfg.save(clanFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save clans.yml: " + e.getMessage());
        }
    }

    public boolean clanExists(String name) {
        return clans.containsKey(name.toLowerCase());
    }

    public Clan getClan(String name) {
        return clans.get(name.toLowerCase());
    }

    public Clan getClanOfPlayer(UUID uuid) {
        for (Clan clan : clans.values()) {
            if (clan.getMembers().contains(uuid)) return clan;
        }
        return null;
    }

    public boolean createClan(String name, UUID leader) {
        if (clanExists(name)) return false;
        if (getClanOfPlayer(leader) != null) return false;
        List<UUID> members = new ArrayList<>();
        members.add(leader);
        Clan clan = new Clan(name, leader, members, Instant.now().toString());
        clans.put(name.toLowerCase(), clan);
        save();
        return true;
    }

    public boolean disbandClan(String name) {
        if (!clanExists(name)) return false;
        clans.remove(name.toLowerCase());
        save();
        return true;
    }

    public void addInvite(UUID target, String clanName) {
        pendingInvites.put(target, clanName.toLowerCase());
    }

    public String getInvite(UUID target) {
        return pendingInvites.get(target);
    }

    public void removeInvite(UUID target) {
        pendingInvites.remove(target);
    }

    public Collection<Clan> getAllClans() {
        return clans.values();
    }

    public Map<String, Clan> getClansMap() {
        return Collections.unmodifiableMap(clans);
    }
}
