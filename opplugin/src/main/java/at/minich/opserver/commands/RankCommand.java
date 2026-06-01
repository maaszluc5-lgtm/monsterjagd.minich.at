package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.ranks.RankManager;
import at.minich.opserver.ranks.RankManager.RankInfo;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RankCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final RankManager rankManager;

    public RankCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
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
                sender.sendMessage("§cUsage: /rank <player>");
                return true;
            }
            Player p = (Player) sender;
            targetUUID = p.getUniqueId();
            targetName = p.getName();
        }

        RankInfo current = rankManager.getRank(targetUUID);
        RankInfo next = rankManager.getNextRank(targetUUID);
        long hours = rankManager.getPlaytimeHours(targetUUID);

        sender.sendMessage("§8=== §6Rank: " + targetName + " §8===");
        if (current != null) {
            sender.sendMessage("§7Current rank: " + current.prefix + " §7(" + current.key + ")");
            sender.sendMessage("§7Playtime: §e" + hours + "h");
            if (next != null) {
                long needed = next.requiredHours - hours;
                sender.sendMessage("§7Next rank: §b" + next.prefix + " §7(in §e" + needed + "h§7)");
            } else {
                sender.sendMessage("§7You have reached the §4§lhighest rank§7!");
            }
            sender.sendMessage("§7Salary multiplier: §a" + current.salaryMultiplier + "x");
        } else {
            sender.sendMessage("§7No rank data found.");
        }
        return true;
    }
}
