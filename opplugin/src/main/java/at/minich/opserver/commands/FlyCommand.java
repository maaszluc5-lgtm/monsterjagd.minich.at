package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlyCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public FlyCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (args.length >= 1) {
            if (sender instanceof Player p && !p.isOp() && !p.hasPermission("opserver.admin")) {
                sender.sendMessage(prefix + "§cYou don't have permission to toggle flight for other players.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(prefix + "§cPlayer not found or offline.");
                return true;
            }
            toggleFly(target);
            target.sendMessage(prefix + "§aFlight §e" + (target.getAllowFlight() ? "enabled" : "disabled") + "§a by " + sender.getName() + ".");
            sender.sendMessage(prefix + "§aFlight §e" + (target.getAllowFlight() ? "enabled" : "disabled") + " §afor §e" + target.getName() + "§a.");
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "§cUsage: /fly <player>");
                return true;
            }
            toggleFly(player);
            player.sendMessage(prefix + "§aFlight §e" + (player.getAllowFlight() ? "enabled" : "disabled") + "§a.");
        }
        return true;
    }

    private void toggleFly(Player player) {
        boolean newState = !player.getAllowFlight();
        player.setAllowFlight(newState);
        if (!newState) {
            player.setFlying(false);
        }
    }
}
