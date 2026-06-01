package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.kits.KitManager;
import at.minich.opserver.kits.KitManager.KitInfo;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class KitListCommand implements CommandExecutor {

    private final KitManager kitManager;

    public KitListCommand(OpServerPlugin plugin) {
        this.kitManager = plugin.getKitManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("§8=== §6Available Kits §8===");

        UUID uuid = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;

        for (KitInfo kit : kitManager.getKits().values()) {
            // Permission check for display
            boolean hasPermission = kit.permission == null
                    || (sender instanceof Player && sender.hasPermission(kit.permission))
                    || sender.isOp();

            String cooldownStr = "";
            if (uuid != null) {
                long cd = kitManager.getCooldownSeconds(uuid, kit.name);
                if (cd > 0) {
                    long h = cd / 3600;
                    long m = (cd % 3600) / 60;
                    cooldownStr = " §c(cooldown: " + h + "h " + m + "m)";
                } else {
                    cooldownStr = " §a(ready)";
                }
            }

            String permStr = (kit.permission != null) ? " §7[" + kit.permission + "]" : "";
            String coinsStr = kit.coins > 0 ? " §e+¢" + String.format("%.0f", kit.coins) : "";

            if (hasPermission) {
                sender.sendMessage("§6" + kit.name + permStr + coinsStr + cooldownStr);
            } else {
                sender.sendMessage("§8" + kit.name + " §c(no permission)" + permStr);
            }
        }
        return true;
    }
}
