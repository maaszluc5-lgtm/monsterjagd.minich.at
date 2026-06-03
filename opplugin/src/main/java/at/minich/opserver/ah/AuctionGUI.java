package at.minich.opserver.ah;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.markt.MarktManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 54-slot Auktionshaus GUI.
 *
 * Layout:
 *   Slots 0-44  – auction items (9 per page)
 *   Slot 45     – Previous page (Arrow)
 *   Slot 46     – filler
 *   Slot 47     – Filter (Hopper)
 *   Slot 48     – filler
 *   Slot 49     – Info (Book)
 *   Slot 50     – filler
 *   Slot 51     – Sort (Comparator)
 *   Slot 52     – filler
 *   Slot 53     – Next page (Arrow)
 */
public class AuctionGUI {

    public static final String TITLE = "§6§lAuktionshaus";
    private static final int PAGE_SIZE = 45;

    private final OpServerPlugin plugin;

    public AuctionGUI(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the GUI for a player with the given state.
     */
    public void open(Player player, int page, AuctionFilter filter, AuctionSort sort) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        List<AuctionEntry> all = plugin.getAuctionManager().getActiveAuctions().stream()
                .filter(a -> filter.matches(a.getItem()))
                .sorted(sort.comparator())
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, all.size());
        List<AuctionEntry> pageEntries = all.subList(start, end);

        ItemStack filler = makeFiller();

        // Fill auction slots
        for (int i = 0; i < PAGE_SIZE; i++) {
            if (i < pageEntries.size()) {
                inv.setItem(i, buildAuctionItem(pageEntries.get(i)));
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

    public ItemStack buildAuctionItem(AuctionEntry auction) {
        ItemStack display = auction.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(display.getType());

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) lore.addAll(meta.getLore());
        if (!lore.isEmpty()) lore.add("§8----------");

        lore.add("§7Verkäufer: §e" + auction.getSellerName());
        lore.add("§7Startpreis: §f" + MarktManager.formatCoins(auction.getStartPrice()) + " Coins");
        lore.add("§7Aktuelles Gebot: §6" + MarktManager.formatCoins(auction.getCurrentBid()) + " Coins");
        lore.add("§7Höchstbieter: §e" + (auction.hasBids() ? auction.getHighestBidderName() : "Kein Gebot"));
        lore.add("§7Endet in: §f" + formatTimeRemaining(auction.getEndsAt()));
        lore.add("§8ID: §7" + auction.getId());
        lore.add("");
        lore.add("§aLinksklick: §fBieten");
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

    private ItemStack makeFilterButton(AuctionFilter current) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eFilter");
        List<String> lore = new ArrayList<>();
        for (AuctionFilter f : AuctionFilter.values()) {
            lore.add((f == current ? "§a▶ " : "§7  ") + f.getDisplayName());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeInfoBook(Player player, int total) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bAuktionshaus-Info");
        int maxPerPlayer = plugin.getConfig().getInt("ah.max-auctions-per-player", 5);
        long yours = plugin.getAuctionManager().getAuctionsByPlayer(player.getUniqueId()).size();
        boolean hasPending = plugin.getAuctionManager().hasPending(player.getUniqueId());
        List<String> lore = new ArrayList<>(Arrays.asList(
                "§7Aktive Auktionen: §f" + total,
                "§7Deine Auktionen: §f" + yours + "/" + maxPerPlayer
        ));
        if (hasPending) {
            lore.add("§eAbholbereit: §f/ah collect");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSortButton(AuctionSort current) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eSort");
        List<String> lore = new ArrayList<>();
        for (AuctionSort s : AuctionSort.values()) {
            lore.add((s == current ? "§a▶ " : "§7  ") + s.getDisplayName());
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    public static String formatTimeRemaining(long endsAt) {
        long remaining = endsAt - System.currentTimeMillis();
        if (remaining <= 0) return "§cAbgelaufen";
        long hours = TimeUnit.MILLISECONDS.toHours(remaining);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
