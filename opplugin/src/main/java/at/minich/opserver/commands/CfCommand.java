package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CfCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();
    private static final long COOLDOWN_MS = 10_000L;

    public CfCommand(OpServerPlugin plugin) {
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

        if (args.length < 1) {
            player.sendMessage(prefix + "§cUsage: /cf <amount>");
            return true;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid)) {
            long elapsed = now - cooldowns.get(uuid);
            if (elapsed < COOLDOWN_MS) {
                long remaining = (COOLDOWN_MS - elapsed) / 1000 + 1;
                player.sendMessage(prefix + "§cBitte warte noch §e" + remaining + " §cSekunden.");
                return true;
            }
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

        if (!plugin.getEconomyManager().has(uuid, amount)) {
            player.sendMessage(prefix + "§cNicht genug Coins. Dein Kontostand: §e" +
                    String.format("%.0f", plugin.getEconomyManager().getBalance(uuid)) + " Coins");
            return true;
        }

        cooldowns.put(uuid, now);

        boolean won = random.nextBoolean();
        if (won) {
            plugin.getEconomyManager().deposit(uuid, amount);
            String msg = "§6" + player.getName() + " §7hat §6" + String.format("%.0f", amount) +
                    " Coins §7beim Münzwurf gewonnen!";
            Bukkit.broadcastMessage(msg);
        } else {
            plugin.getEconomyManager().withdraw(uuid, amount);
            player.sendMessage(prefix + "§cDu hast §e" + String.format("%.0f", amount) +
                    " Coins §beim Münzwurf verloren!");
        }

        return true;
    }
}
