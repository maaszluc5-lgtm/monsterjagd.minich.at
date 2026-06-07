package at.minich.opserver.plots;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single 16x16 plot in the plot world.
 * The plot occupies columns (gridX*16) to (gridX*16+15) and (gridZ*16) to (gridZ*16+15),
 * at y=60 to y=120.
 */
public class Plot {

    public static final int PLOT_SIZE = 16;
    public static final int Y_MIN = 60;
    public static final int Y_MAX = 120;

    private final int id;         // sequential plot id (0-based)
    private final int gridX;      // grid column
    private final int gridZ;      // grid row
    private final UUID owner;
    private final List<UUID> trusted;

    public Plot(int id, int gridX, int gridZ, UUID owner) {
        this.id = id;
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.owner = owner;
        this.trusted = new ArrayList<>();
    }

    public Plot(int id, int gridX, int gridZ, UUID owner, List<UUID> trusted) {
        this.id = id;
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.owner = owner;
        this.trusted = new ArrayList<>(trusted);
    }

    public int getId() { return id; }
    public int getGridX() { return gridX; }
    public int getGridZ() { return gridZ; }
    public UUID getOwner() { return owner; }
    public List<UUID> getTrusted() { return trusted; }

    /** World-space X coordinate of the plot's western edge. */
    public int getWorldMinX() { return gridX * PLOT_SIZE; }
    /** World-space Z coordinate of the plot's northern edge. */
    public int getWorldMinZ() { return gridZ * PLOT_SIZE; }
    /** World-space X coordinate of the plot's eastern edge (inclusive). */
    public int getWorldMaxX() { return getWorldMinX() + PLOT_SIZE - 1; }
    /** World-space Z coordinate of the plot's southern edge (inclusive). */
    public int getWorldMaxZ() { return getWorldMinZ() + PLOT_SIZE - 1; }

    /** Teleport X – centre of plot. */
    public double getTeleportX() { return getWorldMinX() + PLOT_SIZE / 2.0; }
    /** Teleport Z – centre of plot. */
    public double getTeleportZ() { return getWorldMinZ() + PLOT_SIZE / 2.0; }

    public boolean contains(int x, int y, int z) {
        return x >= getWorldMinX() && x <= getWorldMaxX()
                && z >= getWorldMinZ() && z <= getWorldMaxZ()
                && y >= Y_MIN && y <= Y_MAX;
    }

    public boolean isTrusted(UUID uuid) {
        return uuid.equals(owner) || trusted.contains(uuid);
    }

    public void addTrusted(UUID uuid) {
        if (!trusted.contains(uuid)) trusted.add(uuid);
    }

    public boolean removeTrusted(UUID uuid) {
        return trusted.remove(uuid);
    }

    @Override
    public String toString() {
        return "Plot#" + id + " [" + gridX + "," + gridZ + "] owner=" + owner;
    }
}
