package at.minich.opserver.ah;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a single auction entry in the Auktionshaus.
 */
public class AuctionEntry {

    private final String id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final double startPrice;
    private final long listedAt;
    private final long endsAt;

    private double currentBid;
    private UUID highestBidderUuid;   // null if no bids
    private String highestBidderName; // empty string if no bids

    public AuctionEntry(String id, UUID sellerUuid, String sellerName, ItemStack item,
                        double startPrice, long listedAt, long endsAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item;
        this.startPrice = startPrice;
        this.currentBid = startPrice;
        this.listedAt = listedAt;
        this.endsAt = endsAt;
        this.highestBidderUuid = null;
        this.highestBidderName = "";
    }

    // Full constructor for loading from YAML
    public AuctionEntry(String id, UUID sellerUuid, String sellerName, ItemStack item,
                        double startPrice, double currentBid,
                        UUID highestBidderUuid, String highestBidderName,
                        long listedAt, long endsAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item;
        this.startPrice = startPrice;
        this.currentBid = currentBid;
        this.highestBidderUuid = highestBidderUuid;
        this.highestBidderName = highestBidderName;
        this.listedAt = listedAt;
        this.endsAt = endsAt;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getId() { return id; }

    public UUID getSellerUuid() { return sellerUuid; }

    public String getSellerName() { return sellerName; }

    public ItemStack getItem() { return item; }

    public double getStartPrice() { return startPrice; }

    public double getCurrentBid() { return currentBid; }

    public UUID getHighestBidderUuid() { return highestBidderUuid; }

    public String getHighestBidderName() { return highestBidderName; }

    public long getListedAt() { return listedAt; }

    public long getEndsAt() { return endsAt; }

    public boolean hasBids() { return highestBidderUuid != null; }

    public boolean isExpired() { return System.currentTimeMillis() >= endsAt; }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    public void setCurrentBid(double currentBid) { this.currentBid = currentBid; }

    public void setHighestBidder(UUID uuid, String name) {
        this.highestBidderUuid = uuid;
        this.highestBidderName = name;
    }
}
