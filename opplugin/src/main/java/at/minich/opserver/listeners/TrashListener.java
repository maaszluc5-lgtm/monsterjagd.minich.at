package at.minich.opserver.listeners;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class TrashListener implements Listener {

    private final OpServerPlugin plugin;

    public TrashListener(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (plugin.isTrashInventory(inv)) {
            inv.clear();
            plugin.unregisterTrashInventory(inv);
        }
    }
}
