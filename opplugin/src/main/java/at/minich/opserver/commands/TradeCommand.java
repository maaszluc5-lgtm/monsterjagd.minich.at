package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.trade.TradeGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    // initiator uuid -> target uuid (pending requests)
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    // participant uuid -> TradeGUI (active trades)
    private final Map<UUID, TradeGUI> activeTrades;

    public TradeCommand(OpServerPlugin plugin, Map<UUID, TradeGUI> activeTrades) {
        this.plugin = plugin;
        this.activeTrades = activeTrades;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cOnly players can use this command.");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (args.length < 1) {
            player.sendMessage(prefix + "§cUsage: /trade <player> | /trade accept | /trade deny");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            UUID initiatorId = null;
            for (var entry : pendingRequests.entrySet()) {
                if (entry.getValue().equals(uuid)) {
                    initiatorId = entry.getKey();
                    break;
                }
            }
            if (initiatorId == null) {
                player.sendMessage(prefix + "§cDu hast keine ausstehende Handelsanfrage.");
                return true;
            }
            Player initiator = Bukkit.getPlayer(initiatorId);
            if (initiator == null) {
                player.sendMessage(prefix + "§cDer andere Spieler ist nicht mehr online.");
                pendingRequests.remove(initiatorId);
                return true;
            }
            pendingRequests.remove(initiatorId);

            TradeGUI trade = new TradeGUI(initiator, player);
            activeTrades.put(initiatorId, trade);
            activeTrades.put(uuid, trade);
            trade.open();
            return true;
        }

        if (args[0].equalsIgnoreCase("deny")) {
            UUID initiatorId = null;
            for (var entry : pendingRequests.entrySet()) {
                if (entry.getValue().equals(uuid)) {
                    initiatorId = entry.getKey();
                    break;
                }
            }
            if (initiatorId == null) {
                player.sendMessage(prefix + "§cDu hast keine ausstehende Handelsanfrage.");
                return true;
            }
            Player initiator = Bukkit.getPlayer(initiatorId);
            if (initiator != null) {
                initiator.sendMessage(prefix + "§c" + player.getName() + " hat deine Handelsanfrage abgelehnt.");
            }
            pendingRequests.remove(initiatorId);
            player.sendMessage(prefix + "§aHandelsanfrage abgelehnt.");
            return true;
        }

        // Request a trade
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(prefix + "§cSpieler nicht gefunden.");
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(prefix + "§cDu kannst nicht mit dir selbst handeln.");
            return true;
        }
        if (activeTrades.containsKey(uuid)) {
            player.sendMessage(prefix + "§cDu bist bereits in einem Handel.");
            return true;
        }

        pendingRequests.put(uuid, target.getUniqueId());
        player.sendMessage(prefix + "§aHandelsanfrage an §e" + target.getName() + " §agesendet.");
        target.sendMessage(prefix + "§e" + player.getName() + " §7möchte mit dir handeln! " +
                "Tippe §a/trade accept §7oder §c/trade deny§7.");
        return true;
    }
}
