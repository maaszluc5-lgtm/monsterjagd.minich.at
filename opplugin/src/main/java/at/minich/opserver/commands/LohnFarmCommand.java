package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /lohnfarm — teleports the player to the salary farm world.
 */
public class LohnFarmCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public LohnFarmCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cNur Spieler können diesen Befehl nutzen.");
            return true;
        }

        plugin.getSalaryFarmManager().teleportPlayer(player);
        return true;
    }
}
