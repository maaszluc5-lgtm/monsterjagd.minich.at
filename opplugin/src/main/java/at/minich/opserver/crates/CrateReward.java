package at.minich.opserver.crates;

import org.bukkit.Material;

public class CrateReward {

    public enum RewardType {
        COINS, CRYSTALS, ITEM, CUSTOM_ITEM
    }

    private final RewardType type;
    private final long amount;
    private final Material material;   // for ITEM rewards
    private final String customItemId; // for CUSTOM_ITEM rewards
    private final int weight;
    private final String displayName;
    private final Material displayMaterial;

    /** Constructor for COINS / CRYSTALS rewards. */
    public CrateReward(RewardType type, long amount, int weight, String displayName, Material displayMaterial) {
        this.type = type;
        this.amount = amount;
        this.material = null;
        this.customItemId = null;
        this.weight = weight;
        this.displayName = displayName;
        this.displayMaterial = displayMaterial;
    }

    /** Constructor for ITEM rewards. */
    public CrateReward(Material material, long amount, int weight, String displayName, Material displayMaterial) {
        this.type = RewardType.ITEM;
        this.amount = amount;
        this.material = material;
        this.customItemId = null;
        this.weight = weight;
        this.displayName = displayName;
        this.displayMaterial = displayMaterial;
    }

    /** Constructor for CUSTOM_ITEM rewards. */
    public CrateReward(String customItemId, int weight, String displayName, Material displayMaterial) {
        this.type = RewardType.CUSTOM_ITEM;
        this.amount = 1;
        this.material = null;
        this.customItemId = customItemId;
        this.weight = weight;
        this.displayName = displayName;
        this.displayMaterial = displayMaterial;
    }

    public RewardType getType() { return type; }
    public long getAmount() { return amount; }
    public Material getMaterial() { return material; }
    public String getCustomItemId() { return customItemId; }
    public int getWeight() { return weight; }
    public String getDisplayName() { return displayName; }
    public Material getDisplayMaterial() { return displayMaterial; }
}
