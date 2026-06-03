package at.minich.opserver.ah;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.markt.MarktManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages all auctions. Persists to ah.yml.
 */
public class AuctionManager {

    private static final String FILE = "ah.yml";

    private final OpServerPlugin plugin;
    // Active (non-expired, not yet finalized) auctions
    private final Map<String, AuctionEntry> auctions = new ConcurrentHashMap<>();
    // Pending collections per player: UUID -> list of items they are owed
    private final Map<UUID, List<ItemStack>> pendingCollect = new ConcurrentHashMap<>();
    // Pending coin payouts per player: UUID -> coins owed
    private final Map<UUID, Double> pendingCoins = new ConcurrentHashMap<>();

    public AuctionManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Creates an auction from the item currently in the player's main hand.
     * Charges a listing fee (percent of startPrice). Returns the auction ID or null on failure.
     */
    public String createAuction(Player seller, ItemStack item, double startPrice, int durationHours) {
        UUID uuid = seller.getUniqueId();

        // Validate item
        if (item == null || item.getType().isAir()) {
            seller.sendMessage("§cDu hältst kein gültiges Item in der Hand.");
            return null;
        }

        // Validate duration
        int minHours = plugin.getConfig().getInt("ah.min-duration-hours", 1);
        int maxHours = plugin.getConfig().getInt("ah.max-duration-hours", 72);
        if (durationHours < minHours || durationHours > maxHours) {
            seller.sendMessage("§cDie Dauer muss zwischen §e" + minHours + "h §cund §e" + maxHours + "h §cliegen.");
            return null;
        }

        // Validate start price
        if (startPrice <= 0) {
            seller.sendMessage("§cDer Startpreis muss größer als 0 sein.");
            return null;
        }

        // Auction cap
        int maxAuctions = plugin.getConfig().getInt("ah.max-auctions-per-player", 5);
        long currentCount = auctions.values().stream()
                .filter(a -> a.getSellerUuid().equals(uuid) && !a.isExpired())
                .count();
        if (currentCount >= maxAuctions) {
            seller.sendMessage("§cDu hast bereits §e" + maxAuctions + " §caktive Auktionen (Maximum).");
            return null;
        }

        // Listing fee
        double feePercent = plugin.getConfig().getDouble("ah.listing-fee-percent", 2.0);
        double fee = startPrice * feePercent / 100.0;
        if (fee > 0) {
            boolean paid = plugin.getEconomyManager().withdraw(uuid, fee);
            if (!paid) {
                seller.sendMessage("§cDu hast nicht genug Coins für die Einstellungsgebühr (§6"
                        + MarktManager.formatCoins(fee) + " Coins§c, §7" + feePercent + "% von Startpreis§c).");
                return null;
            }
        }

        // Remove item from hand
        seller.getInventory().setItemInMainHand(null);

        long now = System.currentTimeMillis();
        long endsAt = now + (long) durationHours * 3600_000L;
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        AuctionEntry entry = new AuctionEntry(id, uuid, seller.getName(), item.clone(), startPrice, now, endsAt);
        auctions.put(id, entry);
        save();

        seller.sendMessage("§aAuktion erfolgreich erstellt! §7ID: §e" + id
                + " §7Startpreis: §6" + MarktManager.formatCoins(startPrice) + " Coins"
                + " §7Dauer: §f" + durationHours + "h"
                + (fee > 0 ? " §7(Gebühr: §c" + MarktManager.formatCoins(fee) + " Coins§7)" : ""));
        return id;
    }

