package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GodCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public GodCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (args.length >= 1) {
            if (sender instanceof Player p && !p.isOp() && !p.hasPermission("opserver.admin")) {
                sender.sendMessage(prefix + "§cYou don't have permission to toggle god mode for other players.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(prefix + "§cPlayer not found or offline.");
                return true;
            }
            boolean enabled = plugin.toggleGodMode(target.getUniqueId());
            target.sendMessage(prefix + "§aGod mode §e" + (enabled ? "enabled" : "disabled") + " §aby " + sender.getName() + ".");
            sender.sendMessage(prefix + "§aGod mode §e" + (enabled ? "enabled" : "disabled") + " §afor §e" + target.getName() + "§a.");
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "§cUsage: /god <player>");
                return true;
            }
            if (!player.isOp() && !player.hasPermission("opserver.admin")) {
                sender.sendMessage(prefix + "§cYou don't have permission to use this command.");
                return true;
            }
            boolean enabled = plugin.toggleGodMode(player.getUniqueId());
            player.sendMessage(prefix + "§aGod mode §e" + (enabled ? "enabled" : "disabled") + "§a.");
        }
        return true;
    }
}
