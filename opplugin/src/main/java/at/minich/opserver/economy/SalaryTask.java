package at.minich.opserver.economy;

import at.minich.opserver.ranks.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Pays online players a salary every minute, scaled by their rank multiplier.
 */
public class SalaryTask extends BukkitRunnable {

    private final EconomyManager economyManager;
    private final RankManager rankManager;
    private final double baseCoinsPerMinute;

    public SalaryTask(EconomyManager economyManager, RankManager rankManager, double baseCoinsPerMinute) {
        this.economyManager = economyManager;
        this.rankManager = rankManager;
        this.baseCoinsPerMinute = baseCoinsPerMinute;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            double multiplier = rankManager.getSalaryMultiplier(player.getUniqueId());
            double payout = baseCoinsPerMinute * multiplier;
            economyManager.deposit(player.getUniqueId(), payout);

            if (multiplier != 1.0) {
                player.sendMessage("§a+¢" + String.format("%.0f", payout)
                        + " §7(salary §8x" + multiplier + "§7)");
            } else {
                player.sendMessage("§a+¢" + String.format("%.0f", payout) + " §7(salary)");
            }
        }
    }
}
