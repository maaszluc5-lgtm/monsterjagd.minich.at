package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.ranks.RankManager;
import at.minich.opserver.ranks.RankManager.RankInfo;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /rank              → show your current rank and price to next rank
 * /rank buy          → purchase the next rank up
 * /rank [player]     → view another player's rank
 */
public class RankCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final RankManager rankManager;

    public RankCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        // /rank buy
        if (args.length >= 1 && args[0].equalsIgnoreCase("buy")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
                return true;
            }
            UUID uuid = player.getUniqueId();
            RankInfo next = rankManager.getNextRank(uuid);
            if (next == null) {
                player.sendMessage(prefix + "§6Du hast bereits den höchsten Rang!");
                return true;
            }
            double balance = plugin.getEconomyManager().getBalance(uuid);
            if (balance < next.price) {
                player.sendMessage(prefix + "§cNicht genug Coins! Benötigt: §6"
                        + String.format("%.0f", next.price) + " Coins §c(du hast §6"
                        + String.format("%.2f", balance) + " Coins§c).");
                return true;
            }
            boolean success = rankManager.purchaseRank(player);
            if (success) {
                RankInfo current = rankManager.getRank(uuid);
                player.sendMessage(prefix + "§aGlückwunsch! Du hast den Rang "
                        + (current != null ? current.prefix : "") + " §akauft!");
            } else {
                player.sendMessage(prefix + "§cRangkauf fehlgeschlagen.");
            }
            return true;
        }

        // /rank [player]
        UUID targetUUID;
        String targetName;

        if (args.length >= 1) {
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
            if (op == null || (!op.hasPlayedBefore() && !op.isOnline())) {
                sender.sendMessage(prefix + "§cSpieler nicht gefunden: " + args[0]);
                return true;
            }
            targetUUID = op.getUniqueId();
            targetName = op.getName() != null ? op.getName() : args[0];
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cUsage: /rank [buy|player]");
                return true;
            }
            Player p = (Player) sender;
            targetUUID = p.getUniqueId();
            targetName = p.getName();
        }

        RankInfo current = rankManager.getRank(targetUUID);
        RankInfo next = rankManager.getNextRank(targetUUID);

        sender.sendMessage("§8=== §6Rang: " + targetName + " §8===");
        if (current != null) {
            sender.sendMessage("§7Aktueller Rang: " + current.prefix);
            sender.sendMessage("§7Lohn-Multiplikator: §a" + current.salaryMultiplier + "x");
            if (next != null) {
                sender.sendMessage("§7Nächster Rang: §b" + next.prefix
                        + " §7(Preis: §6" + String.format("%.0f", next.price) + " Coins§7)");
                // If sender is the target player show their balance too
                if (sender instanceof Player player && player.getUniqueId().equals(targetUUID)) {
                    double balance = plugin.getEconomyManager().getBalance(targetUUID);
                    double missing = next.price - balance;
                    if (missing > 0) {
                        sender.sendMessage("§7Dein Guthaben: §c" + String.format("%.2f", balance)
                                + " Coins §7(noch §c" + String.format("%.0f", missing) + " Coins§7 benötigt)");
                    } else {
                        sender.sendMessage("§7Dein Guthaben: §a" + String.format("%.2f", balance)
                                + " Coins §7— §a/rank buy §7zum Kaufen!");
                    }
                }
            } else {
                sender.sendMessage("§7Du hast den §4§lhöchsten Rang §7erreicht!");
            }
        } else {
            sender.sendMessage("§7Keine Rangdaten gefunden.");
        }
        return true;
    }
}
