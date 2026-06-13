package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.quests.Quest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class QuestCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public QuestCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler.");
            return true;
        }

        List<Quest> quests = plugin.getQuestManager().getPlayerQuests(player.getUniqueId());
        player.sendMessage("§6§l--- Tägliche Quests ---");
        for (int i = 0; i < quests.size(); i++) {
            Quest q = quests.get(i);
            String bar = buildBar(q.getProgress(), q.getGoal());
            if (q.isCompleted()) {
                player.sendMessage("§a[Quest " + (i + 1) + "] §7" + q.getType().displayName + ": §a✔ Abgeschlossen");
            } else {
                player.sendMessage("§e[Quest " + (i + 1) + "] §7" + q.getType().displayName + ": §a"
                        + q.getProgress() + "§7/§a" + q.getGoal() + " §8" + bar
                        + " §7(+" + (int) q.getReward() + " Coins)");
            }
        }
        return true;
    }

    private String buildBar(int progress, int goal) {
        int total = 10;
        int filled = (int) Math.round((double) progress / goal * total);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < total; i++) {
            sb.append(i < filled ? "§a█" : "§8░");
        }
        sb.append("§8]");
        return sb.toString();
    }
}
