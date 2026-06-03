package at.minich.opserver.ah;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.markt.MarktManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all GUI interactions for the Auktionshaus.
 */
public class AuctionGUIListener implements Listener {

    // Per-player pending bid input session: auctionId
    private final Map<UUID, String> pendingBid = new HashMap<>();

    // Per-player GUI state
    private final Map<UUID, GuiState> guiStates = new HashMap<>();

    private final OpServerPlugin plugin;
    private final AuctionGUI auctionGUI;

    public AuctionGUIListener(OpServerPlugin plugin, AuctionGUI auctionGUI) {
        this.plugin = plugin;
        this.auctionGUI = auctionGUI;
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public void openAuctionHouse(Player player, int page, AuctionFilter filter, AuctionSort sort) {
        UUID uuid = player.getUniqueId();
        guiStates.put(uuid, new GuiState(page, filter, sort));
        auctionGUI.open(player, page, filter, sort);
    }

    // -------------------------------------------------------------------------
    // Inventory click events
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(AuctionGUI.TITLE)) {
            handleAuctionClick(event, player);
        }
    }

    private void handleAuctionClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        UUID uuid = player.getUniqueId();
        GuiState state = guiStates.computeIfAbsent(uuid,
                k -> new GuiState(0, AuctionFilter.ALLE, AuctionSort.ENDET_BALD));

        if (slot == 45) {
            // Previous page
            if (state.page > 0) {
                state.page--;
                auctionGUI.open(player, state.page, state.filter, state.sort);
            }
            return;
        }
        if (slot == 53) {
            // Next page
            state.page++;
            auctionGUI.open(player, state.page, state.filter, state.sort);
            return;
        }
        if (slot == 47) {
            // Cycle filter
            state.filter = state.filter.next();
            state.page = 0;
            auctionGUI.open(player, state.page, state.filter, state.sort);
            return;
        }
        if (slot == 51) {
            // Cycle sort
            state.sort = state.sort.next();
            auctionGUI.open(player, state.page, state.filter, state.sort);
            return;
        }
        if (slot == 49 || slot == 46 || slot == 48 || slot == 50 || slot == 52) {
            return; // info/filler
        }

        // Auction item slot (0-44)
        if (slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

            String auctionId = extractAuctionId(clicked);
            if (auctionId == null) return;

            AuctionEntry auction = plugin.getAuctionManager().getAuction(auctionId);
            if (auction == null || auction.isExpired()) {
                player.sendMessage("§cDiese Auktion ist nicht mehr verfügbar.");
                // Refresh
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        auctionGUI.open(player, state.page, state.filter, state.sort), 2L);
                return;
            }

            if (event.isLeftClick()) {
                // Start bid input via chat
                player.closeInventory();
                pendingBid.put(uuid, auctionId);

                double minBid = auction.hasBids()
                        ? Math.max(auction.getCurrentBid() + plugin.getConfig().getDouble("ah.min-bid-increment", 100.0),
                          auction.getCurrentBid() * 1.05)
                        : auction.getStartPrice();

                player.sendMessage("§6Gib dein Gebot für §f"
                        + MarktManager.itemDisplayName(auction.getItem())
                        + " §6im Chat ein:");
                player.sendMessage("§7Mindestgebot: §e" + MarktManager.formatCoins(minBid) + " Coins");
                player.sendMessage("§7(Tippe §cAbbrechen §7um abzubrechen)");
            } else if (event.isRightClick()) {
                // Show details in chat
                player.sendMessage("§8§m--------------------");
                player.sendMessage("§6§lAuktion-Details");
                player.sendMessage("§7ID: §e" + auction.getId());
                player.sendMessage("§7Item: §f" + MarktManager.itemDisplayName(auction.getItem()));
                player.sendMessage("§7Verkäufer: §e" + auction.getSellerName());
                player.sendMessage("§7Startpreis: §f" + MarktManager.formatCoins(auction.getStartPrice()) + " Coins");
                player.sendMessage("§7Aktuelles Gebot: §6" + MarktManager.formatCoins(auction.getCurrentBid()) + " Coins");
                player.sendMessage("§7Höchstbieter: §e" + (auction.hasBids() ? auction.getHighestBidderName() : "Kein Gebot"));
                player.sendMessage("§7Endet in: §f" + AuctionGUI.formatTimeRemaining(auction.getEndsAt()));
                player.sendMessage("§8§m--------------------");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Chat event for bid input
    // -------------------------------------------------------------------------

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!pendingBid.containsKey(uuid)) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();

        if (msg.equalsIgnoreCase("Abbrechen")) {
            pendingBid.remove(uuid);
            player.sendMessage("§cGebot abgebrochen.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(msg.replace(",", "."));
        } catch (NumberFormatException e) {
            player.sendMessage("§cUngültige Zahl. Bitte gib einen gültigen Betrag ein oder tippe §eAbbrechen§c.");
            return;
        }

        String auctionId = pendingBid.remove(uuid);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getAuctionManager().placeBid(auctionId, player, amount);
            // Reopen GUI
            GuiState state = guiStates.computeIfAbsent(uuid,
                    k -> new GuiState(0, AuctionFilter.ALLE, AuctionSort.ENDET_BALD));
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    auctionGUI.open(player, state.page, state.filter, state.sort), 2L);
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String extractAuctionId(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            if (line.startsWith("§8ID: §7")) {
                return line.substring("§8ID: §7".length()).trim();
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // GUI state
    // -------------------------------------------------------------------------

    private static class GuiState {
        int page;
        AuctionFilter filter;
        AuctionSort sort;

        GuiState(int page, AuctionFilter filter, AuctionSort sort) {
            this.page = page;
            this.filter = filter;
            this.sort = sort;
        }
    }
}
