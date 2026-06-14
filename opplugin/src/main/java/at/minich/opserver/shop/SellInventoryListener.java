package at.minich.opserver.shop;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SellInventoryListener implements Listener {

    private final OpServerPlugin plugin;
    private final SellInventoryGUI gui;
    private final ShopGUI shopGUI;

    public SellInventoryListener(OpServerPlugin plugin, SellInventoryGUI gui, ShopGUI shopGUI) {
        this.plugin = plugin;
        this.gui = gui;
        this.shopGUI = shopGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(SellInventoryGUI.TITLE)) return;
        event.setCancelled(true);

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();

        // Back button
        if (slot == 53) { shopGUI.openCategories(player); return; }

        // Sell ALL button (slot 49)
        if (slot == 49) {
            double total = 0;
            int count = 0;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType().isAir()) continue;
                ShopItem shopItem = ServerShop.findItem(item.getType());
                if (shopItem == null) continue;
                total += shopItem.sellPrice() * item.getAmount();
                count += item.getAmount();
                player.getInventory().setItem(i, null);
            }
            plugin.getBankManager().addMarktBalance(player.getUniqueId(), total);
            player.sendMessage("§a✔ §e" + count + " §aItems verkauft für §e" + String.format("%.1f", total) + " §aCoins! (→ §6Markt-Konto§a)");
            player.closeInventory();
            return;
        }

        // Item slots 0-44
        if (slot < 45 && clicked != null && !clicked.getType().isAir()) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;
            String displayName = meta.getDisplayName();
            // Find original type from display name
            ShopItem shopItem = ServerShop.findItem(clicked.getType());
            if (shopItem == null) return;

            if (event.isRightClick()) {
                // Sell ALL stacks of this type
                double total = 0; int count = 0;
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (item == null || item.getType() != clicked.getType()) continue;
                    total += shopItem.sellPrice() * item.getAmount();
                    count += item.getAmount();
                    player.getInventory().setItem(i, null);
                }
                plugin.getBankManager().addMarktBalance(player.getUniqueId(), total);
                String name = clicked.getType().name().replace("_", " ").toLowerCase();
                player.sendMessage("§a✔ §e" + count + "x " + name + " §averkauft für §e" + String.format("%.1f", total) + " §aCoins! (→ §6Markt-Konto§a)");
            } else {
                // Sell this stack only
                ItemStack real = player.getInventory().getItem(slot);
                if (real == null || real.getType() != clicked.getType()) return;
                double total = shopItem.sellPrice() * real.getAmount();
                player.getInventory().setItem(slot, null);
                plugin.getBankManager().addMarktBalance(player.getUniqueId(), total);
                String name = real.getType().name().replace("_", " ").toLowerCase();
                player.sendMessage("§a✔ §e" + real.getAmount() + "x " + name + " §averkauft für §e" + String.format("%.1f", total) + " §aCoins! (→ §6Markt-Konto§a)");
            }
            // Refresh GUI
            gui.open(player);
        }
    }
}
