package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FeedCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public FeedCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (args.length >= 1) {
            if (!(sender instanceof Player) && !sender.isOp()) {
                sender.sendMessage(prefix + "§cYou don't have permission to feed other players.");
                return true;
            }
            if (sender instanceof Player p && !p.isOp() && !p.hasPermission("opserver.admin")) {
                sender.sendMessage(prefix + "§cYou don't have permission to feed other players.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(prefix + "§cPlayer not found or offline.");
                return true;
            }
            feedPlayer(target);
            target.sendMessage(prefix + "§aYour hunger has been filled.");
            sender.sendMessage(prefix + "§aFed §e" + target.getName() + "§a.");
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "§cUsage: /feed <player>");
                return true;
            }
            feedPlayer(player);
            player.sendMessage(prefix + "§aYour hunger has been filled.");
        }
        return true;
    }

    private void feedPlayer(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }
}
