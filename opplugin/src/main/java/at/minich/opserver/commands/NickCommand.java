package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NickCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public NickCommand(OpServerPlugin plugin) {
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

        if (args.length == 0) {
            player.sendMessage(prefix + "§cUsage: /nick <name|off>");
            return true;
        }

        String path = "players/" + player.getUniqueId() + ".yml";
        YamlConfiguration cfg = plugin.getDataManager().loadYaml(path);

        if (args[0].equalsIgnoreCase("off")) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            cfg.set("nick", null);
            plugin.getDataManager().saveYaml(cfg, path);
            player.sendMessage(prefix + "§aYour nickname has been reset.");
        } else {
            String nick = String.join(" ", args).replace("&", "§");
            player.setDisplayName(nick);
            player.setPlayerListName(nick);
            cfg.set("nick", nick);
            plugin.getDataManager().saveYaml(cfg, path);
            player.sendMessage(prefix + "§aYour nickname is now: " + nick);
        }
        return true;
    }
}
