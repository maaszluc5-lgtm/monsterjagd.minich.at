package at.minich.opserver.markt;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MarktGUI {

    public static final String TITLE = "§6§l🛒 Spieler-Markt";
    private static final int LISTING_SLOTS = 45; // slots 0-44
    private static final int PAGE_SIZE = 45;

    private final OpServerPlugin plugin;

    public MarktGUI(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the MarktGUI for a player with the given state.
     */
    public void open(Player player, int page, MarktFilter filter, MarktSort sort, String searchTerm) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        List<MarktListing> all = plugin.getMarktManager().getListings().stream()
                .filter(l -> filter.matches(l.getItem()))
                .filter(l -> searchTerm == null || matchesSearch(l, searchTerm))
                .sorted(sort.comparator())
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, all.size());
        List<MarktListing> pageListings = all.subList(start, end);

        // Fill listing slots
        ItemStack filler = makeFiller();
        for (int i = 0; i < LISTING_SLOTS; i++) {
            if (i < pageListings.size()) {
                inv.setItem(i, buildListingItem(pageListings.get(i)));
            } else {
                inv.setItem(i, filler);
            }
        }

        // Bottom bar
        inv.setItem(45, makePrevArrow(page > 0, page));
        inv.setItem(46, filler);
        inv.setItem(47, makeFilterButton(filter));
        inv.setItem(48, filler);
        inv.setItem(49, makeInfoBook(player, all.size()));
        inv.setItem(50, filler);
        inv.setItem(51, makeSortButton(sort));
        inv.setItem(52, filler);
        inv.setItem(53, makeNextArrow(page < totalPages - 1, page));

        player.openInventory(inv);
    }

    // -------------------------------------------------------------------------
    // Item builders
    // -------------------------------------------------------------------------

    public ItemStack buildListingItem(MarktListing listing) {
        ItemStack display = listing.getItem().clone();
        display.setAmount(listing.getAmount());
        ItemMeta meta = display.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(display.getType());

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) lore.addAll(meta.getLore());
        if (!lore.isEmpty()) lore.add("§8----------");
        lore.add("§7Verkäufer: §e" + listing.getSellerName());
        lore.add("§7Menge: §f" + listing.getAmount() + "x");
        lore.add("§7Preis: §6" + MarktManager.formatCoins(listing.getPrice()) + " Coins");
        lore.add("§7Eingestellt: §f" + MarktManager.timeAgo(listing.getListedAt()) + " ago");
        lore.add("§8ID: §7" + listing.getId());
        lore.add("");
        lore.add("§aLinksklick: §fKaufen");
        lore.add("§cRechtsklick: §fDetails");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack makeFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makePrevArrow(boolean enabled, int currentPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(enabled ? "§7◀ Zurück" : "§8◀ Zurück");
        meta.setLore(Arrays.asList("§7Seite " + currentPage));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeNextArrow(boolean enabled, int currentPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(enabled ? "§7Weiter ▶" : "§8Weiter ▶");
        meta.setLore(Arrays.asList("§7Seite " + (currentPage + 2)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeFilterButton(MarktFilter current) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eFilter");
        List<String> lore = new ArrayList<>();
        for (MarktFilter f : MarktFilter.values()) {
            lore.add((f == current ? "§a▶ " : "§7  ") + f.getDisplayName());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeInfoBook(Player player, int total) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bMarkt-Info");
        long yours = plugin.getMarktManager().getListingsByPlayer(player.getUniqueId()).size();
        int max = plugin.getConfig().getInt("markt.max-listings-per-player", 10);
        meta.setLore(Arrays.asList(
                "§7Angebote gesamt: §f" + total,
                "§7Deine Angebote: §f" + yours + "/" + max
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSortButton(MarktSort current) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eSort");
        List<String> lore = new ArrayList<>();
        for (MarktSort s : MarktSort.values()) {
            lore.add((s == current ? "§a▶ " : "§7  ") + s.getDisplayName());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private boolean matchesSearch(MarktListing listing, String term) {
        String lower = term.toLowerCase();
        return MarktManager.itemDisplayName(listing.getItem()).toLowerCase().contains(lower)
                || listing.getSellerName().toLowerCase().contains(lower)
                || listing.getItem().getType().name().toLowerCase().replace('_', ' ').contains(lower);
    }
}
