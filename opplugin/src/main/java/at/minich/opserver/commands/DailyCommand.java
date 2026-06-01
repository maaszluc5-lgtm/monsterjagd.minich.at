package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.rewards.DailyRewardManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DailyCommand implements CommandExecutor {

    private final DailyRewardManager dailyRewardManager;

    public DailyCommand(OpServerPlugin plugin) {
        this.dailyRewardManager = plugin.getDailyRewardManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can claim daily rewards.");
            return true;
        }
        dailyRewardManager.claimDaily((Player) sender);
        return true;
    }
}
