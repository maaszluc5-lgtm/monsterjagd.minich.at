package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.shop.ShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final ShopGUI shopGUI;

    public ShopCommand(OpServerPlugin plugin, ShopGUI shopGUI) {
        this.shopGUI = shopGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        shopGUI.openCategories(player);
        return true;
    }
}
