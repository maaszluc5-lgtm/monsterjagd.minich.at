package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.enchants.CustomEnchant;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EnchantCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public EnchantCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!sender.hasPermission("opserver.enchant")) {
            sender.sendMessage(prefix + plugin.getConfig().getString("messages.no-permission", "§cNo permission."));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cOnly players can use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + "§cUsage: /enchant <enchant_name> <level>");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            sender.sendMessage(prefix + "§cYou must hold an item.");
            return true;
        }

        CustomEnchant enchant = CustomEnchant.fromString(args[0]);
        if (enchant == null) {
            sender.sendMessage(prefix + "§cUnknown enchant: §f" + args[0]);
            sender.sendMessage(prefix + "§7Use §f/enchantlist §7to see all enchants.");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + "§cInvalid level.");
            return true;
        }

        if (level < 0) {
            sender.sendMessage(prefix + "§cLevel must be 0 or higher (0 removes the enchant).");
            return true;
        }

        if (level > enchant.getMaxLevel()) {
            sender.sendMessage(prefix + "§eWarning: §7Max level for §f" + enchant.getDisplayName() + " §7is §f" + enchant.getMaxLevel() + "§7. Clamping.");
            level = enchant.getMaxLevel();
        }

        ItemStack result = plugin.getEnchantManager().setEnchant(held, enchant, level);
        player.getInventory().setItemInMainHand(result);

        if (level == 0) {
            sender.sendMessage(prefix + "§7Removed §f" + enchant.getDisplayName() + " §7from your held item.");
        } else {
            sender.sendMessage(prefix + "§aApplied §f" + enchant.getDisplayName() + " " + level + " §ato your held item.");
        }
        return true;
    }
}
