package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SeenCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public SeenCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (args.length == 0) {
            sender.sendMessage(prefix + "§cUsage: /seen <player>");
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(prefix + "§cPlayer not found.");
            return true;
        }

        String playerName = target.getName() != null ? target.getName() : args[0];

        // Check if online
        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null) {
            sender.sendMessage(prefix + "§e" + playerName + " §ais currently §aonline§a.");
            return true;
        }

        String path = "players/" + target.getUniqueId() + ".yml";
        YamlConfiguration cfg = plugin.getDataManager().loadYaml(path);
        String lastSeen = cfg.getString("last-seen");

        if (lastSeen == null) {
            sender.sendMessage(prefix + "§cNo data found for §e" + playerName + "§c.");
            return true;
        }

        try {
            Instant instant = Instant.parse(lastSeen);
            String formatted = FORMATTER.format(instant);
            sender.sendMessage(prefix + "§e" + playerName + " §7was last seen on §e" + formatted + "§7.");
        } catch (Exception e) {
            sender.sendMessage(prefix + "§e" + playerName + " §7was last seen: §e" + lastSeen);
        }
        return true;
    }
}
