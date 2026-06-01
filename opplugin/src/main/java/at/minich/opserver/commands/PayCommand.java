package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PayCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public PayCommand(OpServerPlugin plugin) {
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

        if (args.length < 2) {
            sender.sendMessage(prefix + "§cUsage: /pay <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(prefix + plugin.getConfig().getString("messages.player-not-found", "§cPlayer not found."));
            return true;
        }

        if (target.equals(player)) {
            sender.sendMessage(prefix + "§cYou cannot pay yourself.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + "§cInvalid amount.");
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(prefix + "§cAmount must be positive.");
            return true;
        }

        if (!plugin.getEconomyManager().has(player.getUniqueId(), amount)) {
            sender.sendMessage(prefix + "§cInsufficient funds. Your balance: §a¢" +
                    String.format("%.2f", plugin.getEconomyManager().getBalance(player.getUniqueId())));
            return true;
        }

        plugin.getEconomyManager().withdraw(player.getUniqueId(), amount);
        plugin.getEconomyManager().deposit(target.getUniqueId(), amount);

        player.sendMessage(prefix + "§aPaid §f¢" + String.format("%.2f", amount) + " §ato §e" + target.getName());
        target.sendMessage(prefix + "§eYou received §f¢" + String.format("%.2f", amount) + " §efrom §a" + player.getName());
        return true;
    }
}
