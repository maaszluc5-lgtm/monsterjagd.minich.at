package at.minich.opserver.commands;

import at.minich.opserver.items.InfiniteItemListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class InfiniteItemCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        if (!player.isOp() && !player.hasPermission("opserver.admin")) {
            player.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage("§cHalte ein Item in der Hand!");
            return true;
        }

        ItemMeta meta = held.getItemMeta();
        if (meta == null) return true;

        if (InfiniteItemListener.isInfinite(held)) {
            // Remove infinite tag
            List<String> lore = meta.getLore();
            if (lore != null) lore.removeIf(l -> l.contains("∞ Unendlich"));
            meta.setLore(lore);
            held.setItemMeta(meta);
            player.sendMessage("§c∞ §7Unendlich-Effekt §centfernt.");
        } else {
            // Add infinite tag
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add("§8∞ Unendlich");
            meta.setLore(lore);
            held.setItemMeta(meta);
            player.sendMessage("§a∞ §7Item ist jetzt §aunendlich§7!");
        }
        return true;
    }
}
