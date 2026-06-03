package at.minich.opserver.chestshop;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Data class representing a single player-created chest shop (OPSucht style).
 */
public class ChestShop {

    private final UUID id;
    private final UUID ownerUuid;
    private final String ownerName;
    private final Location chestLoc;
    private final Location signLoc;
    private final ItemStack item;
    private final int amount;
    private final double buyPrice;
    private final double sellPrice;  // 0 = buy-only
    private UUID hologramEntityId;   // mutable – set after spawning

    public ChestShop(UUID id, UUID ownerUuid, String ownerName,
                     Location chestLoc, Location signLoc,
                     ItemStack item, int amount,
                     double buyPrice, double sellPrice) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.chestLoc = chestLoc;
        this.signLoc = signLoc;
        this.item = item.clone();
        this.amount = amount;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public Location getChestLoc() { return chestLoc; }
    public Location getSignLoc() { return signLoc; }
    public ItemStack getItem() { return item.clone(); }
    public int getAmount() { return amount; }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public boolean hasSellPrice() { return sellPrice > 0; }

    public UUID getHologramEntityId() { return hologramEntityId; }
    public void setHologramEntityId(UUID hologramEntityId) {
        this.hologramEntityId = hologramEntityId;
    }
}
