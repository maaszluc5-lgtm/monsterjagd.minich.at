package at.minich.opserver.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /ve <enchant> <stufe> — applies any vanilla enchant up to level 200, bypassing max level.
 */
public class VanillaEnchantCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        if (!player.isOp()) {
            player.sendMessage("§cKein Zugriff!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /ve <enchant> <stufe>");
            player.sendMessage("§7Beispiel: /ve efficiency 100");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("§cHalte ein Item in der Hand!");
            return true;
        }

        Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(args[0].toLowerCase()));
        if (ench == null) {
            // Try by name for common aliases
            ench = Arrays.stream(Enchantment.values())
                    .filter(e -> e.getKey().getKey().equalsIgnoreCase(args[0]))
                    .findFirst().orElse(null);
        }
        if (ench == null) {
            player.sendMessage("§cUnbekannter Enchant: §e" + args[0]);
            player.sendMessage("§7Beispiele: efficiency, sharpness, protection, fortune, looting");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
            if (level < 0 || level > 200) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage("§cStufe muss zwischen 0 und 200 sein. (0 = entfernen)");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;

        if (level == 0) {
            meta.removeEnchant(ench);
            item.setItemMeta(meta);
            player.sendMessage("§aEnchant §e" + ench.getKey().getKey() + " §aentfernt.");
        } else {
            meta.addEnchant(ench, level, true); // true = ignore level restrictions
            item.setItemMeta(meta);
            player.sendMessage("§a✔ §e" + ench.getKey().getKey() + " §7Stufe §e" + level + " §ahinzugefügt!");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Enchantment.values())
                    .map(e -> e.getKey().getKey())
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return List.of("1", "5", "10", "50", "100", "200");
        }
        return List.of();
    }
}
