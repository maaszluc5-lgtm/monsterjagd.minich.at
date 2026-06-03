package at.minich.opserver.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Custom items that grant abilities (fly, heal, speed, god) to players.
 * Only usable by players with rank Elite, Legende, or GOTT.
 * Items expire after 3 days and are automatically removed.
 */
public enum AbilityItem {

    FLY_CRYSTAL(
        "§b§l✦ Flug-Kristall",
        Material.PRISMARINE_CRYSTALS,
        "fly",
        Arrays.asList(
            "§7Rechtsklick: §bFliegen ein-/ausschalten",
            "§7Nur für Rang §6Elite §7oder höher.",
            "§7Läuft nach §f3 Tagen §7ab.",
            "",
            "§8[Ability Item]"
        )
    ),

    HEAL_CRYSTAL(
        "§a§l✦ Heilungs-Kristall",
        Material.EMERALD,
        "heal",
        Arrays.asList(
            "§7Rechtsklick: §aLeben sofort auffüllen",
            "§7Cooldown: §f30 Sekunden",
            "§7Nur für Rang §6Elite §7oder höher.",
            "§7Läuft nach §f3 Tagen §7ab.",
            "",
            "§8[Ability Item]"
        )
    ),

    SPEED_CRYSTAL(
        "§e§l✦ Geschwindigkeits-Kristall",
        Material.QUARTZ,
        "speed",
        Arrays.asList(
            "§7Rechtsklick: §eSchneller laufen (Stufe 2)",
            "§7Dauer: §f60 Sekunden",
            "§7Cooldown: §f45 Sekunden",
            "§7Nur für Rang §6Elite §7oder höher.",
            "§7Läuft nach §f3 Tagen §7ab.",
            "",
            "§8[Ability Item]"
        )
    ),

    GOD_CRYSTAL(
        "§4§l✦ Gottes-Kristall",
        Material.NETHER_STAR,
        "god",
        Arrays.asList(
            "§7Rechtsklick: §4Unverwundbarkeit (30s)",
            "§7Cooldown: §f120 Sekunden",
            "§7Nur für Rang §cLegende §7oder höher.",
            "§7Läuft nach §f3 Tagen §7ab.",
            "",
            "§8[Ability Item]"
        )
    );

    public static final long DURATION_MS = TimeUnit.DAYS.toMillis(3);

    public final String displayName;
    public final Material material;
    public final String abilityId;
    public final List<String> lore;

    AbilityItem(String displayName, Material material, String abilityId, List<String> lore) {
        this.displayName = displayName;
        this.material = material;
        this.abilityId = abilityId;
        this.lore = lore;
    }

    /**
     * Builds the item with an expiry timestamp stored in PDC.
     * The lore shows the exact expiry date.
     */
    public ItemStack build(NamespacedKey abilityKey, NamespacedKey expiryKey) {
        long expiresAt = System.currentTimeMillis() + DURATION_MS;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);

        List<String> builtLore = new ArrayList<>(lore);
        // Insert expiry date line before last two lines (empty + [Ability Item])
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter
                .ofPattern("dd.MM.yyyy HH:mm")
                .withZone(java.time.ZoneId.of("Europe/Vienna"));
        String expiryDate = fmt.format(java.time.Instant.ofEpochMilli(expiresAt));
        // Replace "Läuft nach §f3 Tagen §7ab." line with actual date
        builtLore.replaceAll(l -> l.contains("Läuft nach") ? "§7Läuft ab: §f" + expiryDate : l);

        meta.setLore(builtLore);
        meta.getPersistentDataContainer().set(abilityKey, PersistentDataType.STRING, abilityId);
        meta.getPersistentDataContainer().set(expiryKey, PersistentDataType.LONG, expiresAt);
        item.setItemMeta(meta);
        return item;
    }

    public static AbilityItem fromId(String id) {
        for (AbilityItem ai : values()) {
            if (ai.abilityId.equals(id)) return ai;
        }
        return null;
    }

    /** Minimum rank index required (0=Neuling, 3=Elite, 4=Legende, 5=GOTT) */
    public int requiredRankIndex() {
        return switch (this) {
            case FLY_CRYSTAL, HEAL_CRYSTAL, SPEED_CRYSTAL -> 3; // Elite+
            case GOD_CRYSTAL -> 4; // Legende+
        };
    }
}
