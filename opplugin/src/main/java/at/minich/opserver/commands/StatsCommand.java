package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.economy.BankManager;
import at.minich.opserver.economy.EconomyManager;
import at.minich.opserver.ranks.RankManager;
import at.minich.opserver.ranks.RankManager.RankInfo;
import at.minich.opserver.util.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StatsCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final BankManager bankManager;
    private final RankManager rankManager;

    public StatsCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.economyManager = plugin.getEconomyManager();
        this.bankManager = plugin.getBankManager();
        this.rankManager = plugin.getRankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        UUID targetUUID;
        String targetName;

        if (args.length >= 1) {
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
            if (op == null || (!op.hasPlayedBefore() && !op.isOnline())) {
                sender.sendMessage("§cPlayer not found: " + args[0]);
                return true;
            }
            targetUUID = op.getUniqueId();
            targetName = op.getName() != null ? op.getName() : args[0];
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cUsage: /stats <player>");
                return true;
            }
            Player p = (Player) sender;
            targetUUID = p.getUniqueId();
            targetName = p.getName();
        }

        String path = "players/" + targetUUID + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);

        long playtimeSeconds = cfg.getLong("playtime-seconds", 0);
        long hours = playtimeSeconds / 3600;
        long minutes = (playtimeSeconds % 3600) / 60;

        String joinDate = cfg.getString("join-date", "Unknown");
        // Trim to date only if ISO format
        if (joinDate.length() > 10 && joinDate.contains("T")) {
            joinDate = joinDate.substring(0, 10);
        }

        RankInfo rank = rankManager.getRank(targetUUID);
        String rankDisplay = rank != null ? rank.prefix + " §7(" + rank.key + ")" : "§7None";

        double balance = economyManager.getBalance(targetUUID);
        double bankBalance = bankManager.getBalance(targetUUID);

        long kills = cfg.getLong("stats.kills", 0);
        long deaths = cfg.getLong("stats.deaths", 0);
        long blocksMined = cfg.getLong("stats.blocks-mined", 0);

        sender.sendMessage("§8========= §6Stats: " + targetName + " §8=========");
        sender.sendMessage("§7Playtime:    §e" + hours + "h " + minutes + "m");
        sender.sendMessage("§7Join date:   §e" + joinDate);
        sender.sendMessage("§7Rank:        " + rankDisplay);
        sender.sendMessage("§7Balance:     §a¢" + String.format("%.2f", balance));
        sender.sendMessage("§7Bank:        §a¢" + String.format("%.2f", bankBalance));
        sender.sendMessage("§7Kills:       §c" + kills);
        sender.sendMessage("§7Deaths:      §c" + deaths);
        sender.sendMessage("§7Blocks mined:§b " + blocksMined);
        sender.sendMessage("§8============================================");
        return true;
    }
}
