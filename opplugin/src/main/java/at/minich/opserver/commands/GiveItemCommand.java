package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.items.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public class GiveItemCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public GiveItemCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!sender.hasPermission("opserver.giveitem")) {
            sender.sendMessage(prefix + plugin.getConfig().getString("messages.no-permission", "§cNo permission."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + "§cUsage: /giveitem <player> <item>");
            listItems(sender, prefix);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(prefix + plugin.getConfig().getString("messages.player-not-found", "§cPlayer not found."));
            return true;
        }

        String itemName = args[1].toUpperCase();

        // Special case: GOD_ARMOR_SET and STARTER_KIT
        if (itemName.equals("GOD_ARMOR_SET")) {
            ItemStack[] armor = CustomItems.buildGodArmorSet(plugin.getEnchantManager());
            for (ItemStack piece : armor) {
                target.getInventory().addItem(piece);
            }
            sender.sendMessage(prefix + "§aGave §6God Armor Set §ato §e" + target.getName());
            target.sendMessage(prefix + "§aYou received §6God Armor Set§a!");
            return true;
        }

        if (itemName.equals("STARTER_KIT")) {
            ItemStack[] kit = CustomItems.buildStarterKit(plugin.getEnchantManager());
            for (ItemStack item : kit) {
                target.getInventory().addItem(item);
            }
            sender.sendMessage(prefix + "§aGave §6Starter Kit §ato §e" + target.getName());
            target.sendMessage(prefix + "§aYou received §6Starter Kit§a!");
            return true;
        }

        CustomItems ci = CustomItems.fromString(itemName);
        if (ci == null) {
            sender.sendMessage(prefix + "§cUnknown item: §f" + args[1]);
            listItems(sender, prefix);
            return true;
        }

        ItemStack item = ci.build(plugin.getEnchantManager());
        target.getInventory().addItem(item);
        sender.sendMessage(prefix + "§aGave §6" + ci.getDisplayName() + " §ato §e" + target.getName());
        target.sendMessage(prefix + "§aYou received §6" + ci.getDisplayName() + "§a!");
        return true;
    }

    private void listItems(CommandSender sender, String prefix) {
        String items = Arrays.stream(CustomItems.values())
                .map(CustomItems::name)
                .collect(Collectors.joining(", "));
        sender.sendMessage(prefix + "§7Available items: §f" + items + ", GOD_ARMOR_SET, STARTER_KIT");
    }
}
