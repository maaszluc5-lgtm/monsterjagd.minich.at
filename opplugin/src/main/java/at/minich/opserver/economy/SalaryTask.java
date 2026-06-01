package at.minich.opserver.economy;

import at.minich.opserver.ranks.RankManager;
import at.minich.opserver.util.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pays online players a salary every minute, scaled by their rank multiplier.
 * Players in the lohnfarm world receive a 3x bonus.
 * Tracks total-earned and last payouts per player.
 */
public class SalaryTask extends BukkitRunnable {

    private final EconomyManager economyManager;
    private final RankManager rankManager;
    private final DataManager dataManager;
    private final double baseCoinsPerMinute;

    public SalaryTask(EconomyManager economyManager, RankManager rankManager,
                      DataManager dataManager, double baseCoinsPerMinute) {
        this.economyManager = economyManager;
        this.rankManager = rankManager;
        this.dataManager = dataManager;
        this.baseCoinsPerMinute = baseCoinsPerMinute;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            double multiplier = rankManager.getSalaryMultiplier(uuid);

            // 3x multiplier in lohnfarm world
            boolean inFarm = player.getWorld().getName().equals("lohnfarm");
            double farmMultiplier = inFarm ? 3.0 : 1.0;

            double payout = baseCoinsPerMinute * multiplier * farmMultiplier;
            economyManager.deposit(uuid, payout);

            // Track total-earned and last payouts
            String path = "players/" + uuid + ".yml";
            YamlConfiguration cfg = dataManager.loadYaml(path);

            double totalEarned = cfg.getDouble("salary.total-earned", 0.0);
            cfg.set("salary.total-earned", totalEarned + payout);

            // Keep last 3 payouts
            List<String> lastPayouts = cfg.getStringList("salary.last-payouts");
            if (lastPayouts == null) lastPayouts = new ArrayList<>();
            lastPayouts.add(0, String.format("%.0f", payout) + " Coins");
            if (lastPayouts.size() > 3) {
                lastPayouts = lastPayouts.subList(0, 3);
            }
            cfg.set("salary.last-payouts", lastPayouts);
            dataManager.saveYaml(cfg, path);

            // Notify player
            if (inFarm) {
                player.sendMessage("§a+¢" + String.format("%.0f", payout)
                        + " §7(salary §8x" + multiplier + " §a§l3x Farm§7)");
            } else if (multiplier != 1.0) {
                player.sendMessage("§a+¢" + String.format("%.0f", payout)
                        + " §7(salary §8x" + multiplier + "§7)");
            } else {
                player.sendMessage("§a+¢" + String.format("%.0f", payout) + " §7(salary)");
            }
        }
    }
}
