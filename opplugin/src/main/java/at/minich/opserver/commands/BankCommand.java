package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BankCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public BankCommand(OpServerPlugin plugin) {
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
            sendUsage(player, prefix);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "balance", "bal" -> {
                double bankBal = plugin.getBankManager().getBalance(player.getUniqueId());
                double walletBal = plugin.getEconomyManager().getBalance(player.getUniqueId());
                player.sendMessage(prefix + "§6Bank Balance: §a¢" + String.format("%.2f", bankBal));
                player.sendMessage(prefix + "§6Wallet Balance: §a¢" + String.format("%.2f", walletBal));
            }
            case "deposit" -> {
                if (args.length < 2) { player.sendMessage(prefix + "§cUsage: /bank deposit <amount>"); return true; }
                double amount = parseAmount(args[1]);
                if (Double.isNaN(amount) || amount <= 0) { player.sendMessage(prefix + "§cInvalid amount."); return true; }
                if (plugin.getBankManager().deposit(player.getUniqueId(), amount, plugin.getEconomyManager())) {
                    player.sendMessage(prefix + "§aDeposited §f¢" + String.format("%.2f", amount) +
                            " §ainto your bank. Bank: §f¢" + String.format("%.2f", plugin.getBankManager().getBalance(player.getUniqueId())));
                } else {
                    player.sendMessage(prefix + "§cInsufficient wallet funds.");
                }
            }
            case "withdraw" -> {
                if (args.length < 2) { player.sendMessage(prefix + "§cUsage: /bank withdraw <amount>"); return true; }
                double amount = parseAmount(args[1]);
                if (Double.isNaN(amount) || amount <= 0) { player.sendMessage(prefix + "§cInvalid amount."); return true; }
                if (plugin.getBankManager().withdraw(player.getUniqueId(), amount, plugin.getEconomyManager())) {
                    player.sendMessage(prefix + "§aWithdrew §f¢" + String.format("%.2f", amount) +
                            " §afrom your bank. Bank: §f¢" + String.format("%.2f", plugin.getBankManager().getBalance(player.getUniqueId())));
                } else {
                    player.sendMessage(prefix + "§cInsufficient bank funds.");
                }
            }
            default -> sendUsage(player, prefix);
        }
        return true;
    }

    private void sendUsage(Player player, String prefix) {
        player.sendMessage(prefix + "§6/bank balance §7— show balances");
        player.sendMessage(prefix + "§6/bank deposit <amount> §7— deposit coins");
        player.sendMessage(prefix + "§6/bank withdraw <amount> §7— withdraw coins");
    }

    private double parseAmount(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.NaN; }
    }
}
