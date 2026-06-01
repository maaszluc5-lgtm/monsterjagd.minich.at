package at.minich.opserver.listeners;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.util.DataManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

/**
 * Tracks kills, deaths, and blocks mined per player and persists them in player YAML files.
 */
public class StatsListener implements Listener {

    private final DataManager dataManager;

    public StatsListener(OpServerPlugin plugin) {
        this.dataManager = plugin.getDataManager();
    }

    /** Track player kills (only count when the killer is a player). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        // Don't count killing yourself
        if (event.getEntity() instanceof Player && event.getEntity().equals(killer)) return;

        incrementStat(killer.getUniqueId(), "stats.kills");
    }

    /** Track player deaths. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        incrementStat(event.getEntity().getUniqueId(), "stats.deaths");
    }

    /** Track blocks mined. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        incrementStat(event.getPlayer().getUniqueId(), "stats.blocks-mined");
    }

    // -------------------------------------------------------------------------

    private void incrementStat(UUID uuid, String statKey) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        long current = cfg.getLong(statKey, 0);
        cfg.set(statKey, current + 1);
        dataManager.saveYaml(cfg, path);
    }
}
