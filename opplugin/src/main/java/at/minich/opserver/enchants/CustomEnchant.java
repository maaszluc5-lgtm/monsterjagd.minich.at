package at.minich.opserver.enchants;

/**
 * Enum of all custom enchantments with their metadata.
 */
public enum CustomEnchant {

    LIFESTEAL("Lifesteal", 1, 100),
    THUNDER("Thunder", 1, 50),
    BURN("Burn", 1, 100),
    POISON("Poison", 1, 50),
    EXPLOSIVE("Explosive", 1, 20),
    SPEED_BOOST("Speed Boost", 1, 50),
    JUMP_BOOST("Jump Boost", 1, 50),
    AUTOSMELT("Autosmelt", 1, 1),
    VEINMINER("Veinminer", 1, 5),
    TITAN("Titan", 1, 200),
    BERSERKER("Berserker", 1, 100),
    SHOCKWAVE("Shockwave", 1, 50),
    WEALTH("Wealth", 1, 50),
    EXPERIENCE("Experience", 1, 100);

    private final String displayName;
    private final int minLevel;
    private final int maxLevel;

    CustomEnchant(String displayName, int minLevel, int maxLevel) {
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Case-insensitive lookup by enum name (e.g. "lifesteal", "SPEED_BOOST").
     */
    public static CustomEnchant fromString(String name) {
        for (CustomEnchant ce : values()) {
            if (ce.name().equalsIgnoreCase(name.replace(" ", "_"))) {
                return ce;
            }
        }
        return null;
    }
}
