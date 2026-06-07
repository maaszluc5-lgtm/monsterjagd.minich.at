package at.minich.opserver.plots;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.util.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * Manages all plots: loading/saving, allocation, and lookup.
 *
 * Per-player data:  plugins/OpServer/plots/<uuid>.yml
 *   plotIds: [0, 2, ...]
 *
 * World state:      plugins/OpServer/plots/world.yml
 *   nextId: 5
 *   plots:
 *     0:
 *       gridX: 0
 *       gridZ: 0
 *       owner: <uuid>
 *       trusted: [<uuid>, ...]
 */
public class PlotManager {

    public static final int MAX_PLOTS_PER_PLAYER = 3;

    private final OpServerPlugin plugin;
    private final DataManager dataManager;

    /** id -> Plot */
    private final Map<Integer, Plot> plots = new HashMap<>();

    /** owner UUID -> list of plot ids */
    private final Map<UUID, List<Integer>> ownerIndex = new HashMap<>();

    private int nextId = 0;
    private String plotWorldName = "plots";

    public PlotManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        load();
    }

    // -------------------------------------------------------------------------
    // World resolution
    // -------------------------------------------------------------------------

    public World getPlotWorld() {
        World w = Bukkit.getWorld(plotWorldName);
        if (w == null) {
            // Fallback to main (first) world
            w = Bukkit.getWorlds().get(0);
        }
        return w;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns plots owned by the given player. */
    public List<Plot> getPlotsOf(UUID uuid) {
        List<Integer> ids = ownerIndex.getOrDefault(uuid, Collections.emptyList());
        List<Plot> result = new ArrayList<>();
        for (int id : ids) {
            Plot p = plots.get(id);
            if (p != null) result.add(p);
        }
        return result;
    }

    /** Returns the number of plots a player owns. */
    public int getPlotCount(UUID uuid) {
        return ownerIndex.getOrDefault(uuid, Collections.emptyList()).size();
    }

    /** Claims the next free plot in the grid for the player. Returns the new Plot or null if limit reached. */
    public Plot claimNextPlot(UUID uuid) {
        if (getPlotCount(uuid) >= MAX_PLOTS_PER_PLAYER) return null;

        int id = nextId++;
        int[] gridPos = idToGrid(id);
        Plot plot = new Plot(id, gridPos[0], gridPos[1], uuid);
        plots.put(id, plot);
        ownerIndex.computeIfAbsent(uuid, k -> new ArrayList<>()).add(id);
        save();
        return plot;
    }

    /** Returns the plot at the given world coordinates, or null. */
    public Plot getPlotAt(int worldX, int worldY, int worldZ) {
        for (Plot p : plots.values()) {
            if (p.contains(worldX, worldY, worldZ)) return p;
        }
        return null;
    }

    /** Returns the first plot owned by this player (for /plot home). */
    public Plot getFirstPlot(UUID uuid) {
        List<Plot> owned = getPlotsOf(uuid);
        return owned.isEmpty() ? null : owned.get(0);
    }

    /** Adds trust. Returns false if player not found or already trusted. */
    public boolean addTrust(Plot plot, UUID target) {
        if (plot.isTrusted(target)) return false;
        plot.addTrusted(target);
        save();
        return true;
    }

    /** Removes trust. Returns false if target was not trusted. */
    public boolean removeTrust(Plot plot, UUID target) {
        boolean removed = plot.removeTrusted(target);
        if (removed) save();
        return removed;
    }

    // -------------------------------------------------------------------------
    // Grid layout
    // -------------------------------------------------------------------------

    /**
     * Maps a sequential plot id to a grid (x, z) pair using a spiral-free
     * row-major layout: id=0 → (0,0), id=1 → (1,0), id=2 → (2,0), ...
     * Rows are separated by a 2-block gap (gap=2 between adjacent plots).
     * Actually we space plots by PLOT_SIZE + 1 (one block border) to leave a
     * visible path. Here we just use adjacent cells; operators can add road via
     * worldedit if they want.
     */
    private int[] idToGrid(int id) {
        // 16 plots per row
        int row = id / 16;
        int col = id % 16;
        return new int[]{col, row};
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    private void load() {
        YamlConfiguration cfg = dataManager.loadYaml("plots/world.yml");
        nextId = cfg.getInt("nextId", 0);
        plotWorldName = cfg.getString("worldName", "plots");

        if (cfg.isConfigurationSection("plots")) {
            for (String key : cfg.getConfigurationSection("plots").getKeys(false)) {
                try {
                    int id = Integer.parseInt(key);
                    int gx = cfg.getInt("plots." + key + ".gridX");
                    int gz = cfg.getInt("plots." + key + ".gridZ");
                    UUID owner = UUID.fromString(cfg.getString("plots." + key + ".owner"));
                    List<UUID> trusted = new ArrayList<>();
                    for (String s : cfg.getStringList("plots." + key + ".trusted")) {
                        try { trusted.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                    }
                    Plot plot = new Plot(id, gx, gz, owner, trusted);
                    plots.put(id, plot);
                    ownerIndex.computeIfAbsent(owner, k -> new ArrayList<>()).add(id);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not load plot entry: " + key + " – " + e.getMessage());
                }
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("nextId", nextId);
        cfg.set("worldName", plotWorldName);
        for (Plot p : plots.values()) {
            String base = "plots." + p.getId();
            cfg.set(base + ".gridX", p.getGridX());
            cfg.set(base + ".gridZ", p.getGridZ());
            cfg.set(base + ".owner", p.getOwner().toString());
            List<String> trustedStrings = new ArrayList<>();
            for (UUID u : p.getTrusted()) trustedStrings.add(u.toString());
            cfg.set(base + ".trusted", trustedStrings);
        }
        dataManager.saveYaml(cfg, "plots/world.yml");
    }
}
