package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CrystalsCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public CrystalsCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "§cNur Spieler können diesen Befehl ausführen.");
                return true;
            }
            long crystals = plugin.getCrystalManager().getCrystals(player.getUniqueId());
            player.sendMessage(prefix + "§bDein Kristall-Guthaben: §3" + crystals + " §bKristalle");
            return true;
        }

        // /kristalle <player>
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(prefix + plugin.getConfig().getString("messages.player-not-found", "§cSpieler nicht gefunden."));
            return true;
        }
        long crystals = plugin.getCrystalManager().getCrystals(target.getUniqueId());
        sender.sendMessage(prefix + "§bKristalle von §3" + target.getName() + "§b: §3" + crystals);
        return true;
    }
}
