package at.minich.opserver.listeners;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class GodListener implements Listener {

    private final OpServerPlugin plugin;

    public GodListener(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.isGodMode(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
