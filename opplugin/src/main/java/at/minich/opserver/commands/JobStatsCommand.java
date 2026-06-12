package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.jobs.Job;
import at.minich.opserver.jobs.JobManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /jobstats [player] – shows text-based job statistics.
 */
public class JobStatsCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final JobManager jobManager;

    public JobStatsCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        UUID targetUuid;
        String targetName;

        if (args.length >= 1) {
            // Look up other player
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
            if (!op.hasPlayedBefore() && !op.isOnline()) {
                sender.sendMessage(plugin.getConfig().getString("messages.player-not-found",
                        "§cPlayer not found."));
                return true;
            }
            targetUuid = op.getUniqueId();
            targetName = op.getName() != null ? op.getName() : args[0];
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cUsage: /jobstats <player>");
                return true;
            }
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        sender.sendMessage(prefix + "§6§lBerufsstatus von §e" + targetName);
        sender.sendMessage("§7────────────────────────────");
        sender.sendMessage("§7Alle Jobs sind gleichzeitig aktiv:");
        sender.sendMessage("");

        for (Job job : Job.values()) {
            int level = jobManager.getLevel(targetUuid, job);
            long actions = jobManager.getActions(targetUuid, job);
            long total = jobManager.getTotalActions(targetUuid, job);
            double coins = jobManager.getCoinsPerAction(level);
            sender.sendMessage(job.getDisplayName() + " §8| §7Level §e" + level
                    + " §8| §e" + String.format("%.2f", coins) + " §7Coins/Aktion");
            if (level < 100) {
                long required = jobManager.getRequiredActions(level);
                sender.sendMessage("  §7" + buildBar(actions, required)
                        + " §e" + actions + "§8/§e" + required);
            } else {
                sender.sendMessage("  §a§lMAX LEVEL!");
            }
        }

        sender.sendMessage("§7────────────────────────────");
        return true;
    }

    private String buildBar(long current, long max) {
        int totalBars = 20;
        int filled = max > 0 ? (int) Math.round((double) current / max * totalBars) : 0;
        filled = Math.min(filled, totalBars);
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < totalBars; i++) {
            sb.append(i < filled ? "§a|" : "§8|");
        }
        sb.append("§8]");
        double pct = max > 0 ? (double) current / max * 100 : 0;
        sb.append(String.format(" §7%.1f%%", pct));
        return sb.toString();
    }

    private int nextRewardLevel(int currentLevel) {
        for (int lvl = 5; lvl <= 100; lvl += 5) {
            if (lvl > currentLevel) return lvl;
        }
        return -1;
    }
}
