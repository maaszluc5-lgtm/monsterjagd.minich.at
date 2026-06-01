package at.minich.opserver.commands;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Cancels all clicks inside the Trophy GUI to prevent item theft.
 */
public class TrophyGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() != null) return; // has a holder — not our GUI
        if (inv.getView().getTitle().startsWith("§6§lPokale von ")) {
            event.setCancelled(true);

            // Close on barrier click
            if (event.getRawSlot() == 49) {
                event.getWhoClicked().closeInventory();
            }
        }
    }
}
