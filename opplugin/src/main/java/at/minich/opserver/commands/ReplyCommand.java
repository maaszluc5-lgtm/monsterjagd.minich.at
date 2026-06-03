package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ReplyCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public ReplyCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(prefix + "§cUsage: /r <message>");
            return true;
        }

        UUID partnerUuid = plugin.getLastMessagePartner(player.getUniqueId());
        if (partnerUuid == null) {
            player.sendMessage(prefix + "§cYou have no one to reply to.");
            return true;
        }

        Player target = Bukkit.getPlayer(partnerUuid);
        if (target == null) {
            player.sendMessage(prefix + "§cYour last message partner is no longer online.");
            return true;
        }

        String message = String.join(" ", args);

        player.sendMessage("§7[§eDir → §f" + target.getDisplayName() + "§7] §f" + message);
        target.sendMessage("§7[§f" + player.getDisplayName() + " §e→ §7Dir] §f" + message);

        plugin.setLastMessagePartner(player.getUniqueId(), target.getUniqueId());
        plugin.setLastMessagePartner(target.getUniqueId(), player.getUniqueId());

        return true;
    }
}
