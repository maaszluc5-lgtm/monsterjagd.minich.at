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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReportCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5 * 60 * 1000L;

    public ReportCommand(OpServerPlugin plugin) {
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

        if (args.length < 2) {
            player.sendMessage(prefix + "§cUsage: /report <player> <reason>");
            return true;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid)) {
            long elapsed = now - cooldowns.get(uuid);
            if (elapsed < COOLDOWN_MS) {
                long remaining = (COOLDOWN_MS - elapsed) / 1000;
                player.sendMessage(prefix + "§cBitte warte noch §e" + remaining + " §cSekunden.");
                return true;
            }
        }

        String targetName = args[0];
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) reasonBuilder.append(" ");
            reasonBuilder.append(args[i]);
        }
        String reason = reasonBuilder.toString();

        cooldowns.put(uuid, now);

        // Save to reports.yml
        File reportsFile = new File(plugin.getDataFolder(), "reports.yml");
        YamlConfiguration cfg = reportsFile.exists()
                ? YamlConfiguration.loadConfiguration(reportsFile)
                : new YamlConfiguration();

        String key = "reports." + now;
        cfg.set(key + ".reporter", player.getName());
        cfg.set(key + ".reported", targetName);
        cfg.set(key + ".reason", reason);
        cfg.set(key + ".timestamp", Instant.now().toString());
        try {
            cfg.save(reportsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save reports.yml: " + e.getMessage());
        }

        // Notify OPs
        String opMsg = "§c[REPORT] §e" + player.getName() + " §7meldet §e" + targetName + "§7: §f" + reason;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.isOp()) online.sendMessage(opMsg);
        }

        player.sendMessage(prefix + "§aDein Report wurde abgesendet.");
        return true;
    }
}
