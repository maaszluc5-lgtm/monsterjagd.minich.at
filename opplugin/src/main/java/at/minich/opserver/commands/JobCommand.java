package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.jobs.JobGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobCommand implements CommandExecutor {

    private final JobGUI jobGUI;

    public JobCommand(OpServerPlugin plugin, JobGUI jobGUI) {
        this.jobGUI = jobGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        jobGUI.open(player);
        return true;
    }
}
