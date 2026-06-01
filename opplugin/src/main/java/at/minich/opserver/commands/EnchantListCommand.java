package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.enchants.CustomEnchant;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class EnchantListCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public EnchantListCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
        sender.sendMessage(prefix + "§6§l--- Custom Enchants ---");
        for (CustomEnchant ce : CustomEnchant.values()) {
            sender.sendMessage("§b" + ce.getDisplayName() + " §7(" + ce.name() + ")  §7Max: §f" + ce.getMaxLevel());
        }
        return true;
    }
}
