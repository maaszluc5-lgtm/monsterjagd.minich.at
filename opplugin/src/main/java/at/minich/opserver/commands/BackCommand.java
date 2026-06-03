package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class BackCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final Map<UUID, Location> lastLocations;

    public BackCommand(OpServerPlugin plugin, Map<UUID, Location> lastLocations) {
        this.plugin = plugin;
        this.lastLocations = lastLocations;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cOnly players can use this command.");
            return true;
        }

        Location loc = lastLocations.get(player.getUniqueId());
        if (loc == null) {
            sender.sendMessage(prefix + "§cKein vorheriger Standort gefunden.");
            return true;
        }

        player.teleport(loc);
        sender.sendMessage(prefix + "§aTeleportiert zum letzten Standort.");
        return true;
    }
}
