package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpeedCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public SpeedCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (args.length == 0) {
            sender.sendMessage(prefix + "§cUsage: /speed <1-10> [player] or /speed fly <1-10> [player]");
            return true;
        }

        boolean flyMode = false;
        int speedArg = 0;
        Player target = null;

        if (args[0].equalsIgnoreCase("fly")) {
            flyMode = true;
            if (args.length < 2) {
                sender.sendMessage(prefix + "§cUsage: /speed fly <1-10> [player]");
                return true;
            }
            try {
                speedArg = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(prefix + "§cInvalid speed value. Use 1-10.");
                return true;
            }
            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(prefix + "§cPlayer not found or offline.");
                    return true;
                }
            }
        } else {
            try {
                speedArg = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(prefix + "§cInvalid speed value. Use 1-10.");
                return true;
            }
            if (args.length >= 2) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(prefix + "§cPlayer not found or offline.");
                    return true;
                }
            }
        }

        if (speedArg < 1 || speedArg > 10) {
            sender.sendMessage(prefix + "§cSpeed must be between §e1§c and §e10§c.");
            return true;
        }

        if (target != null) {
            if (sender instanceof Player p && !p.isOp() && !p.hasPermission("opserver.admin")) {
                sender.sendMessage(prefix + "§cYou don't have permission to change other players' speed.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + "§cConsole must specify a player.");
                return true;
            }
            target = (Player) sender;
        }

        // Bukkit speed range: 0.0 to 1.0 (walk default 0.2, fly default 0.1)
        float bukkitSpeed = speedArg / 10.0f;

        if (flyMode) {
            target.setFlySpeed(bukkitSpeed);
            target.sendMessage(prefix + "§aFly speed set to §e" + speedArg + "§a.");
            if (!target.equals(sender)) {
                sender.sendMessage(prefix + "§aSet fly speed to §e" + speedArg + " §afor §e" + target.getName() + "§a.");
            }
        } else {
            target.setWalkSpeed(bukkitSpeed);
            target.sendMessage(prefix + "§aWalk speed set to §e" + speedArg + "§a.");
            if (!target.equals(sender)) {
                sender.sendMessage(prefix + "§aSet walk speed to §e" + speedArg + " §afor §e" + target.getName() + "§a.");
            }
        }
        return true;
    }
}
