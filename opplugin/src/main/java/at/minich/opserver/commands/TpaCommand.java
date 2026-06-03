package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    // Maps target UUID -> requester UUID (pending requests)
    private final Map<UUID, UUID> pendingRequests;
    // Maps target UUID -> task ID for expiry
    private final Map<UUID, Integer> expiryTasks;

    public TpaCommand(OpServerPlugin plugin,
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

        if (!(sender instanceof Player requester)) {
            sender.sendMessage(prefix + "§cOnly players can use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(prefix + "§cUsage: /tpa <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(prefix + plugin.getConfig().getString("messages.player-not-found", "§cPlayer not found."));
            return true;
        }

        if (target.equals(requester)) {
            sender.sendMessage(prefix + "§cDu kannst keine Anfrage an dich selbst senden.");
            return true;
        }

        // Cancel any existing expiry task for that target
        if (expiryTasks.containsKey(target.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(expiryTasks.get(target.getUniqueId()));
        }

        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());

        // Schedule expiry after 30 seconds
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (requester.getUniqueId().equals(pendingRequests.get(target.getUniqueId()))) {
                pendingRequests.remove(target.getUniqueId());
                expiryTasks.remove(target.getUniqueId());
                if (requester.isOnline()) {
                    requester.sendMessage(prefix + "§eDeine Teleport-Anfrage an §f" + target.getName() + " §eist abgelaufen.");
                }
                if (target.isOnline()) {
                    target.sendMessage(prefix + "§eDie Teleport-Anfrage von §f" + requester.getName() + " §eist abgelaufen.");
                }
            }
        }, 20L * 30).getTaskId();

        expiryTasks.put(target.getUniqueId(), taskId);

        requester.sendMessage(prefix + "§aTeleport-Anfrage an §f" + target.getName() + " §agesendet. (30s)");
        target.sendMessage(prefix + "§f" + requester.getName() + " §emöchte zu dir teleportieren. §a/tpaccept §eoder §c/tpdeny");
        return true;
    }
}
