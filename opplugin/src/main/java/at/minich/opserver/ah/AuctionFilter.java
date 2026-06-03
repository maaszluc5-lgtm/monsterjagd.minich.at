package at.minich.opserver.ah;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Filter categories for the Auktionshaus GUI.
 */
public enum AuctionFilter {
    ALLE("Alle"),
    WAFFEN("Waffen"),
    WERKZEUGE("Werkzeuge"),
    RUESTUNG("Rüstung"),
    BLOECKE("Blöcke"),
    SONSTIGES("Sonstiges");

    private final String displayName;

    AuctionFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public AuctionFilter next() {
        AuctionFilter[] vals = values();
        return vals[(ordinal() + 1) % vals.length];
    }

    public boolean matches(ItemStack item) {
        if (this == ALLE) return true;
        Material type = item.getType();
        String name = type.name();
        switch (this) {
            case WAFFEN:
                return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_BOW")
                        || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("TRIDENT");
            case WERKZEUGE:
                return name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                        || name.equals("FISHING_ROD") || name.equals("SHEARS") || name.equals("FLINT_AND_STEEL");
            case RUESTUNG:
                return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                        || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                        || name.equals("SHIELD") || name.equals("ELYTRA");
            case BLOECKE:
                return type.isBlock();
            case SONSTIGES:
                return !isWeapon(name) && !isTool(name) && !isArmor(name) && !type.isBlock();
            default:
                return true;
        }
    }

    private static boolean isWeapon(String name) {
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_BOW")
                || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("TRIDENT");
    }

    private static boolean isTool(String name) {
        return name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.equals("FISHING_ROD") || name.equals("SHEARS") || name.equals("FLINT_AND_STEEL");
    }

    private static boolean isArmor(String name) {
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.equals("SHIELD") || name.equals("ELYTRA");
    }
}
