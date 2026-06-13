package at.minich.opserver.plots;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 32x32 plot with 3-block roads, opsucht-style.
 * Grid step = 35 (32 plot + 3 road).
 */
public class Plot {

    public static final int PLOT_SIZE = 32;
    public static final int ROAD_WIDTH = 3;
    public static final int GRID_STEP = PLOT_SIZE + ROAD_WIDTH; // 35
    public static final int GROUND_Y = 4;  // grass block y in flat world
    public static final int Y_MIN = 5;     // first buildable layer
    public static final int Y_MAX = 256;

    private final int id;
    private final int gridX;
    private final int gridZ;
    private final UUID owner;
    private final List<UUID> trusted;

    public Plot(int id, int gridX, int gridZ, UUID owner) {
        this.id = id; this.gridX = gridX; this.gridZ = gridZ;
        this.owner = owner; this.trusted = new ArrayList<>();
    }

    public Plot(int id, int gridX, int gridZ, UUID owner, List<UUID> trusted) {
        this.id = id; this.gridX = gridX; this.gridZ = gridZ;
        this.owner = owner; this.trusted = new ArrayList<>(trusted);
    }

    public int getId() { return id; }
    public int getGridX() { return gridX; }
    public int getGridZ() { return gridZ; }
    public UUID getOwner() { return owner; }
    public List<UUID> getTrusted() { return trusted; }

    /** World-space X of western edge (first plot block, after road). */
    public int getWorldMinX() { return gridX * GRID_STEP; }
    public int getWorldMinZ() { return gridZ * GRID_STEP; }
    public int getWorldMaxX() { return getWorldMinX() + PLOT_SIZE - 1; }
    public int getWorldMaxZ() { return getWorldMinZ() + PLOT_SIZE - 1; }

    public double getTeleportX() { return getWorldMinX() + PLOT_SIZE / 2.0; }
    public double getTeleportZ() { return getWorldMinZ() + PLOT_SIZE / 2.0; }

    public boolean contains(int x, int y, int z) {
        return x >= getWorldMinX() && x <= getWorldMaxX()
                && z >= getWorldMinZ() && z <= getWorldMaxZ();
    }

    public boolean isTrusted(UUID uuid) {
        return uuid.equals(owner) || trusted.contains(uuid);
    }

    public void addTrusted(UUID uuid) { if (!trusted.contains(uuid)) trusted.add(uuid); }
    public boolean removeTrusted(UUID uuid) { return trusted.remove(uuid); }

    @Override
    public String toString() { return "Plot#" + id + " [" + gridX + "," + gridZ + "] owner=" + owner; }
}
