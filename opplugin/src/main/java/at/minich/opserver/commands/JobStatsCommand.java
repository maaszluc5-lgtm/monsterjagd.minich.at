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

        Job job = jobManager.getJob(targetUuid);
        int level = jobManager.getLevel(targetUuid);
        long actions = jobManager.getActions(targetUuid);
        long total = jobManager.getTotalActions(targetUuid);

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        sender.sendMessage(prefix + "§6§lBerufsstatus von §e" + targetName);
        sender.sendMessage("§7────────────────────────────");

        if (job == null) {
            sender.sendMessage("§7Beruf: §cKein Beruf gewählt");
        } else {
            sender.sendMessage("§7Beruf:   " + job.getDisplayName());
            sender.sendMessage("§7Level:   §e" + level + " §8/ §e100");

            if (level < 100) {
                long required = jobManager.getRequiredActions(level);
                sender.sendMessage("§7Aktionen: §e" + actions + " §8/ §e" + required);
                sender.sendMessage("§7Fortschritt: " + buildBar(actions, required));

                // Next reward level
                int nextReward = nextRewardLevel(level);
                if (nextReward != -1) {
                    sender.sendMessage("§7Nächste Belohnung: §eLevel " + nextReward);
                }
            } else {
                sender.sendMessage("§a§lMAX LEVEL ERREICHT!");
            }

            sender.sendMessage("§7Gesamt-Aktionen: §e" + total);
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
