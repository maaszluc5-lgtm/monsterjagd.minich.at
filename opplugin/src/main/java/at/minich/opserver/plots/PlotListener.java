package at.minich.opserver.plots;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlotListener implements Listener {

    private final OpServerPlugin plugin;
    private final PlotManager plotManager;

    public PlotListener(OpServerPlugin plugin, PlotManager plotManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isProtected(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDu hast keine Erlaubnis, hier Blöcke abzubauen.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isProtected(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDu hast keine Erlaubnis, hier Blöcke zu setzen.");
        }
    }

    /**
     * Returns true if the block is inside a plot and the player is NOT allowed to build there.
     */
    private boolean isProtected(Player player, Block block) {
        // Admins bypass protection
        if (player.hasPermission("opserver.plot.admin")) return false;

        World plotWorld = plotManager.getPlotWorld();
        if (!block.getWorld().equals(plotWorld)) return false;

        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        Plot plot = plotManager.getPlotAt(x, y, z);
        if (plot == null) {
            // Block is in the plot world but not inside any claimed plot – allow
            return false;
        }

        // Allow if owner or trusted
        return !plot.isTrusted(player.getUniqueId());
    }
}