    /**
     * Places a bid on an auction. Refunds the previous highest bidder.
     */
    public boolean placeBid(String id, Player bidder, double amount) {
        AuctionEntry auction = auctions.get(id);
        if (auction == null || auction.isExpired()) {
            bidder.sendMessage("§cDiese Auktion existiert nicht oder ist bereits abgelaufen.");
            return false;
        }
        if (auction.getSellerUuid().equals(bidder.getUniqueId())) {
            bidder.sendMessage("§cDu kannst nicht auf deine eigene Auktion bieten.");
            return false;
        }
        if (auction.hasBids() && auction.getHighestBidderUuid().equals(bidder.getUniqueId())) {
            bidder.sendMessage("§cDu bist bereits der Höchstbieter.");
            return false;
        }

        // Minimum increment: max(5% of current bid, configurable min)
        double minIncrement = plugin.getConfig().getDouble("ah.min-bid-increment", 100.0);
        double minBid = Math.max(auction.getCurrentBid() + minIncrement,
                auction.getCurrentBid() * 1.05);
        // If no bids yet, minimum is the start price itself
        if (!auction.hasBids()) {
            minBid = auction.getStartPrice();
        }

        if (amount < minBid) {
            bidder.sendMessage("§cDein Gebot muss mindestens §6" + MarktManager.formatCoins(minBid)
                    + " Coins §csein.");
            return false;
        }

        // Withdraw from bidder
        if (!plugin.getEconomyManager().has(bidder.getUniqueId(), amount)) {
            bidder.sendMessage("§cDu hast nicht genug Coins. (Benötigt: §6"
                    + MarktManager.formatCoins(amount) + " Coins§c)");
            return false;
        }
        plugin.getEconomyManager().withdraw(bidder.getUniqueId(), amount);

        // Refund previous highest bidder
        if (auction.hasBids()) {
            UUID prevBidder = auction.getHighestBidderUuid();
            double refund = auction.getCurrentBid();
            plugin.getEconomyManager().deposit(prevBidder, refund);
            // Notify previous bidder if online
            Player prev = plugin.getServer().getPlayer(prevBidder);
            if (prev != null && prev.isOnline()) {
                prev.sendMessage("§cDein Gebot für §e" + MarktManager.itemDisplayName(auction.getItem())
                        + " §c(ID: §e" + id + "§c) wurde überboten! §6"
                        + MarktManager.formatCoins(refund) + " Coins §czurückgegeben.");
            }
        }

        // Update auction
        auction.setCurrentBid(amount);
        auction.setHighestBidder(bidder.getUniqueId(), bidder.getName());
        save();

        bidder.sendMessage("§aGebot platziert! §e" + MarktManager.formatCoins(amount)
                + " Coins §afür §f" + MarktManager.itemDisplayName(auction.getItem())
                + " §a(ID: §e" + id + "§a).");

        // Notify seller
        Player seller = plugin.getServer().getPlayer(auction.getSellerUuid());
        if (seller != null && seller.isOnline()) {
            seller.sendMessage("§a" + bidder.getName() + " §ahat §6"
                    + MarktManager.formatCoins(amount) + " Coins §afür deine Auktion geboten. §7(ID: §e"
                    + id + "§7)");
        }
        return true;
    }

    /**
     * Cancels an auction. Only allowed if there are no bids yet.
     */
    public boolean cancelAuction(String id, Player requester) {
        AuctionEntry auction = auctions.get(id);
        if (auction == null) {
            requester.sendMessage("§cKeine Auktion mit der ID §e" + id + " §cgefunden.");
            return false;
        }
        if (!auction.getSellerUuid().equals(requester.getUniqueId()) && !requester.isOp()) {
            requester.sendMessage("§cDu kannst nur deine eigenen Auktionen abbrechen.");
            return false;
        }
        if (auction.hasBids()) {
            requester.sendMessage("§cDu kannst diese Auktion nicht abbrechen, da bereits Gebote vorliegen.");
            return false;
        }

        auctions.remove(id);
        giveOrDrop(requester, auction.getItem().clone());
        requester.sendMessage("§aAuktion §e" + id + " §aabgebrochen. Item zurückgegeben.");
        save();
        return true;
    }

