package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VanishCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public VanishCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (sender instanceof Player p && !p.isOp() && !p.hasPermission("opserver.admin")) {
            sender.sendMessage(prefix + "§cYou don't have permission to use this command.");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(prefix + "§cPlayer not found or offline.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + "§cUsage: /vanish <player>");
                return true;
            }
            target = (Player) sender;
        }

        boolean vanished = plugin.toggleVanish(target);
        if (!target.equals(sender)) {
            sender.sendMessage(prefix + "§e" + target.getName() + " §ais now §e" + (vanished ? "vanished" : "visible") + "§a.");
        }
        target.sendMessage(prefix + "§aYou are now §e" + (vanished ? "vanished" : "visible") + "§a.");
        return true;
    }
}
