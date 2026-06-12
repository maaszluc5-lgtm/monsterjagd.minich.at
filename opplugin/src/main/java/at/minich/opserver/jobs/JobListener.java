package at.minich.opserver.jobs;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class JobListener implements Listener {

    private static final Set<Material> GRAEBER_BLOCKS = EnumSet.of(
            Material.DIRT, Material.GRAVEL, Material.SAND, Material.COARSE_DIRT,
            Material.GRASS_BLOCK, Material.CLAY, Material.SOUL_SAND,
            Material.RED_SAND, Material.DIRT_PATH, Material.MYCELIUM,
            Material.PODZOL, Material.MUD, Material.ROOTED_DIRT
    );

    private static final Set<Material> MINER_BLOCKS = EnumSet.of(
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS,
            Material.NETHERRACK, Material.BASALT, Material.BLACKSTONE,
            Material.TUFF, Material.CALCITE, Material.DRIPSTONE_BLOCK
    );

    private static final Set<Material> FARMER_CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.MELON, Material.PUMPKIN,
            Material.SUGAR_CANE, Material.NETHER_WART, Material.COCOA
    );

    private static final Set<Material> BUILDER_BLOCKS;

    static {
        BUILDER_BLOCKS = EnumSet.noneOf(Material.class);
        for (Material m : Material.values()) {
            String n = m.name();
            if (n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_PLANKS")
                    || n.endsWith("_STONE_BRICKS") || n.equals("STONE_BRICKS")
                    || n.endsWith("_GLASS") || n.equals("GLASS")
                    || n.endsWith("_CONCRETE") || n.endsWith("_TERRACOTTA")
                    || n.equals("TERRACOTTA") || n.endsWith("_WOOL")
                    || n.endsWith("_STAIRS") || n.endsWith("_SLAB")
                    || n.endsWith("_BRICKS") || n.equals("BRICKS")) {
                BUILDER_BLOCKS.add(m);
            }
        }
    }

    private final OpServerPlugin plugin;
    private final JobManager jobManager;

    public JobListener(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Block block = event.getBlock();
        Material mat = block.getType();

        // All jobs active simultaneously
        if (GRAEBER_BLOCKS.contains(mat)) {
            jobManager.addActions(uuid, Job.GRAEBER, 1);
        }
        if (MINER_BLOCKS.contains(mat)) {
            jobManager.addActions(uuid, Job.MIENENARBEITER, 1);
        }
        if (FARMER_CROPS.contains(mat) && isFullyGrown(block)) {
            jobManager.addActions(uuid, Job.FARMER, 1);
        }
    }

    private boolean isFullyGrown(Block block) {
        Material mat = block.getType();
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return mat == Material.MELON || mat == Material.PUMPKIN || mat == Material.SUGAR_CANE;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        jobManager.addActions(event.getPlayer().getUniqueId(), Job.FISHER, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        UUID uuid = killer.getUniqueId();
        int amount = event.getEntity() instanceof Player ? 10 : 1;
        jobManager.addActions(uuid, Job.JAEGER, amount);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Material mat = event.getBlock().getType();
        if (BUILDER_BLOCKS.contains(mat)) {
            jobManager.addActions(event.getPlayer().getUniqueId(), Job.BUILDER, 1);
        }
    }
}