    /**
     * Finalizes all expired auctions. Called every 30 seconds by a scheduler.
     */
    public void finalizeAuctions() {
        List<AuctionEntry> expired = auctions.values().stream()
                .filter(AuctionEntry::isExpired)
                .collect(Collectors.toList());

        for (AuctionEntry auction : expired) {
            auctions.remove(auction.getId());

            if (auction.hasBids()) {
                // Winner gets item
                UUID winnerUuid = auction.getHighestBidderUuid();
                Player winner = plugin.getServer().getPlayer(winnerUuid);
                if (winner != null && winner.isOnline()) {
                    giveOrDrop(winner, auction.getItem().clone());
                    winner.sendMessage("§6§lGlückwunsch! §aDu hast die Auktion für §f"
                            + MarktManager.itemDisplayName(auction.getItem())
                            + " §agewonnen! Das Item wurde deinem Inventar hinzugefügt.");
                } else {
                    // Winner offline — queue for collection
                    addPendingItem(winnerUuid, auction.getItem().clone());
                }

                // Seller gets coins
                double payout = auction.getCurrentBid();
                Player seller = plugin.getServer().getPlayer(auction.getSellerUuid());
                if (seller != null && seller.isOnline()) {
                    plugin.getEconomyManager().deposit(auction.getSellerUuid(), payout);
                    seller.sendMessage("§aDeine Auktion für §f"
                            + MarktManager.itemDisplayName(auction.getItem())
                            + " §awurde verkauft! §6"
                            + MarktManager.formatCoins(payout) + " Coins §agutgeschrieben.");
                } else {
                    // Seller offline — queue payout
                    addPendingCoins(auction.getSellerUuid(), payout);
                }
            } else {
                // No bids — return item to seller
                Player seller = plugin.getServer().getPlayer(auction.getSellerUuid());
                if (seller != null && seller.isOnline()) {
                    giveOrDrop(seller, auction.getItem().clone());
                    seller.sendMessage("§7Deine Auktion für §f"
                            + MarktManager.itemDisplayName(auction.getItem())
                            + " §7ist abgelaufen ohne Gebote. Item zurückgegeben.");
                } else {
                    addPendingItem(auction.getSellerUuid(), auction.getItem().clone());
                }
            }
        }

        if (!expired.isEmpty()) {
            save();
        }
    }

