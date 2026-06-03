package at.minich.opserver.markt;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MarktGUIListener implements Listener {

    // Per-player pending buy confirmation: listing ID -> confirm timestamp
    private final Map<UUID, String> pendingBuy = new HashMap<>();
    private final Map<UUID, Long> pendingBuyTime = new HashMap<>();

    // Per-player sell session: waiting for price input
    private final Map<UUID, SellSession> pendingSell = new HashMap<>();

    // Per-player GUI state
    private final Map<UUID, GuiState> guiStates = new HashMap<>();

    private final OpServerPlugin plugin;
    private final MarktGUI marktGUI;
    private final MarktSellGUI marktSellGUI;

    public MarktGUIListener(OpServerPlugin plugin, MarktGUI marktGUI, MarktSellGUI marktSellGUI) {
        this.plugin = plugin;
        this.marktGUI = marktGUI;
        this.marktSellGUI = marktSellGUI;
    }

    // -------------------------------------------------------------------------
    // Sell session helpers
    // -------------------------------------------------------------------------

    public void startSellSession(Player player, ItemStack item, int amount) {
        pendingSell.put(player.getUniqueId(), new SellSession(item, amount));
        player.sendMessage("§6Bitte gib den Preis für §e" + amount + "x §f"
                + MarktManager.itemDisplayName(item) + " §6im Chat ein:");
        player.sendMessage("§7(Tippe §cAbbrechen §7um abzubrechen)");
    }

    public boolean hasPendingSell(UUID uuid) {
        return pendingSell.containsKey(uuid);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!pendingSell.containsKey(uuid)) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();

        if (msg.equalsIgnoreCase("Abbrechen")) {
            pendingSell.remove(uuid);
            player.sendMessage("§cVerkauf abgebrochen.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(msg.replace(",", "."));
        } catch (NumberFormatException e) {
            player.sendMessage("§cUngültige Zahl. Bitte gib einen gültigen Preis ein oder tippe §eAbbrechen§c.");
            return;
        }

        SellSession session = pendingSell.remove(uuid);
        // Open sell GUI on main thread
        plugin.getServer().getScheduler().runTask(plugin, () ->
                marktSellGUI.open(player, session.item, session.amount, price));
        guiStates.put(uuid, new GuiState(0, MarktFilter.ALLE, MarktSort.PREIS_ASC, null, session.item, session.amount, price));
    }

    // -------------------------------------------------------------------------
    // Inventory click events
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(MarktGUI.TITLE)) {
            handleMarktClick(event, player);
        } else if (title.equals(MarktSellGUI.TITLE)) {
            handleSellClick(event, player);
        }
    }

    private void handleMarktClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        UUID uuid = player.getUniqueId();
        GuiState state = guiStates.computeIfAbsent(uuid, k -> new GuiState(0, MarktFilter.ALLE, MarktSort.PREIS_ASC, null, null, 0, 0));

        // Bottom bar controls
        if (slot == 45) {
            // Previous page
            if (state.page > 0) {
                state.page--;
                marktGUI.open(player, state.page, state.filter, state.sort, state.search);
            }
            return;
        }
        if (slot == 53) {
            // Next page
            state.page++;
            marktGUI.open(player, state.page, state.filter, state.sort, state.search);
            return;
        }
        if (slot == 47) {
            // Cycle filter
            state.filter = state.filter.next();
            state.page = 0;
            marktGUI.open(player, state.page, state.filter, state.sort, state.search);
            return;
        }
        if (slot == 51) {
            // Cycle sort
            state.sort = state.sort.next();
            marktGUI.open(player, state.page, state.filter, state.sort, state.search);
            return;
        }
        if (slot == 49 || slot == 46 || slot == 48 || slot == 50 || slot == 52) {
            return; // info/filler
        }

        // Listing slot (0-44)
        if (slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

            // Find the listing by matching the listing ID from lore
            String listingId = extractListingId(clicked);
            if (listingId == null) return;

            if (event.isLeftClick()) {
                // Buy with confirmation
                String prev = pendingBuy.get(uuid);
                long prevTime = pendingBuyTime.getOrDefault(uuid, 0L);
                boolean withinWindow = (System.currentTimeMillis() - prevTime) < 5000;

                if (listingId.equals(prev) && withinWindow) {
                    // Second click — confirm purchase
                    pendingBuy.remove(uuid);
                    pendingBuyTime.remove(uuid);
                    player.closeInventory();
                    plugin.getMarktManager().buyListing(listingId, player);
                    // Reopen market
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            marktGUI.open(player, state.page, state.filter, state.sort, state.search), 2L);
                } else {
                    // First click — ask to confirm
                    pendingBuy.put(uuid, listingId);
                    pendingBuyTime.put(uuid, System.currentTimeMillis());
                    MarktListing listing = plugin.getMarktManager().getListing(listingId);
                    if (listing != null) {
                        player.sendMessage("§6Klicke nochmal innerhalb von 5 Sekunden, um §e"
                                + listing.getAmount() + "x §f" + MarktManager.itemDisplayName(listing.getItem())
                                + " §6für §e" + MarktManager.formatCoins(listing.getPrice()) + " Coins §6zu kaufen.");
                    }
                }
            } else if (event.isRightClick()) {
                // Show details
                MarktListing listing = plugin.getMarktManager().getListing(listingId);
                if (listing != null) {
                    player.sendMessage("§8§m--------------------");
                    player.sendMessage("§6§lAngebot-Details");
                    player.sendMessage("§7ID: §e" + listing.getId());
                    player.sendMessage("§7Item: §f" + MarktManager.itemDisplayName(listing.getItem()));
                    player.sendMessage("§7Menge: §f" + listing.getAmount() + "x");
                    player.sendMessage("§7Preis: §6" + MarktManager.formatCoins(listing.getPrice()) + " Coins");
                    player.sendMessage("§7Verkäufer: §e" + listing.getSellerName());
                    player.sendMessage("§7Eingestellt: §f" + MarktManager.timeAgo(listing.getListedAt()) + " ago");
                    player.sendMessage("§8§m--------------------");
                }
            }
        }
    }

    private void handleSellClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        UUID uuid = player.getUniqueId();
        GuiState state = guiStates.get(uuid);
        if (state == null || state.sellItem == null) {
            player.closeInventory();
            return;
        }

        if (slot == 11) {
            // Confirm sell
            player.closeInventory();
            plugin.getMarktManager().createListing(player, state.sellItem, state.sellAmount, state.sellPrice);
            guiStates.remove(uuid); // clear sell state
        } else if (slot == 15) {
            // Cancel
            player.closeInventory();
            player.sendMessage("§cVerkauf abgebrochen.");
            guiStates.remove(uuid);
        }
    }

    private String extractListingId(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            if (line.startsWith("§8ID: §7")) {
                return line.substring("§8ID: §7".length()).trim();
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // GUI state holder
    // -------------------------------------------------------------------------

    public void openMarkt(Player player, int page, MarktFilter filter, MarktSort sort, String search) {
        UUID uuid = player.getUniqueId();
        GuiState state = new GuiState(page, filter, sort, search, null, 0, 0);
        guiStates.put(uuid, state);
        marktGUI.open(player, page, filter, sort, search);
    }

    /**
     * Opens the sell GUI directly (price already known, no chat input needed).
     */
    public void openSell(Player player, ItemStack item, int amount, double price) {
        UUID uuid = player.getUniqueId();
        GuiState state = new GuiState(0, MarktFilter.ALLE, MarktSort.NEU, null, item.clone(), amount, price);
        guiStates.put(uuid, state);
        marktSellGUI.open(player, item, amount, price);
    }

    private static class GuiState {
        int page;
        MarktFilter filter;
        MarktSort sort;
        String search;
        ItemStack sellItem;
        int sellAmount;
        double sellPrice;

        GuiState(int page, MarktFilter filter, MarktSort sort, String search, ItemStack sellItem, int sellAmount, double sellPrice) {
            this.page = page;
            this.filter = filter;
            this.sort = sort;
            this.search = search;
            this.sellItem = sellItem;
            this.sellAmount = sellAmount;
            this.sellPrice = sellPrice;
        }
    }

    private static class SellSession {
        final ItemStack item;
        final int amount;
        SellSession(ItemStack item, int amount) {
            this.item = item;
            this.amount = amount;
        }
    }
}
