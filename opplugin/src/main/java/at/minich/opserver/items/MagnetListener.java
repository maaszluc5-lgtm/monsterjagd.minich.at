package at.minich.opserver.items;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * Pulls nearby dropped items to the player when they hold the Magnet.
 */
public class MagnetListener implements Listener {

    private static final double RADIUS = 5.0;

    private final OpServerPlugin plugin;

    public MagnetListener(OpServerPlugin plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (!isMagnet(hand)) continue;

                    Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                            player.getLocation(), RADIUS, RADIUS, RADIUS,
                            e -> e instanceof Item
                    );
                    for (Entity entity : nearby) {
                        Item dropped = (Item) entity;
                        dropped.teleport(player.getLocation());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private boolean isMagnet(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (!item.getItemMeta().hasDisplayName()) return false;
        return item.getItemMeta().getDisplayName().contains("Magnet");
    }
}
