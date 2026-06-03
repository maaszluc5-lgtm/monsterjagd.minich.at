package at.minich.opserver.markt;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MarktSellGUI {

    public static final String TITLE = "§6§lItem verkaufen";

    private final OpServerPlugin plugin;

    public MarktSellGUI(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the sell confirmation GUI.
     *
     * @param player the seller
     * @param item   the item to sell (a clone — not yet removed from inventory)
     * @param amount how many to sell
     * @param price  the price per listing
     */
    public void open(Player player, ItemStack item, int amount, double price) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        ItemStack filler = makeFiller();

        // Top row filler
        for (int i = 0; i <= 8; i++) inv.setItem(i, filler);
        // Bottom row filler
        for (int i = 18; i <= 26; i++) inv.setItem(i, filler);

        // Middle row extra fillers
        inv.setItem(9, filler);
        inv.setItem(10, filler);
        inv.setItem(12, filler);
        inv.setItem(14, filler);
        inv.setItem(16, filler);
        inv.setItem(17, filler);

        // Slot 13: Item preview
        ItemStack preview = item.clone();
        preview.setAmount(amount);
        ItemMeta previewMeta = preview.getItemMeta();
        if (previewMeta == null) previewMeta = Bukkit.getItemFactory().getItemMeta(preview.getType());
        double fee = plugin.getConfig().getDouble("markt.listing-fee", 50.0);
        previewMeta.setLore(Arrays.asList(
                "§7Menge: §f" + amount + "x",
                "§7Preis: §6" + MarktManager.formatCoins(price) + " Coins",
                fee > 0 ? "§7Gebühr: §c" + MarktManager.formatCoins(fee) + " Coins" : "§7Gebühr: §aKostenlos"
        ));
        preview.setItemMeta(previewMeta);
        inv.setItem(13, preview);

        // Slot 11: Confirm (green)
        inv.setItem(11, makeConfirm());

        // Slot 15: Cancel (red)
        inv.setItem(15, makeCancel());

        player.openInventory(inv);
    }

    private ItemStack makeFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeConfirm() {
        ItemStack item = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aVerkaufen");
        meta.setLore(Arrays.asList("§7Klicke zum Bestätigen"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeCancel() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cAbbrechen");
        meta.setLore(Arrays.asList("§7Klicke zum Abbrechen"));
        item.setItemMeta(meta);
        return item;
    }
}
