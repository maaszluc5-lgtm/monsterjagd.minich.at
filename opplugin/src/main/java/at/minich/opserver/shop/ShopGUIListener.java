package at.minich.opserver.shop;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ShopGUIListener implements Listener {

    private final OpServerPlugin plugin;
    private final ShopGUI shopGUI;
    private SellInventoryGUI sellInventoryGUI;

    public ShopGUIListener(OpServerPlugin plugin, ShopGUI shopGUI) {
        this.plugin = plugin;
        this.shopGUI = shopGUI;
    }

    public void setSellInventoryGUI(SellInventoryGUI sellInventoryGUI) {
        this.sellInventoryGUI = sellInventoryGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (!title.equals(ShopGUI.CATEGORY_TITLE) && !title.startsWith(ShopGUI.ITEM_TITLE_PREFIX)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        // Category overview
        if (title.equals(ShopGUI.CATEGORY_TITLE)) {
            // Open sell inventory GUI
            if (event.getSlot() == 49) {
                if (sellInventoryGUI != null) sellInventoryGUI.open(player);
                return;
            }
            // Find clicked category
            for (ServerShop.Category cat : ServerShop.Category.values()) {
                if (meta.getDisplayName().equals(cat.display)) {
                    shopGUI.openCategory(player, cat, 0);
                    return;
                }
            }
            return;
        }

        // Item page
        if (title.startsWith(ShopGUI.ITEM_TITLE_PREFIX)) {
            String dispName = meta.getDisplayName();

            // Back button
            if (dispName.equals("§c← Zurück zur Übersicht")) {
                shopGUI.openCategories(player);
                return;
            }

            // Navigation arrows
            List<String> lore = meta.getLore();
            if (lore != null && !lore.isEmpty()) {
                String last = lore.get(lore.size() - 1);
                if (last.startsWith("§8page:")) {
                    String[] parts = last.replace("§8", "").split(":");
                    try {
                        int targetPage = Integer.parseInt(parts[1]);
                        ServerShop.Category cat = ServerShop.Category.valueOf(parts[3]);
                        shopGUI.openCategory(player, cat, targetPage);
                    } catch (Exception ignored) {}
                    return;
                }
            }

            // Buy item
            ShopItem shopItem = ServerShop.findItem(clicked.getType());
            if (shopItem == null) return;

            int amount = event.isLeftClick() ? 1 : 64;
            double totalCost = shopItem.buyPrice() * amount;
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());

            if (balance < totalCost) {
                player.sendMessage("§cNicht genug Coins! Du brauchst §e" + String.format("%.1f", totalCost)
                        + " §cCoins, hast aber §e" + String.format("%.1f", balance) + "§c.");
                return;
            }

            plugin.getEconomyManager().withdraw(player.getUniqueId(), totalCost);
            plugin.getBankManager().addShopEarnings(totalCost);
            ItemStack purchase = new ItemStack(shopItem.material(), amount);
            player.getInventory().addItem(purchase).values().forEach(
                    leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover)
            );
            player.sendMessage("§a✔ §e" + amount + "x " + shopItem.material().name().replace("_", " ").toLowerCase()
                    + " §agekauft für §e" + String.format("%.1f", totalCost) + " §aCoins!");
        }
    }

    private void sellHeldItem(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage("§cHalte ein Item in der Hand um es zu verkaufen!");
            return;
        }
        ShopItem shopItem = ServerShop.findItem(held.getType());
        if (shopItem == null) {
            player.sendMessage("§cDieses Item kann nicht verkauft werden!");
            return;
        }
        int amount = held.getAmount();
        double total = shopItem.sellPrice() * amount;
        player.getInventory().setItemInMainHand(null);
        plugin.getBankManager().addMarktBalance(player.getUniqueId(), total);
        player.sendMessage("§a✔ §e" + amount + "x " + held.getType().name().replace("_", " ").toLowerCase()
                + " §averkauft für §e" + String.format("%.1f", total) + " §aCoins! (→ §6Markt-Konto§a)");
    }
}
