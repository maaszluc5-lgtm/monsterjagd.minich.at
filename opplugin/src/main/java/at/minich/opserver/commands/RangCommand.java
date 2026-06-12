package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.ranks.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class RangCommand implements CommandExecutor, TabCompleter {

    private final OpServerPlugin plugin;
    private final RankManager rankManager;
    private static final List<String> RANKS = List.of("neuling", "spieler", "veteran", "elite", "legende", "gott");

    public RangCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cKein Zugriff!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /rang <spieler> <rang>");
            sender.sendMessage("§7Ränge: " + String.join(", ", RANKS));
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cSpieler §e" + args[0] + " §cnicht gefunden!");
            return true;
        }

        String rank = args[1].toLowerCase();
        if (!RANKS.contains(rank)) {
            sender.sendMessage("§cUnbekannter Rang: §e" + rank);
            sender.sendMessage("§7Verfügbare Ränge: " + String.join(", ", RANKS));
            return true;
        }

        // Find RankInfo by key
        RankManager.RankInfo rankInfo = rankManager.getRanks().stream()
                .filter(r -> r.key.equalsIgnoreCase(rank))
                .findFirst().orElse(null);
        if (rankInfo == null) {
            sender.sendMessage("§cRang §e" + rank + " §cnicht in config gefunden!");
            return true;
        }

        rankManager.setRank(target.getUniqueId(), rankInfo);

        // Apply prefix if player is online
        if (target.isOnline()) {
            Player online = (Player) target;
            rankManager.applyRankPrefix(online);
            online.sendMessage("§aDein Rang wurde auf §e" + rank + " §agesetzt!");
        }

        sender.sendMessage("§a✔ Rang §e" + rank + " §aan §e" + target.getName() + " §avergeben!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return RANKS.stream()
                    .filter(r -> r.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
