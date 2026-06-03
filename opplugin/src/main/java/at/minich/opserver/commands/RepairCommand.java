package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class RepairCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public RepairCommand(OpServerPlugin plugin) {
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

        if (!player.isOp() && !player.hasPermission("opserver.admin")) {
            sender.sendMessage(prefix + "§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("all")) {
            int repaired = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (repairItem(item)) repaired++;
            }
            player.updateInventory();
            player.sendMessage(prefix + "§aRepaired §e" + repaired + " §aitems in your inventory.");
        } else {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                player.sendMessage(prefix + "§cYou are not holding an item.");
                return true;
            }
            if (!repairItem(held)) {
                player.sendMessage(prefix + "§cThis item cannot be repaired.");
                return true;
            }
            player.updateInventory();
            player.sendMessage(prefix + "§aYour item has been repaired.");
        }
        return true;
    }

    private boolean repairItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return false;
        if (damageable.getDamage() == 0) return false;
        damageable.setDamage(0);
        item.setItemMeta(damageable);
        return true;
    }
}
