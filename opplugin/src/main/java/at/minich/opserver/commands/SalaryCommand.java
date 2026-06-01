package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SalaryCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public SalaryCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
        double salary = plugin.getConfig().getDouble("salary.coins-per-minute", 10.0);
        sender.sendMessage(prefix + "§6Salary: §a¢" + String.format("%.2f", salary) + " §7per minute");
        sender.sendMessage(prefix + "§7You earn this amount for every minute you are online.");
        return true;
    }
}
