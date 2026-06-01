package at.minich.opserver.jobs;

import org.bukkit.Material;

/**
 * Enum of all available jobs/professions.
 */
public enum Job {

    GRAEBER(
            "§6Gräber",
            "Grabe Erde, Sand, Kies und Ton ab.",
            Material.IRON_SHOVEL
    ),
    MIENENARBEITER(
            "§7Minenarbeiter",
            "Baue Steine, Erze und Obsidian ab.",
            Material.IRON_PICKAXE
    ),
    FARMER(
            "§aFarmer",
            "Ernte Weizen, Karotten, Kartoffeln und mehr.",
            Material.WHEAT
    ),
    FISHER(
            "§bFischer",
            "Angle Fische aus allen Gewässern.",
            Material.FISHING_ROD
    ),
    JAEGER(
            "§cJäger",
            "Töte Monster und Tiere. PvP zählt 10x.",
            Material.BOW
    ),
    BUILDER(
            "§eBaumeister",
            "Baue mit Holz, Stein, Glas und mehr.",
            Material.BRICKS
    );

    private final String displayName;
    private final String description;
    private final Material icon;

    Job(String displayName, String description, Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    /**
     * Case-insensitive lookup by enum name or display name (without color codes).
     */
    public static Job fromString(String name) {
        if (name == null) return null;
        for (Job j : values()) {
            if (j.name().equalsIgnoreCase(name)) return j;
            // Strip color codes from display name for comparison
            String plain = j.displayName.replaceAll("§[0-9a-fklmnor]", "");
            if (plain.equalsIgnoreCase(name)) return j;
        }
        return null;
    }
}
