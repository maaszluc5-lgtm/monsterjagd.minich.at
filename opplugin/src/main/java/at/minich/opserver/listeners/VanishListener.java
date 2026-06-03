package at.minich.opserver.listeners;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class VanishListener implements Listener {

    private final OpServerPlugin plugin;

    public VanishListener(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();

        // Hide all currently vanished players from the newly joining player
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.isVanished(online.getUniqueId())) {
                joining.hidePlayer(plugin, online);
            }
        }

        // If the joining player is vanished themselves, hide them from everyone (non-ops)
        if (plugin.isVanished(joining.getUniqueId())) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(joining) && !online.isOp() && !online.hasPermission("opserver.admin")) {
                    online.hidePlayer(plugin, joining);
                }
            }
            // Suppress join message for vanished players
            event.setJoinMessage(null);
        }
    }
}
