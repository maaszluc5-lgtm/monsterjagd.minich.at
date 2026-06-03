package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GiveCrystalsCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public GiveCrystalsCommand(OpServerPlugin plugin) {
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
            sender.sendMessage(prefix + "§cUsage: /givekristalle <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(prefix + plugin.getConfig().getString("messages.player-not-found", "§cSpieler nicht gefunden."));
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + "§cUngültige Zahl: §f" + args[1]);
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage(prefix + "§cDer Betrag muss positiv sein.");
            return true;
        }

        plugin.getCrystalManager().addCrystals(target.getUniqueId(), amount);
        sender.sendMessage(prefix + "§aGegeben: §b" + amount + " Kristalle §aan §e" + target.getName());
        target.sendMessage(prefix + "§aDu hast §b" + amount + " §aKristalle erhalten!");
        return true;
    }
}
