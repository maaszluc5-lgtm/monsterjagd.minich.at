package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.crates.CrateType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GiveCrateCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public GiveCrateCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!sender.hasPermission("opserver.admin")) {
            sender.sendMessage(prefix + plugin.getConfig().getString("messages.no-permission", "§cKeine Berechtigung."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix + "§cUsage: /givecrate <player> <common|rare|epic|legendary> [amount]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(prefix + plugin.getConfig().getString("messages.player-not-found", "§cSpieler nicht gefunden."));
            return true;
        }

        CrateType type;
        try {
            type = CrateType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(prefix + "§cUnbekannter Kisten-Typ: §f" + args[1] + "§c. Verfügbar: common, rare, epic, legendary");
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                sender.sendMessage(prefix + "§cUngültige Anzahl: §f" + args[2]);
                return true;
            }
        }

        ItemStack crate = plugin.getCrateManager().buildCrateItem(type);
        crate.setAmount(amount);
        target.getInventory().addItem(crate);

        sender.sendMessage(prefix + "§aGegeben: §f" + amount + "x " + type.getDisplayName() + " §aan §e" + target.getName());
        target.sendMessage(prefix + "§aDu hast §f" + amount + "x " + type.getDisplayName() + " §aerhalten!");
        return true;
    }
}
