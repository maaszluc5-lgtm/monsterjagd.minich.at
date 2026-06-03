package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public class HelpopCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public HelpopCommand(OpServerPlugin plugin) {
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

        if (args.length < 1) {
            player.sendMessage(prefix + "§cUsage: /helpop <message>");
            return true;
        }

        StringBuilder msgBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) msgBuilder.append(" ");
            msgBuilder.append(args[i]);
        }
        String message = msgBuilder.toString();

        String opMsg = "§c[HELPOP] §e" + player.getName() + "§7: §f" + message;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOp()) online.sendMessage(opMsg);
        }

        // Also log to helpop.yml
        File helpopFile = new File(plugin.getDataFolder(), "helpop.yml");
        YamlConfiguration cfg = helpopFile.exists()
                ? YamlConfiguration.loadConfiguration(helpopFile)
                : new YamlConfiguration();

        long now = System.currentTimeMillis();
        String key = "helpop." + now;
        cfg.set(key + ".player", player.getName());
        cfg.set(key + ".message", message);
        cfg.set(key + ".timestamp", Instant.now().toString());
        try {
            cfg.save(helpopFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save helpop.yml: " + e.getMessage());
        }

        player.sendMessage(prefix + "§aDeine Nachricht wurde an alle OPs gesendet.");
        return true;
    }
}
