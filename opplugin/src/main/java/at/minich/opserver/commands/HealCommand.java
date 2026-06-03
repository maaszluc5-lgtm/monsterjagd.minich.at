package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HealCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public HealCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (args.length >= 1) {
            if (sender instanceof Player p && !p.isOp() && !p.hasPermission("opserver.admin")) {
                sender.sendMessage(prefix + "§cYou don't have permission to heal other players.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(prefix + "§cPlayer not found or offline.");
                return true;
            }
            healPlayer(target);
            target.sendMessage(prefix + "§aYou have been healed.");
            sender.sendMessage(prefix + "§aHealed §e" + target.getName() + "§a.");
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "§cUsage: /heal <player>");
                return true;
            }
            healPlayer(player);
            player.sendMessage(prefix + "§aYou have been healed.");
        }
        return true;
    }

    private void healPlayer(Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(maxHealth);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
    }
}
