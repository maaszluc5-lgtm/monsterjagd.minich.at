package at.minich.opserver.perks;

import org.bukkit.Material;

public enum Perk {

    KEEP_INVENTORY("Inventar behalten", "Kein Itemverlust beim Tod.", Material.TOTEM_OF_UNDYING, Rarity.LEGENDARY),
    DOUBLE_COINS("Doppelte Münzen", "2x Münzen aus Jobs.", Material.GOLD_INGOT, Rarity.RARE),
    TRIPLE_COINS("Dreifache Münzen", "3x Münzen aus Jobs.", Material.GOLD_BLOCK, Rarity.EPIC),
    FAST_MINER("Schnellbergbau", "Permanent Eile II.", Material.GOLDEN_PICKAXE, Rarity.RARE),
    NIGHT_VISION("Nachtsicht", "Permanente Nachtsicht.", Material.POTION, Rarity.COMMON),
    SPEED_BOOST("Schnelligkeit I", "Permanent Schnelligkeit I.", Material.SUGAR, Rarity.COMMON),
    SPEED_BOOST_2("Schnelligkeit II", "Permanent Schnelligkeit II.", Material.SUGAR, Rarity.RARE),
    JUMP_BOOST("Sprungkraft", "Permanent Sprungkraft I.", Material.RABBIT_FOOT, Rarity.COMMON),
    WATER_BREATHING("Wasseratmung", "Permanent unter Wasser atmen.", Material.PUFFERFISH, Rarity.COMMON),
    FIRE_RESISTANCE("Feuerresistenz", "Permanent Feuerresistenz.", Material.BLAZE_ROD, Rarity.RARE),
    NO_FALL_DAMAGE("Kein Fallschaden", "Nimm keinen Fallschaden.", Material.FEATHER, Rarity.EPIC),
    DOUBLE_XP("Doppelte XP", "2x XP aus Jobs.", Material.EXPERIENCE_BOTTLE, Rarity.RARE),
    TRIPLE_XP("Dreifache XP", "3x XP aus Jobs.", Material.EXPERIENCE_BOTTLE, Rarity.EPIC),
    AUTO_SMELT("Auto-Schmelze", "Erze droppen direkt geschmolzen.", Material.FURNACE, Rarity.EPIC),
    LUCKY_DROPS("Glückliche Drops I", "Fortune I auf allen Blöcken.", Material.EMERALD, Rarity.RARE),
    LUCKY_DROPS_2("Glückliche Drops II", "Fortune II auf allen Blöcken.", Material.DIAMOND, Rarity.EPIC),
    DOUBLE_DROPS("Doppelte Drops", "Chance auf doppelte Item-Drops.", Material.CHEST, Rarity.RARE),
    MAGNET_SMALL("Kleiner Magnet", "Items werden aus 4 Blöcken angezogen.", Material.IRON_INGOT, Rarity.RARE),
    MAGNET_LARGE("Großer Magnet", "Items werden aus 8 Blöcken angezogen.", Material.IRON_BLOCK, Rarity.EPIC),
    FLY("Fliegen", "Du kannst fliegen.", Material.ELYTRA, Rarity.LEGENDARY),
    GLIDE("Gleiten", "Immer langsamer Fall.", Material.PHANTOM_MEMBRANE, Rarity.RARE),
    REGENERATION("Regeneration", "Permanent Regeneration I.", Material.GLISTERING_MELON_SLICE, Rarity.RARE),
    STRENGTH("Stärke", "Permanent Stärke I.", Material.BLAZE_POWDER, Rarity.EPIC),
    RESISTANCE("Resistenz", "Permanent Resistenz I.", Material.SHIELD, Rarity.EPIC),
    INVISIBILITY_ON_SNEAK("Schleich-Unsichtbarkeit", "Unsichtbar wenn du schleichst.", Material.GLASS_BOTTLE, Rarity.LEGENDARY),
    TELEKINESIS("Telekinese", "Abgebaute Blöcke gehen direkt ins Inventar.", Material.ENDER_EYE, Rarity.EPIC),
    EXPLOSION_MINER("Sprengstoff-Bergbau", "TNT lässt alle Blöcke droppen.", Material.TNT, Rarity.EPIC),
    TREE_FELLER("Baumfäller", "Bäume auf einmal fällen.", Material.IRON_AXE, Rarity.EPIC),
    AUTO_REPLANT("Auto-Neupflanzung", "Pflanzen werden automatisch neu gepflanzt.", Material.WHEAT_SEEDS, Rarity.RARE),
    LARGER_INVENTORY("Erweitertes Inventar", "Tipp: Nutze /ec für 9 Extra-Slots via Enderchest.", Material.CHEST, Rarity.LEGENDARY),
    DOUBLE_SALARY("Doppelter Lohn", "2x Lohn-Multiplikator.", Material.EMERALD, Rarity.RARE),
    TRIPLE_SALARY("Dreifacher Lohn", "3x Lohn-Multiplikator.", Material.EMERALD_BLOCK, Rarity.EPIC),
    COIN_MAGNET("Münzen-Magnet", "+5% Münzen pro Kill.", Material.GOLD_NUGGET, Rarity.RARE),
    LUCKY_CRATE("Glückliche Kiste", "Bessere Chancen bei seltenen Kisten-Belohnungen.", Material.CHEST, Rarity.LEGENDARY),
    PVP_STRENGTH("PvP-Stärke", "Stärke I während des Kampfes.", Material.IRON_SWORD, Rarity.RARE),
    PVP_RESISTANCE("PvP-Resistenz", "Resistenz I während des Kampfes.", Material.IRON_CHESTPLATE, Rarity.RARE),
    FAST_SWIMMER("Schnellschwimmer", "Permanent Delfingnade.", Material.TROPICAL_FISH, Rarity.RARE),
    ALWAYS_FULL_HEALTH("Immer voll Leben", "Augenblickliche Heilung bei niedrigem Leben.", Material.GOLDEN_APPLE, Rarity.LEGENDARY),
    NO_HUNGER("Kein Hunger", "Hunger nimmt nicht ab.", Material.COOKED_BEEF, Rarity.EPIC),
    INFINITE_ARROWS("Unendliche Pfeile", "Pfeile werden nicht verbraucht.", Material.ARROW, Rarity.EPIC),
    SHIELD_BASH("Schildstoß", "Rückstoß beim Blocken.", Material.SHIELD, Rarity.RARE),
    DOUBLE_FISHING("Doppeltes Angeln", "2x Belohnungen beim Angeln.", Material.FISHING_ROD, Rarity.RARE),
    NIGHT_MINER("Nacht-Bergbauer", "Eile III bei Nacht.", Material.BLACK_STAINED_GLASS, Rarity.EPIC),
    SOLAR_MINER("Solar-Bergbauer", "Eile III am Tag.", Material.YELLOW_STAINED_GLASS, Rarity.EPIC),
    BONUS_COINS_10("Münzen-Bonus 10%", "+10% Münzen flach.", Material.GOLD_NUGGET, Rarity.COMMON),
    BONUS_COINS_25("Münzen-Bonus 25%", "+25% Münzen flach.", Material.GOLD_INGOT, Rarity.RARE),
    BONUS_COINS_50("Münzen-Bonus 50%", "+50% Münzen flach.", Material.GOLD_BLOCK, Rarity.EPIC),
    SUPER_JUMP("Super-Sprung", "Permanent Sprungkraft III.", Material.SLIME_BALL, Rarity.LEGENDARY),
    GOD_APPLE_EFFECT("Gottes Apfel", "Permanent Absorption IV.", Material.ENCHANTED_GOLDEN_APPLE, Rarity.LEGENDARY);

    public enum Rarity {
        COMMON("§fGewöhnlich"),
        RARE("§9Selten"),
        EPIC("§5Episch"),
        LEGENDARY("§6Legendär");

        private final String display;

        Rarity(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    private final String displayName;
    private final String description;
    private final Material icon;
    private final Rarity rarity;

    Perk(String displayName, String description, Material icon, Rarity rarity) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.rarity = rarity;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Material getIcon() { return icon; }
    public Rarity getRarity() { return rarity; }
}
