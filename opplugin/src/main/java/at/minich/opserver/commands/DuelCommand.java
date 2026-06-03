package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.listeners.DuelListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DuelCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final DuelListener duelListener;

    public DuelCommand(OpServerPlugin plugin, DuelListener duelListener) {
        this.plugin = plugin;
        this.duelListener = duelListener;
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
            player.sendMessage(prefix + "§cUsage: /duel <player> [amount] | /duel accept | /duel deny");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (args[0].equalsIgnoreCase("accept")) {
            // Find a pending duel where this player is the target
            UUID challengerId = null;
            for (var entry : duelListener.pendingDuels.entrySet()) {
                if (entry.getValue().equals(uuid)) {
                    challengerId = entry.getKey();
                    break;
                }
            }

            if (challengerId == null) {
                player.sendMessage(prefix + "§cDu hast keine ausstehende Duellanfrage.");
                return true;
            }

            Player challenger = Bukkit.getPlayer(challengerId);
            if (challenger == null) {
                player.sendMessage(prefix + "§cDer Herausforderer ist nicht mehr online.");
                duelListener.pendingDuels.remove(challengerId);
                duelListener.pendingBets.remove(challengerId);
                return true;
            }

            double bet = duelListener.pendingBets.getOrDefault(challengerId, 0.0);

            // Check both have enough
            if (bet > 0 && !plugin.getEconomyManager().has(uuid, bet)) {
                player.sendMessage(prefix + "§cDu hast nicht genug Coins für das Duell (" +
                        String.format("%.0f", bet) + " benötigt).");
                challenger.sendMessage(prefix + "§c" + player.getName() + " hat nicht genug Coins für das Duell.");
                duelListener.pendingDuels.remove(challengerId);
                duelListener.pendingBets.remove(challengerId);
                return true;
            }

            // Deduct from challenger (bet already checked when challenged)
            if (bet > 0) {
                plugin.getEconomyManager().withdraw(challengerId, bet);
                plugin.getEconomyManager().withdraw(uuid, bet);
            }

            // Start duel
            duelListener.pendingDuels.remove(challengerId);
            duelListener.pendingBets.remove(challengerId);

            duelListener.activeDuels.put(challengerId, uuid);
            duelListener.activeDuels.put(uuid, challengerId);
            // Store total pot under challenger key
            duelListener.activeBets.put(challengerId, bet * 2);

            // Apply Strength I to both
            PotionEffect strength = new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, true, true);
            challenger.addPotionEffect(strength);
            player.addPotionEffect(strength);

            Bukkit.broadcastMessage(prefix + "§6" + challenger.getName() + " §7und §6" + player.getName() +
                    " §7duellieren sich" + (bet > 0 ? " um §6" + String.format("%.0f", bet) + " Coins§7" : "") + "!");
            return true;
        }

        if (args[0].equalsIgnoreCase("deny")) {
            UUID challengerId = null;
            for (var entry : duelListener.pendingDuels.entrySet()) {
                if (entry.getValue().equals(uuid)) {
                    challengerId = entry.getKey();
                    break;
                }
            }

            if (challengerId == null) {
                player.sendMessage(prefix + "§cDu hast keine ausstehende Duellanfrage.");
                return true;
            }

            Player challenger = Bukkit.getPlayer(challengerId);
            if (challenger != null) {
                challenger.sendMessage(prefix + "§c" + player.getName() + " hat deine Duellanfrage abgelehnt.");
            }
            duelListener.pendingDuels.remove(challengerId);
            duelListener.pendingBets.remove(challengerId);
            player.sendMessage(prefix + "§aDuellanfrage abgelehnt.");
            return true;
        }

        // Challenge a player
        if (args.length < 1) {
            player.sendMessage(prefix + "§cUsage: /duel <player> [amount]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(prefix + "§cSpieler nicht gefunden.");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(prefix + "§cDu kannst dich nicht selbst herausfordern.");
            return true;
        }

        double bet = 0.0;
        if (args.length >= 2) {
            try {
                bet = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(prefix + "§cUngültiger Betrag.");
                return true;
            }
            if (bet < 0) {
                player.sendMessage(prefix + "§cDer Betrag muss positiv sein.");
                return true;
            }
            if (bet > 0 && !plugin.getEconomyManager().has(uuid, bet)) {
                player.sendMessage(prefix + "§cNicht genug Coins.");
                return true;
            }
        }

        duelListener.pendingDuels.put(uuid, target.getUniqueId());
        duelListener.pendingBets.put(uuid, bet);

        player.sendMessage(prefix + "§aDuellanfrage an §e" + target.getName() +
                (bet > 0 ? " §aüber §e" + String.format("%.0f", bet) + " Coins" : "") + " §agesendet.");
        target.sendMessage(prefix + "§e" + player.getName() + " §7fordert dich zu einem Duell heraus" +
                (bet > 0 ? " um §6" + String.format("%.0f", bet) + " Coins§7" : "") +
                "! Tippe §a/duel accept §7oder §c/duel deny§7.");
        return true;
    }
}
