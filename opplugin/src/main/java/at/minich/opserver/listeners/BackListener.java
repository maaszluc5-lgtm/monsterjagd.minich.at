package at.minich.opserver.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;

public class BackListener implements Listener {

    private final Map<UUID, Location> lastLocations;

    public BackListener(Map<UUID, Location> lastLocations) {
        this.lastLocations = lastLocations;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Location loc = event.getEntity().getLocation();
        lastLocations.put(event.getEntity().getUniqueId(), loc);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        // Store location before teleport so /back brings them back
        lastLocations.put(event.getPlayer().getUniqueId(), event.getFrom());
    }
}
