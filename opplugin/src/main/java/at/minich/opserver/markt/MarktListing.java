package at.minich.opserver.markt;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MarktListing {

    private final String id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final int amount;
    private final double price;
    private final long listedAt;

    public MarktListing(String id, UUID sellerUuid, String sellerName, ItemStack item, int amount, double price, long listedAt) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.item = item;
        this.amount = amount;
        this.price = price;
        this.listedAt = listedAt;
    }

    public String getId() { return id; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item; }
    public int getAmount() { return amount; }
    public double getPrice() { return price; }
    public long getListedAt() { return listedAt; }
}
