package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.lootbox.LootboxType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LootboxCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public LootboxCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length >= 4 && args[0].equalsIgnoreCase("give")) {
                handleGive(sender, args);
                return true;
            }
            sender.sendMessage("§cNur Spieler (außer /lootbox give).");
            return true;
        }

        if (args.length == 0) {
            showInventory(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("give") && sender.hasPermission("opserver.admin")) {
            handleGive(sender, args);
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /lootbox <open|buy> <common|rare|legendary>");
            return true;
        }

        LootboxType type = parseType(args[1]);
        if (type == null) {
            player.sendMessage("§cUnbekannter Kisten-Typ. Nutze: common, rare, legendary");
            return true;
        }

        if (sub.equals("open")) {
            plugin.getLootboxManager().openBox(player, type);
        } else if (sub.equals("buy")) {
            plugin.getLootboxManager().buyBox(player, type);
        } else {
            player.sendMessage("§cUnbekannter Unterbefehl.");
        }
        return true;
    }

    private void showInventory(Player player) {
        player.sendMessage("§6§l--- Deine Lootboxen ---");
        for (LootboxType type : LootboxType.values()) {
            int count = plugin.getLootboxManager().getCount(player.getUniqueId(), type);
            player.sendMessage(type.displayName + "§7: §e" + count + "x §8(Kosten: §e" + (int) type.cost + " Coins§8)");
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /lootbox give <player> <type> <amount>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cSpieler nicht gefunden.");
            return;
        }
        LootboxType type = parseType(args[2]);
        if (type == null) {
            sender.sendMessage("§cUnbekannter Kisten-Typ.");
            return;
        }
        int amount;
        try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException e) {
            sender.sendMessage("§cUngültige Menge.");
            return;
        }
        plugin.getLootboxManager().addBox(target.getUniqueId(), type, amount);
        target.sendMessage("§aDu hast §e" + amount + "x " + type.displayName + " §aerhalten!");
        sender.sendMessage("§aGegeben: §e" + amount + "x " + type.displayName + " §aan §e" + target.getName());
    }

    private LootboxType parseType(String s) {
        return switch (s.toLowerCase()) {
            case "common", "gewohnlich", "gewöhnlich" -> LootboxType.COMMON;
            case "rare", "selten" -> LootboxType.RARE;
            case "legendary", "legendar", "legendär" -> LootboxType.LEGENDARY;
            default -> null;
        };
    }
}
