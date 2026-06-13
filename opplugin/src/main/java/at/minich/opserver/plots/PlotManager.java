package at.minich.opserver.plots;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.util.DataManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;

import java.util.*;

public class PlotManager {

    public static final int MAX_PLOTS_PER_PLAYER = 3;
    private static final String WORLD_NAME = "plots";

    private final OpServerPlugin plugin;
    private final DataManager dataManager;
    private final Map<Integer, Plot> plots = new HashMap<>();
    private final Map<UUID, List<Integer>> ownerIndex = new HashMap<>();
    private int nextId = 0;

    public PlotManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        ensurePlotWorld();
        load();
    }

    // -------------------------------------------------------------------------
    // Flat world creation
    // -------------------------------------------------------------------------

    private void ensurePlotWorld() {
        if (Bukkit.getWorld(WORLD_NAME) != null) return;

        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.type(WorldType.FLAT);
        creator.generatorSettings("{\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},{\"block\":\"minecraft:dirt\",\"height\":3},{\"block\":\"minecraft:grass_block\",\"height\":1}],\"biome\":\"minecraft:plains\"}");
        creator.environment(World.Environment.NORMAL);
        World w = creator.createWorld();
        if (w != null) {
            w.setSpawnLocation(0, 65, 0);
            w.setTime(6000);
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            w.setGameRule(GameRule.MOB_SPAWNING, false);
            w.setGameRule(GameRule.DO_FIRE_TICK, false);
            plugin.getLogger().info("[Plots] Flat plot world created.");
        }
    }

    public World getPlotWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        return w != null ? w : Bukkit.getWorlds().get(0);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public List<Plot> getPlotsOf(UUID uuid) {
        List<Integer> ids = ownerIndex.getOrDefault(uuid, Collections.emptyList());
        List<Plot> result = new ArrayList<>();
        for (int id : ids) { Plot p = plots.get(id); if (p != null) result.add(p); }
        return result;
    }

    public int getPlotCount(UUID uuid) {
        return ownerIndex.getOrDefault(uuid, Collections.emptyList()).size();
    }

    public Plot claimNextPlot(UUID uuid) {
        if (getPlotCount(uuid) >= MAX_PLOTS_PER_PLAYER) return null;
        int id = nextId++;
        int[] grid = idToGrid(id);
        Plot plot = new Plot(id, grid[0], grid[1], uuid);
        plots.put(id, plot);
        ownerIndex.computeIfAbsent(uuid, k -> new ArrayList<>()).add(id);
        save();
        Bukkit.getScheduler().runTask(plugin, () -> buildPlotBorders(plot));
        return plot;
    }

    public Plot getPlotAt(int worldX, int worldZ) {
        for (Plot p : plots.values()) {
            if (p.contains(worldX, 65, worldZ)) return p;
        }
        return null;
    }

    public Plot getFirstPlot(UUID uuid) {
        List<Plot> owned = getPlotsOf(uuid);
        return owned.isEmpty() ? null : owned.get(0);
    }

    public boolean addTrust(Plot plot, UUID target) {
        if (plot.isTrusted(target)) return false;
        plot.addTrusted(target); save(); return true;
    }

    public boolean removeTrust(Plot plot, UUID target) {
        boolean removed = plot.removeTrusted(target);
        if (removed) save();
        return removed;
    }

    // -------------------------------------------------------------------------
    // Plot border building
    // -------------------------------------------------------------------------

    /** Places gold block corners and quartz slab/stone road around the plot. */
    public void buildPlotBorders(Plot plot) {
        World w = getPlotWorld();
        if (w == null) return;

        int minX = plot.getWorldMinX();
        int minZ = plot.getWorldMinZ();
        int maxX = plot.getWorldMaxX();
        int maxZ = plot.getWorldMaxZ();
        int y = Plot.Y_MIN - 1; // border at ground level (y=63)

        // Corners - gold block
        w.getBlockAt(minX - 1, y, minZ - 1).setType(Material.GOLD_BLOCK);
        w.getBlockAt(maxX + 1, y, minZ - 1).setType(Material.GOLD_BLOCK);
        w.getBlockAt(minX - 1, y, maxZ + 1).setType(Material.GOLD_BLOCK);
        w.getBlockAt(maxX + 1, y, maxZ + 1).setType(Material.GOLD_BLOCK);

        // Border edges - quartz slabs
        for (int x = minX; x <= maxX; x++) {
            setRoadBlock(w, x, y, minZ - 1);
            setRoadBlock(w, x, y, maxZ + 1);
        }
        for (int z = minZ; z <= maxZ; z++) {
            setRoadBlock(w, minX - 1, y, z);
            setRoadBlock(w, maxX + 1, y, z);
        }

        // Road fill between plots (the 3-block gap)
        // Fill road to the right (positive X)
        for (int rx = maxX + 2; rx < maxX + Plot.ROAD_WIDTH; rx++) {
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                setRoadBlock(w, rx, y, z);
            }
        }
        // Fill road downward (positive Z)
        for (int rz = maxZ + 2; rz < maxZ + Plot.ROAD_WIDTH; rz++) {
            for (int x = minX - 1; x <= maxX + 1; x++) {
                setRoadBlock(w, x, y, rz);
            }
        }
    }

    private void setRoadBlock(World w, int x, int y, int z) {
        Block b = w.getBlockAt(x, y, z);
        if (b.getType() == Material.GOLD_BLOCK) return;
        b.setType(Material.QUARTZ_SLAB);
    }

    // -------------------------------------------------------------------------
    // Grid layout (spiral from 0,0)
    // -------------------------------------------------------------------------

    private int[] idToGrid(int id) {
        int col = id % 16;
        int row = id / 16;
        return new int[]{col, row};
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    private void load() {
        YamlConfiguration cfg = dataManager.loadYaml("plots/world.yml");
        nextId = cfg.getInt("nextId", 0);
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
                    plugin.getLogger().warning("Could not load plot " + key + ": " + e.getMessage());
                }
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("nextId", nextId);
        for (Plot p : plots.values()) {
            String base = "plots." + p.getId();
            cfg.set(base + ".gridX", p.getGridX());
            cfg.set(base + ".gridZ", p.getGridZ());
            cfg.set(base + ".owner", p.getOwner().toString());
            List<String> ts = new ArrayList<>();
            for (UUID u : p.getTrusted()) ts.add(u.toString());
            cfg.set(base + ".trusted", ts);
        }
        dataManager.saveYaml(cfg, "plots/world.yml");
    }
}
