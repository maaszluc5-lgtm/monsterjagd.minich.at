package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.items.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class GiveItemCommand implements CommandExecutor, TabCompleter {

    /** Armor-set pseudo-names that expand to 4 pieces each. */
    private static final List<String> ARMOR_SETS = List.of(
        "GOD_ARMOR_SET",
        "SHADOW_ARMOR_SET",
        "DRAGON_ARMOR_SET",
        "BERSERKER_ARMOR_SET",
        "SPEED_ARMOR_SET"
    );

    private static final String STARTER_KIT_NAME = "STARTER_KIT";

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

        // ------------------------------------------------------------------
        // Armor sets (give all 4 pieces at once)
        // ------------------------------------------------------------------
        switch (itemName) {
            case "GOD_ARMOR_SET" -> {
                giveAll(target, Arrays.asList(CustomItems.buildGodArmorSet(plugin.getEnchantManager())));
                notify(sender, target, prefix, "God Armor Set");
                return true;
            }
            case "SHADOW_ARMOR_SET" -> {
                giveAll(target, CustomItems.buildShadowArmorSet(plugin.getEnchantManager()));
                notify(sender, target, prefix, "Shadow Armor Set");
                return true;
            }
            case "DRAGON_ARMOR_SET" -> {
                giveAll(target, CustomItems.buildDragonArmorSet(plugin.getEnchantManager()));
                notify(sender, target, prefix, "Dragon Armor Set");
                return true;
            }
            case "BERSERKER_ARMOR_SET" -> {
                giveAll(target, CustomItems.buildBerserkerArmorSet(plugin.getEnchantManager()));
                notify(sender, target, prefix, "Berserker Armor Set");
                return true;
            }
            case "SPEED_ARMOR_SET" -> {
                giveAll(target, CustomItems.buildSpeedArmorSet(plugin.getEnchantManager()));
                notify(sender, target, prefix, "Speed Armor Set");
                return true;
            }
            case STARTER_KIT_NAME -> {
                giveAll(target, Arrays.asList(CustomItems.buildStarterKit(plugin.getEnchantManager())));
                notify(sender, target, prefix, "Starter Kit");
                return true;
            }
        }

        // ------------------------------------------------------------------
        // Single custom item
        // ------------------------------------------------------------------
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            List<String> all = new ArrayList<>();
            for (CustomItems ci : CustomItems.values()) all.add(ci.name());
            all.addAll(ARMOR_SETS);
            all.add(STARTER_KIT_NAME);
            String partial = args[1].toUpperCase();
            return all.stream()
                    .filter(n -> n.startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void giveAll(Player target, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (item != null) target.getInventory().addItem(item);
        }
    }

    private void notify(CommandSender sender, Player target, String prefix, String name) {
        sender.sendMessage(prefix + "§aGave §6" + name + " §ato §e" + target.getName());
        target.sendMessage(prefix + "§aYou received §6" + name + "§a!");
    }

    private void listItems(CommandSender sender, String prefix) {
        String items = Arrays.stream(CustomItems.values())
                .map(CustomItems::name)
                .collect(Collectors.joining(", "));
        String sets = String.join(", ", ARMOR_SETS);
        sender.sendMessage(prefix + "§7Items: §f" + items);
        sender.sendMessage(prefix + "§7Sets: §f" + sets + ", " + STARTER_KIT_NAME);
    }
}
