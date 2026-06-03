package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class TpDenyCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final Map<UUID, UUID> pendingRequests;
    private final Map<UUID, Integer> expiryTasks;

    public TpDenyCommand(OpServerPlugin plugin,
                         Map<UUID, UUID> pendingRequests,
                         Map<UUID, Integer> expiryTasks) {
        this.plugin = plugin;
        this.pendingRequests = pendingRequests;
        this.expiryTasks = expiryTasks;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!(sender instanceof Player target)) {
            sender.sendMessage(prefix + "§cOnly players can use this command.");
            return true;
        }

        UUID requesterUuid = pendingRequests.get(target.getUniqueId());
        if (requesterUuid == null) {
            sender.sendMessage(prefix + "§cDu hast keine offene Teleport-Anfrage.");
            return true;
        }

        Integer taskId = expiryTasks.remove(target.getUniqueId());
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        pendingRequests.remove(target.getUniqueId());

        Player requester = Bukkit.getPlayer(requesterUuid);
        target.sendMessage(prefix + "§cTeleport-Anfrage abgelehnt.");
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(prefix + "§f" + target.getName() + " §chat deine Teleport-Anfrage abgelehnt.");
        }
        return true;
    }
}
