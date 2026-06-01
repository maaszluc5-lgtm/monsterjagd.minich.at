package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.bank.BankGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BankCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final BankGUI bankGUI;

    public BankCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.bankGUI = new BankGUI(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cNur Spieler können diesen Befehl nutzen.");
            return true;
        }

        // Text fallback: /bank balance
        if (args.length > 0 && (args[0].equalsIgnoreCase("balance")
                || args[0].equalsIgnoreCase("bal"))) {
            double bankBal   = plugin.getBankManager().getBalance(player.getUniqueId());
            double walletBal = plugin.getEconomyManager().getBalance(player.getUniqueId());
            player.sendMessage(prefix + "§6Bank-Guthaben: §a¢" + String.format("%.2f", bankBal));
            player.sendMessage(prefix + "§6Wallet-Guthaben: §a¢" + String.format("%.2f", walletBal));
            return true;
        }

        // Default: open GUI
        bankGUI.open(player);
        return true;
    }
}
