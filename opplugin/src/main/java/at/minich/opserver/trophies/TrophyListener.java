package at.minich.opserver.trophies;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.util.DataManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/**
 * Listens for trophy-relevant events and triggers awards.
 */
public class TrophyListener implements Listener {

    private final OpServerPlugin plugin;
    private final TrophyManager trophyManager;
    private final DataManager dataManager;

    public TrophyListener(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.trophyManager = plugin.getTrophyManager();
        this.dataManager = plugin.getDataManager();
    }

    /**
     * Awards ERSTER_SPIELER to the very first player who ever joins.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!trophyManager.anyoneHasErsterSpieler()) {
            trophyManager.awardTrophy(player, TrophyType.ERSTER_SPIELER);
        }
        // Check general trophies (rank, coins) for returning players too
        trophyManager.checkTrophies(player);
    }

    /**
     * Awards PVP_GOTT when a player reaches 10,000 player kills.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        // Don't count self-kills
        if (event.getEntity().equals(killer)) return;

        UUID uuid = killer.getUniqueId();
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        long kills = cfg.getLong("stats.kills", 0);

        if (kills >= 10_000) {
            trophyManager.awardTrophy(killer, TrophyType.PVP_GOTT);
        }
    }

    /**
     * Public hook to check all general trophies for a player.
     * Can be called from other parts of the plugin.
     */
    public void checkTrophies(Player player) {
        trophyManager.checkTrophies(player);
    }
}
