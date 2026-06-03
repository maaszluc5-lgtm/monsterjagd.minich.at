package at.minich.opserver.crates;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum CrateType {

    COMMON("§7Gewöhnliche Kiste", Material.CHEST, ChatColor.GRAY, "common"),
    RARE("§9Seltene Kiste", Material.ENDER_CHEST, ChatColor.BLUE, "rare"),
    EPIC("§5Epische Kiste", Material.PURPLE_SHULKER_BOX, ChatColor.DARK_PURPLE, "epic"),
    LEGENDARY("§6Legendäre Kiste", Material.NETHER_STAR, ChatColor.GOLD, "legendary");

    private final String displayName;
    private final Material material;
    private final ChatColor color;
    private final String pdcKey;

    CrateType(String displayName, Material material, ChatColor color, String pdcKey) {
        this.displayName = displayName;
        this.material = material;
        this.color = color;
        this.pdcKey = pdcKey;
    }

    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public ChatColor getColor() { return color; }
    public String getPdcKey() { return pdcKey; }

    public static CrateType fromPdcKey(String key) {
        for (CrateType type : values()) {
            if (type.pdcKey.equalsIgnoreCase(key)) return type;
        }
        return null;
    }
}
