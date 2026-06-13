package at.minich.opserver.plots;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlotListener implements Listener {

    private final OpServerPlugin plugin;
    private final PlotManager plotManager;
    private final Map<UUID, Integer> lastPlotId = new HashMap<>();

    public PlotListener(OpServerPlugin plugin, PlotManager plotManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        // Protect border blocks always
        if (isBorderBlock(block)) {
            event.setCancelled(true);
            return;
        }
        if (isProtected(event.getPlayer(), block)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDu hast keine Erlaubnis, hier Blöcke abzubauen.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (isBorderBlock(block)) {
            event.setCancelled(true);
            return;
        }
        if (isProtected(event.getPlayer(), block)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDu hast keine Erlaubnis, hier Blöcke zu setzen.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof BlockInventoryHolder holder)) return;
        Block block = holder.getBlock();
        if (isProtected(player, block)) {
            event.setCancelled(true);
            player.sendMessage("§cDu hast keine Erlaubnis, diese Kiste zu öffnen.");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        World plotWorld = plotManager.getPlotWorld();
        if (!player.getWorld().equals(plotWorld)) return;

        Location to = event.getTo();
        if (to == null) return;

        Plot newPlot = plotManager.getPlotAt(to.getBlockX(), to.getBlockZ());
        Integer oldPlotId = lastPlotId.get(player.getUniqueId());
        int newPlotId = newPlot == null ? -1 : newPlot.getId();

        if (oldPlotId == null || oldPlotId != newPlotId) {
            lastPlotId.put(player.getUniqueId(), newPlotId);
            if (newPlot != null) {
                String ownerName = plugin.getServer().getOfflinePlayer(newPlot.getOwner()).getName();
                player.sendMessage("§6§l[Plot] §eGrundstück #" + newPlot.getId()
                        + " §7von §a" + (ownerName != null ? ownerName : "Unbekannt"));
                showBorderParticles(player, newPlot);
            } else if (oldPlotId != null && oldPlotId != -1) {
                player.sendMessage("§7[Plot] §8Verlassen.");
            }
        }
    }

    private void showBorderParticles(Player player, Plot plot) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 3) { cancel(); return; }
                World w = plotManager.getPlotWorld();
                if (w == null) { cancel(); return; }
                double y = player.getLocation().getY() + 0.5;
                int minX = plot.getWorldMinX(); int maxX = plot.getWorldMaxX();
                int minZ = plot.getWorldMinZ(); int maxZ = plot.getWorldMaxZ();
                for (double x = minX; x <= maxX; x += 0.5) {
                    w.spawnParticle(Particle.FLAME, x, y, minZ, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.FLAME, x, y, maxZ, 1, 0, 0, 0, 0);
                }
                for (double z = minZ; z <= maxZ; z += 0.5) {
                    w.spawnParticle(Particle.FLAME, minX, y, z, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.FLAME, maxX, y, z, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private boolean isBorderBlock(Block block) {
        if (!block.getWorld().equals(plotManager.getPlotWorld())) return false;
        Material type = block.getType();
        String typeName = type.name();
        return typeName.contains("QUARTZ") || type == Material.GOLD_BLOCK;
    }

    private boolean isProtected(Player player, Block block) {
        if (player.hasPermission("opserver.admin")) return false;
        World plotWorld = plotManager.getPlotWorld();
        if (!block.getWorld().equals(plotWorld)) return false;
        // In plot world: only allowed to build on your own/trusted plot
        Plot plot = plotManager.getPlotAt(block.getX(), block.getZ());
        if (plot == null) {
            // Road / unclaimed area - nobody can build here
            player.sendMessage("§cDu kannst hier nicht bauen (kein Grundstück).");
            return true;
        }
        return !plot.isTrusted(player.getUniqueId());
    }
}
