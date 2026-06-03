package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.tasks.JackpotTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JackpotCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final JackpotTask jackpotTask;

    public JackpotCommand(OpServerPlugin plugin, JackpotTask jackpotTask) {
        this.plugin = plugin;
        this.jackpotTask = jackpotTask;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cOnly players can use this command.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(prefix + "§cUsage: /jackpot <amount> | /jackpot info");
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            double total = jackpotTask.getTotal();
            double myContrib = jackpotTask.getContribution(player.getUniqueId());
            player.sendMessage(prefix + "§6Jackpot-Topf: §e" + String.format("%.0f", total) + " Coins");
            player.sendMessage(prefix + "§6Dein Beitrag: §e" + String.format("%.0f", myContrib) + " Coins");
            if (total > 0) {
                double chance = (myContrib / total) * 100;
                player.sendMessage(prefix + "§6Deine Gewinnchance: §e" + String.format("%.1f", chance) + "%%");
            }
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§cUngültiger Betrag.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(prefix + "§cDer Betrag muss positiv sein.");
            return true;
        }

        if (!plugin.getEconomyManager().has(player.getUniqueId(), amount)) {
            player.sendMessage(prefix + "§cNicht genug Coins.");
            return true;
        }

        plugin.getEconomyManager().withdraw(player.getUniqueId(), amount);
        jackpotTask.contribute(player.getUniqueId(), amount);

        player.sendMessage(prefix + "§aDu hast §e" + String.format("%.0f", amount) +
                " Coins §azum Jackpot beigetragen. Topf: §e" +
                String.format("%.0f", jackpotTask.getTotal()) + " Coins");
        return true;
    }
}
