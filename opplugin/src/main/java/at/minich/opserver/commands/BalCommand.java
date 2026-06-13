package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BalCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public BalCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        UUID targetUuid;
        String targetName;

        if (args.length >= 1) {
            // Special: /bal server
            if (args[0].equalsIgnoreCase("server")) {
                double serverBal = plugin.getEconomyManager().getBalance(at.minich.opserver.economy.ServerAccount.UUID);
                sender.sendMessage(prefix + "§6Server-Konto§7: §a¢" + String.format("%.2f", serverBal));
                return true;
            }
            // Look up another player
            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                sender.sendMessage(prefix + plugin.getConfig().getString("messages.player-not-found", "§cPlayer not found."));
                return true;
            }
            targetUuid = offlinePlayer.getUniqueId();
            targetName = offlinePlayer.getName() != null ? offlinePlayer.getName() : args[0];
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "§cUsage: /bal <player>");
                return true;
            }
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        }

        double balance = plugin.getEconomyManager().getBalance(targetUuid);
        sender.sendMessage(prefix + "§e" + targetName + "§7's balance: §a¢" + String.format("%.2f", balance));
        return true;
    }
}
