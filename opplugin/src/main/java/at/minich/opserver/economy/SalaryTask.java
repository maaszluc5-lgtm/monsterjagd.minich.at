package at.minich.opserver.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Pays online players a salary every minute.
 */
public class SalaryTask extends BukkitRunnable {

    private final EconomyManager economyManager;
    private final double coinsPerMinute;

    public SalaryTask(EconomyManager economyManager, double coinsPerMinute) {
        this.economyManager = economyManager;
        this.coinsPerMinute = coinsPerMinute;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            economyManager.deposit(player.getUniqueId(), coinsPerMinute);
            player.sendMessage("§a+¢" + String.format("%.0f", coinsPerMinute) + " §7(salary)");
        }
    }
}
