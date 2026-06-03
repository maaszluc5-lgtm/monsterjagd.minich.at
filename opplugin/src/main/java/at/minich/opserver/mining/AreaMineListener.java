package at.minich.opserver.mining;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Listens for block break events and triggers area mining when the player's
 * configured size is greater than 1.
 */
public class AreaMineListener implements Listener {

    // Thread-local guard: tracks blocks currently being broken by area mining
    // to prevent recursive triggering.
    private final Set<Long> processingBlocks = new HashSet<>();

    private final OpServerPlugin plugin;
    private final AreaMineManager manager;

    public AreaMineListener(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getAreaMineManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block origin = event.getBlock();

        // Skip recursion guard
        long originKey = blockKey(origin);
        if (processingBlocks.contains(originKey)) return;

        int size = manager.getSize(player.getUniqueId());
        if (size <= 1) return; // normal mining

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isMiningTool(tool.getType())) return;

        // Prevent bedrock mining under any circumstance
        if (origin.getType() == Material.BEDROCK) return;

        int half = size / 2; // e.g. size=3 -> half=1
        Material originType = origin.getType();

        // Collect blocks to break (excluding the origin which the event already handles)
        Set<Block> toBreak = new HashSet<>();
        for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
                for (int dz = -half; dz <= half; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue; // origin handled by event
                    Block b = origin.getRelative(dx, dy, dz);
                    if (b.getType() == Material.BEDROCK) continue;
                    if (b.getType() == Material.AIR) continue;
                    if (isHarderThan(b.getType(), originType)) continue;
                    toBreak.add(b);
                }
            }
        }

        // Break the extra blocks, giving drops to the player
        for (Block b : toBreak) {
            long key = blockKey(b);
            processingBlocks.add(key);
            try {
                Collection<ItemStack> drops = b.getDrops(tool);
                b.setType(Material.AIR);
                for (ItemStack drop : drops) {
                    b.getWorld().dropItemNaturally(b.getLocation(), drop);
                }
            } finally {
                processingBlocks.remove(key);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean isMiningTool(Material mat) {
        return switch (mat) {
            case WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE,
                 DIAMOND_PICKAXE, NETHERITE_PICKAXE,
                 WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE,
                 DIAMOND_AXE, NETHERITE_AXE,
                 WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL,
                 DIAMOND_SHOVEL, NETHERITE_SHOVEL -> true;
            default -> false;
        };
    }

    /**
     * Returns true if {@code candidate} is considered harder than {@code reference}.
     * We use a simple tier mapping so that e.g. obsidian won't be broken when mining stone.
     */
    private boolean isHarderThan(Material candidate, Material reference) {
        return hardnessTier(candidate) > hardnessTier(reference);
    }

    /**
     * Rough hardness tiers. Lower = softer.
     */
    private int hardnessTier(Material mat) {
        return switch (mat) {
            // Indestructible
            case BEDROCK, BARRIER, COMMAND_BLOCK, CHAIN_COMMAND_BLOCK,
                 REPEATING_COMMAND_BLOCK, END_PORTAL_FRAME -> 100;
            // Very hard
            case OBSIDIAN, CRYING_OBSIDIAN, REINFORCED_DEEPSLATE,
                 ANCIENT_DEBRIS -> 9;
            // Hard
            case DEEPSLATE, COBBLED_DEEPSLATE, POLISHED_DEEPSLATE,
                 DEEPSLATE_BRICKS, DEEPSLATE_TILES, CHISELED_DEEPSLATE,
                 DEEPSLATE_COAL_ORE, DEEPSLATE_IRON_ORE, DEEPSLATE_GOLD_ORE,
                 DEEPSLATE_DIAMOND_ORE, DEEPSLATE_EMERALD_ORE,
                 DEEPSLATE_LAPIS_ORE, DEEPSLATE_REDSTONE_ORE,
                 DEEPSLATE_COPPER_ORE -> 7;
            // Stone-like
            case STONE, COBBLESTONE, MOSSY_COBBLESTONE, STONE_BRICKS,
                 MOSSY_STONE_BRICKS, CRACKED_STONE_BRICKS, CHISELED_STONE_BRICKS,
                 ANDESITE, DIORITE, GRANITE, POLISHED_ANDESITE, POLISHED_DIORITE,
                 POLISHED_GRANITE, BLACKSTONE, POLISHED_BLACKSTONE,
                 BASALT, SMOOTH_BASALT, NETHERRACK,
                 COAL_ORE, IRON_ORE, GOLD_ORE, COPPER_ORE,
                 DIAMOND_ORE, EMERALD_ORE, LAPIS_ORE, REDSTONE_ORE,
                 NETHER_QUARTZ_ORE, NETHER_GOLD_ORE -> 5;
            // Soft stone / dirt
            case DIRT, GRASS_BLOCK, PODZOL, MYCELIUM, ROOTED_DIRT,
                 COARSE_DIRT, CLAY, GRAVEL, SAND, RED_SAND,
                 SOUL_SAND, SOUL_SOIL -> 2;
            // Wood / logs
            case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG, ACACIA_LOG,
                 DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG,
                 OAK_WOOD, BIRCH_WOOD, SPRUCE_WOOD, JUNGLE_WOOD,
                 ACACIA_WOOD, DARK_OAK_WOOD -> 3;
            default -> 4; // mid-range for everything else
        };
    }

    /** Encodes a block location into a single long for use as a map key. */
    private long blockKey(Block b) {
        // X: 26 bits, Y: 12 bits, Z: 26 bits  — sufficient for vanilla world limits
        long x = (long) (b.getX() + 0x2000000) & 0x3FFFFFFL;
        long y = (long) (b.getY() + 0x800) & 0xFFFL;
        long z = (long) (b.getZ() + 0x2000000) & 0x3FFFFFFL;
        return (x << 38) | (y << 26) | z;
    }
}
