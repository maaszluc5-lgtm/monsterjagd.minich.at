package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FarmWorldCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public FarmWorldCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (args.length >= 1 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("opserver.farmworld.reset")) {
                sender.sendMessage(prefix + plugin.getConfig().getString("messages.no-permission", "§cNo permission."));
                return true;
            }
            sender.sendMessage(prefix + "§eResetting farm world, please wait...");
            boolean success = plugin.getFarmWorldManager().reset();
            if (success) {
                sender.sendMessage(prefix + "§aFarm world has been reset successfully!");
            } else {
                sender.sendMessage(prefix + "§cFarm world reset failed. Check console for errors.");
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cOnly players can teleport to the farm world.");
            return true;
        }

        plugin.getFarmWorldManager().teleportToFarmWorld(player);
        return true;
    }
}
