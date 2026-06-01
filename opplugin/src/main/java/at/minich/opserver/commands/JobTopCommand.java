package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.jobs.Job;
import at.minich.opserver.jobs.JobManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /jobtop [job] – shows top 10 players by level for a specific job.
 */
public class JobTopCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final JobManager jobManager;

    public JobTopCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /jobtop <job>");
            sender.sendMessage("§7Berufe: GRAEBER, MIENENARBEITER, FARMER, FISHER, JAEGER, BUILDER");
            return true;
        }

        Job job = Job.fromString(args[0]);
        if (job == null) {
            sender.sendMessage("§cUnbekannter Beruf: §e" + args[0]);
            sender.sendMessage("§7Verfügbare Berufe: GRAEBER, MIENENARBEITER, FARMER, FISHER, JAEGER, BUILDER");
            return true;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
        List<Map.Entry<UUID, Integer>> top = jobManager.getTopPlayers(job, 10);

        sender.sendMessage(prefix + "§6§lTop 10 – " + job.getDisplayName());
        sender.sendMessage("§7────────────────────────────");

        if (top.isEmpty()) {
            sender.sendMessage("§7Noch keine Spieler mit diesem Beruf.");
        } else {
            for (int i = 0; i < top.size(); i++) {
                UUID uuid = top.get(i).getKey();
                int level = top.get(i).getValue();
                @SuppressWarnings("deprecation")
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                String name = op.getName() != null ? op.getName() : uuid.toString();
                String medal = switch (i) {
                    case 0 -> "§6#1 ";
                    case 1 -> "§7#2 ";
                    case 2 -> "§c#3 ";
                    default -> "§8#" + (i + 1) + " ";
                };
                sender.sendMessage(medal + "§e" + name + " §7– Level §a" + level);
            }
        }

        sender.sendMessage("§7────────────────────────────");
        return true;
    }
}
