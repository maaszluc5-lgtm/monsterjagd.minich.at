package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.kits.KitManager;
import at.minich.opserver.kits.KitManager.KitInfo;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class KitCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final KitManager kitManager;

    public KitCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.kitManager = plugin.getKitManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /kit <name>  — Use /kitlist to see available kits.");
            return true;
        }

        Player player = (Player) sender;
        kitManager.giveKit(player, args[0]);
        return true;
    }
}
