package at.minich.opserver.listeners;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelListener implements Listener {

    private final OpServerPlugin plugin;

    // challenger -> challenged
    public final Map<UUID, UUID> pendingDuels = new HashMap<>();
    // challenger -> bet amount
    public final Map<UUID, Double> pendingBets = new HashMap<>();
    // active duel: uuid -> opponent uuid
    public final Map<UUID, UUID> activeDuels = new HashMap<>();
    // duel bet: challenger uuid -> amount
    public final Map<UUID, Double> activeBets = new HashMap<>();

    public DuelListener(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        UUID deadId = dead.getUniqueId();

        if (!activeDuels.containsKey(deadId)) return;

        UUID opponentId = activeDuels.get(deadId);
        Player opponent = Bukkit.getPlayer(opponentId);

        // Determine challenger to find bet
        UUID challengerId = activeBets.containsKey(deadId) ? deadId : opponentId;
        double bet = activeBets.getOrDefault(challengerId, 0.0);

        // Transfer coins from loser to winner
        if (opponent != null) {
            plugin.getEconomyManager().withdraw(deadId, bet);
            plugin.getEconomyManager().deposit(opponentId, bet);

            String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
            Bukkit.broadcastMessage(prefix + "§6" + opponent.getName() + " §7hat das Duell gegen §6" +
                    dead.getName() + " §7gewonnen und §6" + String.format("%.0f", bet) + " Coins §7erhalten!");
        }

        // Clean up
        activeDuels.remove(deadId);
        activeDuels.remove(opponentId);
        activeBets.remove(deadId);
        activeBets.remove(opponentId);

        // Remove strength effects
        if (opponent != null) {
            opponent.removePotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Cancel active duel
        if (activeDuels.containsKey(uuid)) {
            UUID opponentId = activeDuels.get(uuid);
            Player opponent = Bukkit.getPlayer(opponentId);

            // Refund bet to the challenger
            UUID challengerId = activeBets.containsKey(uuid) ? uuid : opponentId;
            double bet = activeBets.getOrDefault(challengerId, 0.0);
            if (bet > 0) {
                plugin.getEconomyManager().deposit(challengerId, bet);
            }

            String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
            if (opponent != null) {
                opponent.removePotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH);
                opponent.sendMessage(prefix + "§cDein Duell wurde abgebrochen, da dein Gegner die Verbindung getrennt hat.");
            }

            activeDuels.remove(uuid);
            activeDuels.remove(opponentId);
            activeBets.remove(uuid);
            activeBets.remove(opponentId);
        }

        // Remove pending duel requests involving this player
        pendingDuels.entrySet().removeIf(e -> e.getKey().equals(uuid) || e.getValue().equals(uuid));
        pendingBets.remove(uuid);
    }
}
