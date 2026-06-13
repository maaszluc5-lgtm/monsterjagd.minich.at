package at.minich.opserver.shop;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI {

    public static final String CATEGORY_TITLE = "§6§lServer-Shop";
    public static final String ITEM_TITLE_PREFIX = "§6§lShop: ";

    private final OpServerPlugin plugin;

    public ShopGUI(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    public void openCategories(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, CATEGORY_TITLE);
        ItemStack glass = makeGlass();
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        int[] slots = {10, 12, 14, 16, 28, 30, 32, 34, 22};
        ServerShop.Category[] cats = ServerShop.Category.values();
        for (int i = 0; i < cats.length && i < slots.length; i++) {
            inv.setItem(slots[i], makeCategoryItem(cats[i]));
        }

        // Sell info
        ItemStack info = new ItemStack(Material.EMERALD);
        ItemMeta m = info.getItemMeta();
        if (m != null) {
            m.setDisplayName("§a§lInventar verkaufen");
            List<String> lore = new ArrayList<>();
            lore.add("§7Klicke um dein Inventar zu öffnen");
            lore.add("§7und Items schnell zu verkaufen.");
            lore.add("§7§oDu kannst alles in 3 Klicks leeren!");
            m.setLore(lore);
            info.setItemMeta(m);
        }
        inv.setItem(49, info);

        player.openInventory(inv);
    }

    public void openCategory(Player player, ServerShop.Category category, int page) {
        List<ShopItem> items = ServerShop.getItems(category);
        int perPage = 45;
        int totalPages = (int) Math.ceil((double) items.size() / perPage);
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, ITEM_TITLE_PREFIX + category.display + " §8(Seite " + (page + 1) + ")");
        ItemStack glass = makeGlass();
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        int start = page * perPage;
        int end = Math.min(start + perPage, items.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, makeShopItem(items.get(i)));
        }

        // Navigation
        if (page > 0) inv.setItem(45, makeNavItem(Material.ARROW, "§7← Zurück", page - 1, category));
        inv.setItem(49, makeBackItem());
        if (page < totalPages - 1) inv.setItem(53, makeNavItem(Material.ARROW, "§7Weiter →", page + 1, category));

        player.openInventory(inv);
    }

    private ItemStack makeCategoryItem(ServerShop.Category cat) {
        ItemStack item = new ItemStack(cat.icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(cat.display);
        List<String> lore = new ArrayList<>();
        lore.add("§7" + ServerShop.getItems(cat).size() + " Items");
        lore.add("§eKlicken zum Öffnen");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeShopItem(ShopItem shopItem) {
        ItemStack item = new ItemStack(shopItem.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        String name = shopItem.material().name().replace("_", " ").toLowerCase();
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        meta.setDisplayName("§f" + name);
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Kaufpreis: §e" + formatPrice(shopItem.buyPrice()) + " Coins");
        lore.add("§7Verkaufspreis: §e" + formatPrice(shopItem.sellPrice()) + " Coins");
        lore.add("");
        lore.add("§aLinksklick §7= §eKaufen (1)");
        lore.add("§aRechtsklick §7= §eKaufen (64)");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeNavItem(Material mat, String name, int targetPage, ServerShop.Category cat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add("§8page:" + targetPage + ":cat:" + cat.name());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeBackItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName("§c← Zurück zur Übersicht"); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack makeGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private String formatPrice(double price) {
        if (price >= 1000000) return String.format("%.1fM", price / 1000000);
        if (price >= 1000) return String.format("%.1fK", price / 1000);
        return String.format("%.1f", price);
    }
}
