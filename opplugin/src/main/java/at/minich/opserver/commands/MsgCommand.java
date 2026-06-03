package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class MsgCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public MsgCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (args.length < 2) {
            sender.sendMessage(prefix + "§cUsage: /msg <player> <message>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(prefix + "§cPlayer not found or offline.");
            return true;
        }

        if (target.equals(sender)) {
            sender.sendMessage(prefix + "§cYou cannot message yourself.");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String senderName = sender instanceof Player ? ((Player) sender).getDisplayName() : sender.getName();

        sender.sendMessage("§7[§eDir → §f" + target.getDisplayName() + "§7] §f" + message);
        target.sendMessage("§7[§f" + senderName + " §e→ §7Dir] §f" + message);

        // Track last message partners
        if (sender instanceof Player senderPlayer) {
            plugin.setLastMessagePartner(senderPlayer.getUniqueId(), target.getUniqueId());
        }
        plugin.setLastMessagePartner(target.getUniqueId(), sender instanceof Player ? ((Player) sender).getUniqueId() : null);

        return true;
    }
}
