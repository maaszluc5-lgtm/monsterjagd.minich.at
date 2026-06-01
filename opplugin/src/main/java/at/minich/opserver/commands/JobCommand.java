package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.jobs.Job;
import at.minich.opserver.jobs.JobGUI;
import at.minich.opserver.jobs.JobManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /job [jobname] – opens the Job GUI or switches to the given job.
 */
public class JobCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final JobManager jobManager;
    private final JobGUI jobGUI;

    public JobCommand(OpServerPlugin plugin, JobGUI jobGUI) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.jobGUI = jobGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }

        if (args.length == 0) {
            // Open GUI
            jobGUI.open(player);
            return true;
        }

        // Switch job by name
        if (!player.hasPermission("opserver.job.switch")) {
            player.sendMessage(plugin.getConfig().getString("messages.no-permission",
                    "§cYou don't have permission to do that."));
            return true;
        }

        Job job = Job.fromString(args[0]);
        if (job == null) {
            player.sendMessage("§cUnbekannter Beruf: §e" + args[0]);
            player.sendMessage("§7Verfügbare Berufe: GRAEBER, MIENENARBEITER, FARMER, FISHER, JAEGER, BUILDER");
            return true;
        }

        UUID uuid = player.getUniqueId();
        Job current = jobManager.getJob(uuid);

        if (job == current) {
            player.sendMessage("§eDu bist bereits " + job.getDisplayName() + "§e!");
            return true;
        }

        jobManager.setJob(uuid, job);
        player.sendMessage("§a§lBeruf gesetzt! §rDu bist jetzt " + job.getDisplayName() + "§r§a.");
        player.sendMessage("§7" + job.getDescription());
        return true;
    }
}
