package at.minich.opserver.trophies;

/**
 * All available trophy types that can be awarded to players.
 */
public enum TrophyType {

    JAGER_MEISTER(
            "§6§lJäger-Meister",
            "Jäger-Meister",
            "Erreiche Jäger Level 100"
    ),
    BERGMANN_LEGENDE(
            "§7§lBergmann-Legende",
            "Bergmann-Legende",
            "Erreiche Mienenarbeiter Level 100"
    ),
    FARM_KONIG(
            "§a§lFarm-König",
            "Farm-König",
            "Erreiche Farmer Level 100"
    ),
    FISCHER_PROFI(
            "§b§lFischer-Profi",
            "Fischer-Profi",
            "Erreiche Fischer Level 100"
    ),
    GRABER_CHAMPION(
            "§e§lGräber-Champion",
            "Gräber-Champion",
            "Erreiche Gräber Level 100"
    ),
    BAUMSTER_ELITE(
            "§d§lBaumeister-Elite",
            "Baumeister-Elite",
            "Erreiche Baumeister Level 100"
    ),
    REICHSTER_SPIELER(
            "§6§lDer Reichste",
            "Der Reichste",
            "Besitze 10.000.000 Coins gleichzeitig"
    ),
    GOTT_SPIELER(
            "§4§lGott des Servers",
            "Gott des Servers",
            "Erreiche den GOTT-Rang (200h Spielzeit)"
    ),
    PVP_GOTT(
            "§c§lPvP-Gott",
            "PvP-Gott",
            "Töte 10.000 Spieler"
    ),
    ERSTER_SPIELER(
            "§f§lErster Spieler",
            "Erster Spieler",
            "Als erstes auf dem Server beigetreten"
    ),
    OVERLORD(
            "§4§l✦ OVERLORD ✦",
            "OVERLORD",
            "Sammle alle anderen Pokale"
    );

    private final String displayName;
    private final String description;
    private final String requirement;

    TrophyType(String displayName, String description, String requirement) {
        this.displayName = displayName;
        this.description = description;
        this.requirement = requirement;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getRequirement() {
        return requirement;
    }
}
