package at.minich.opserver.tasks;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class JackpotTask extends BukkitRunnable {

    private final OpServerPlugin plugin;
    // uuid -> amount contributed
    private final Map<UUID, Double> contributions = new LinkedHashMap<>();
    private final Random random = new Random();

    public JackpotTask(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean contribute(UUID uuid, double amount) {
        double current = contributions.getOrDefault(uuid, 0.0);
        contributions.put(uuid, current + amount);
        return true;
    }

    public double getTotal() {
        return contributions.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getContribution(UUID uuid) {
        return contributions.getOrDefault(uuid, 0.0);
    }

    public Map<UUID, Double> getContributions() {
        return Collections.unmodifiableMap(contributions);
    }

    @Override
    public void run() {
        if (contributions.isEmpty()) return;

        double total = getTotal();
        // Weighted random selection
        double roll = random.nextDouble() * total;
        double cumulative = 0.0;
        UUID winner = null;
        for (Map.Entry<UUID, Double> entry : contributions.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                winner = entry.getKey();
                break;
            }
        }
        if (winner == null) {
            // fallback: pick last
            winner = contributions.keySet().stream().reduce((a, b) -> b).orElse(null);
        }
        if (winner == null) return;

        plugin.getEconomyManager().deposit(winner, total);

        Player winnerPlayer = Bukkit.getPlayer(winner);
        String winnerName = winnerPlayer != null ? winnerPlayer.getName() : winner.toString();

        String msg = "§6§l✦ " + winnerName + " §7hat den Jackpot von §6" +
                String.format("%.0f", total) + " Coins §7gewonnen! ✦";
        Bukkit.broadcastMessage(msg);

        contributions.clear();
    }
}
