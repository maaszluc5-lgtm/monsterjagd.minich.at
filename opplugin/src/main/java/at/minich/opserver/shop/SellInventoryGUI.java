package at.minich.opserver.shop;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SellInventoryGUI {

    public static final String TITLE = "§6§lInventar verkaufen";

    private final OpServerPlugin plugin;

    public SellInventoryGUI(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Slots 0-44: player inventory items (sellable only)
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < Math.min(contents.length, 45); i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            ShopItem shopItem = ServerShop.findItem(item.getType());
            if (shopItem == null) continue; // not sellable
            inv.setItem(i, makeDisplayItem(item, shopItem));
        }

        // Bottom bar
        inv.setItem(45, makeGlass());
        inv.setItem(46, makeGlass());
        inv.setItem(47, makeSellAllSameButton());
        inv.setItem(48, makeGlass());
        inv.setItem(49, makeSellAllButton(player));
        inv.setItem(50, makeGlass());
        inv.setItem(51, makeGlass());
        inv.setItem(52, makeGlass());
        inv.setItem(53, makeBackButton());

        player.openInventory(inv);
    }

    private ItemStack makeDisplayItem(ItemStack original, ShopItem shopItem) {
        ItemStack display = original.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;
        String name = original.getType().name().replace("_", " ").toLowerCase();
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        meta.setDisplayName("§f" + name);
        List<String> lore = new ArrayList<>();
        lore.add("§7Menge: §e" + original.getAmount());
        lore.add("§7Verkaufspreis: §e" + formatPrice(shopItem.sellPrice() * original.getAmount()) + " Coins");
        lore.add("");
        lore.add("§aLinksklick §7= diesen Stapel verkaufen");
        lore.add("§aRechtsklick §7= alle §e" + name + " §averkaufen");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack makeSellAllButton(Player player) {
        double total = calcTotalValue(player);
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§lAlles verkaufen");
            List<String> lore = new ArrayList<>();
            lore.add("§7Verkauft alle verkaufbaren Items");
            lore.add("§7aus deinem Inventar auf einmal.");
            lore.add("");
            lore.add("§7Gesamtwert: §e" + formatPrice(total) + " Coins");
            lore.add("");
            lore.add("§eKlicken zum Verkaufen!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeSellAllSameButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lAlle gleichen verkaufen");
            List<String> lore = new ArrayList<>();
            lore.add("§7Rechtsklick auf ein Item =");
            lore.add("§7alle Stapel dieses Typs verkaufen.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName("§c← Zurück"); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack makeGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    public double calcTotalValue(Player player) {
        double total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            ShopItem shopItem = ServerShop.findItem(item.getType());
            if (shopItem != null) total += shopItem.sellPrice() * item.getAmount();
        }
        return total;
    }

    private String formatPrice(double price) {
        if (price >= 1000000) return String.format("%.1fM", price / 1000000);
        if (price >= 1000) return String.format("%.1fK", price / 1000);
        return String.format("%.1f", price);
    }
}
