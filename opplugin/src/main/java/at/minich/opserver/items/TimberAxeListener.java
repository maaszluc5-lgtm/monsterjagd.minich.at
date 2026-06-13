package at.minich.opserver.items;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TimberAxeListener implements Listener {

    private static final int MAX_LOGS = 200;

    private static final Set<Material> LOGS = Set.of(
        Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
        Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
        Material.MANGROVE_LOG, Material.CHERRY_LOG,
        Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD,
        Material.JUNGLE_WOOD, Material.ACACIA_WOOD, Material.DARK_OAK_WOOD,
        Material.MANGROVE_WOOD, Material.CHERRY_WOOD
    );

    private static final Set<Material> LEAVES = Set.of(
        Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES,
        Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES,
        Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES, Material.AZALEA_LEAVES,
        Material.FLOWERING_AZALEA_LEAVES
    );

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isTimberAxe(tool)) return;

        Block origin = event.getBlock();
        if (!LOGS.contains(origin.getType())) return;

        // BFS: collect all connected logs
        Set<Block> toBreak = new LinkedHashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(origin);
        toBreak.add(origin);

        while (!queue.isEmpty() && toBreak.size() < MAX_LOGS) {
            Block current = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block neighbor = current.getRelative(dx, dy, dz);
                        if (LOGS.contains(neighbor.getType()) && toBreak.add(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        // Also collect adjacent leaves
        Set<Block> leavesToBreak = new HashSet<>();
        for (Block log : toBreak) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        Block b = log.getRelative(dx, dy, dz);
                        if (LEAVES.contains(b.getType())) leavesToBreak.add(b);
                    }
                }
            }
        }

        // Break all logs (except origin — handled by the event)
        for (Block log : toBreak) {
            if (!log.equals(origin)) {
                log.breakNaturally(tool);
            }
        }
        // Remove leaves (no drops, like natural decay)
        for (Block leaf : leavesToBreak) {
            leaf.breakNaturally(tool);
        }
    }

    private boolean isTimberAxe(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Timber Axt");
    }
}
