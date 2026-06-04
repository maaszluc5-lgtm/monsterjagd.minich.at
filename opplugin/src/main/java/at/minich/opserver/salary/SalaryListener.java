package at.minich.opserver.salary;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Handles click events inside the Salary GUI.
 * All clicks are cancelled (read-only); clicking the Barrier closes the inventory.
 */
public class SalaryListener implements Listener {

    private static final String GUI_TITLE = "§6§lDein Lohn";

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        if (!GUI_TITLE.equals(event.getView().getTitle())) return;

        event.setCancelled(true);

        // Slot 40 = Barrier → close
        if (event.getRawSlot() == 40 && event.getCurrentItem() != null
                && event.getCurrentItem().getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }
}
