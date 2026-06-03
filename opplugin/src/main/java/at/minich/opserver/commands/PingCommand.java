package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PingCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public PingCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (args.length >= 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(prefix + "§cPlayer not found or offline.");
                return true;
            }
            sender.sendMessage(prefix + "§e" + target.getName() + "§7's ping: §a" + target.getPing() + "ms");
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "§cUsage: /ping <player>");
                return true;
            }
            player.sendMessage(prefix + "§7Your ping: §a" + player.getPing() + "ms");
        }
        return true;
    }
}
