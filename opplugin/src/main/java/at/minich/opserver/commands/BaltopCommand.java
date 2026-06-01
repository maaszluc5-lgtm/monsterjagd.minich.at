package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaltopCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public BaltopCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        List<Map.Entry<UUID, Double>> top = plugin.getEconomyManager().getTopBalances(10);

        sender.sendMessage(prefix + "§6§l--- Top 10 Richest Players ---");
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<UUID, Double> entry = top.get(i);
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : entry.getKey().toString();
            sender.sendMessage("§e" + (i + 1) + ". §f" + name + " §7- §a¢" + String.format("%.2f", entry.getValue()));
        }

        if (top.isEmpty()) {
            sender.sendMessage("§7No data available.");
        }
        return true;
    }
}