    /**
     * Lets a player collect their pending items and coins.
     */
    public void collectPending(Player player) {
        UUID uuid = player.getUniqueId();
        boolean anything = false;

        // Flush pending coins first
        double coins = pendingCoins.getOrDefault(uuid, 0.0);
        if (coins > 0) {
            plugin.getEconomyManager().deposit(uuid, coins);
            pendingCoins.remove(uuid);
            player.sendMessage("§a§6" + MarktManager.formatCoins(coins) + " Coins §agutgeschrieben.");
            anything = true;
        }

        // Flush pending items
        List<ItemStack> items = pendingCollect.remove(uuid);
        if (items != null && !items.isEmpty()) {
            for (ItemStack item : items) {
                giveOrDrop(player, item);
            }
            player.sendMessage("§a" + items.size() + " Item(s) aus dem Auktionshaus abgeholt.");
            anything = true;
        }

        if (!anything) {
            player.sendMessage("§7Du hast nichts abzuholen.");
        } else {
            save();
        }
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public List<AuctionEntry> getActiveAuctions() {
        return auctions.values().stream()
                .filter(a -> !a.isExpired())
                .collect(Collectors.toList());
    }

    public List<AuctionEntry> getAuctionsByPlayer(UUID uuid) {
        return auctions.values().stream()
                .filter(a -> a.getSellerUuid().equals(uuid) && !a.isExpired())
                .collect(Collectors.toList());
    }

    public AuctionEntry getAuction(String id) {
        return auctions.get(id);
    }

    public boolean hasPending(UUID uuid) {
        return pendingCollect.containsKey(uuid) || pendingCoins.containsKey(uuid);
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();

        // Save active auctions
        for (AuctionEntry entry : auctions.values()) {
            String base = "auctions." + entry.getId();
            cfg.set(base + ".seller-uuid", entry.getSellerUuid().toString());
            cfg.set(base + ".seller-name", entry.getSellerName());
            cfg.set(base + ".start-price", entry.getStartPrice());
            cfg.set(base + ".current-bid", entry.getCurrentBid());
            cfg.set(base + ".highest-bidder-uuid",
                    entry.getHighestBidderUuid() != null ? entry.getHighestBidderUuid().toString() : "");
            cfg.set(base + ".highest-bidder-name", entry.getHighestBidderName());
            cfg.set(base + ".ends-at", entry.getEndsAt());
            cfg.set(base + ".listed-at", entry.getListedAt());
            try {
                cfg.set(base + ".item", MarktManager.itemToBase64(entry.getItem()));
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not serialize item for auction " + entry.getId(), e);
            }
        }

        // Save pending items
        for (Map.Entry<UUID, List<ItemStack>> e : pendingCollect.entrySet()) {
            List<String> encoded = new ArrayList<>();
            for (ItemStack item : e.getValue()) {
                try {
                    encoded.add(MarktManager.itemToBase64(item));
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.WARNING, "Could not serialize pending item for " + e.getKey(), ex);
                }
            }
            cfg.set("pending-items." + e.getKey().toString(), encoded);
        }

        // Save pending coins
        for (Map.Entry<UUID, Double> e : pendingCoins.entrySet()) {
            cfg.set("pending-coins." + e.getKey().toString(), e.getValue());
        }

        File file = new File(plugin.getDataFolder(), FILE);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save ah.yml", e);
        }
    }

    private void load() {
        File file = new File(plugin.getDataFolder(), FILE);
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Load auctions
        ConfigurationSection auctionsSection = cfg.getConfigurationSection("auctions");
        if (auctionsSection != null) {
            for (String id : auctionsSection.getKeys(false)) {
                try {
                    String base = "auctions." + id;
                    UUID sellerUuid = UUID.fromString(cfg.getString(base + ".seller-uuid"));
                    String sellerName = cfg.getString(base + ".seller-name", "Unbekannt");
                    double startPrice = cfg.getDouble(base + ".start-price");
                    double currentBid = cfg.getDouble(base + ".current-bid");
                    String bidderUuidStr = cfg.getString(base + ".highest-bidder-uuid", "");
                    UUID highestBidderUuid = bidderUuidStr.isEmpty() ? null : UUID.fromString(bidderUuidStr);
                    String highestBidderName = cfg.getString(base + ".highest-bidder-name", "");
                    long endsAt = cfg.getLong(base + ".ends-at");
                    long listedAt = cfg.getLong(base + ".listed-at");
                    String itemData = cfg.getString(base + ".item");
                    if (itemData == null) continue;
                    ItemStack item = MarktManager.itemFromBase64(itemData);
                    if (item == null) continue;

                    AuctionEntry entry = new AuctionEntry(id, sellerUuid, sellerName, item,
                            startPrice, currentBid, highestBidderUuid, highestBidderName, listedAt, endsAt);
                    auctions.put(id, entry);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Could not load auction " + id, e);
                }
            }
        }

        // Load pending items
        ConfigurationSection pendingItemsSection = cfg.getConfigurationSection("pending-items");
        if (pendingItemsSection != null) {
            for (String uuidStr : pendingItemsSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<String> encoded = cfg.getStringList("pending-items." + uuidStr);
                    List<ItemStack> items = new ArrayList<>();
                    for (String s : encoded) {
                        ItemStack item = MarktManager.itemFromBase64(s);
                        if (item != null) items.add(item);
                    }
                    if (!items.isEmpty()) pendingCollect.put(uuid, items);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Could not load pending items for " + uuidStr, e);
                }
            }
        }

        // Load pending coins
        ConfigurationSection pendingCoinsSection = cfg.getConfigurationSection("pending-coins");
        if (pendingCoinsSection != null) {
            for (String uuidStr : pendingCoinsSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    double coins = cfg.getDouble("pending-coins." + uuidStr);
                    if (coins > 0) pendingCoins.put(uuid, coins);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Could not load pending coins for " + uuidStr, e);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void addPendingItem(UUID uuid, ItemStack item) {
        pendingCollect.computeIfAbsent(uuid, k -> new ArrayList<>()).add(item);
    }

    private void addPendingCoins(UUID uuid, double amount) {
        pendingCoins.merge(uuid, amount, Double::sum);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }
}
