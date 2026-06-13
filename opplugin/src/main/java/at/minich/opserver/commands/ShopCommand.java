package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.shop.SellInventoryGUI;
import at.minich.opserver.shop.ShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final ShopGUI shopGUI;
    private final SellInventoryGUI sellInventoryGUI;

    public ShopCommand(OpServerPlugin plugin, ShopGUI shopGUI, SellInventoryGUI sellInventoryGUI) {
        this.shopGUI = shopGUI;
        this.sellInventoryGUI = sellInventoryGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("sell")) {
            sellInventoryGUI.open(player);
        } else {
            shopGUI.openCategories(player);
        }
        return true;
    }
}
