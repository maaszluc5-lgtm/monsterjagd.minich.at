package at.minich.opserver.lootbox;

public enum LootboxType {
    COMMON(500, "§7Gewöhnliche Kiste"),
    RARE(2000, "§9Seltene Kiste"),
    LEGENDARY(10000, "§6§lLegendäre Kiste");

    public final double cost;
    public final String displayName;

    LootboxType(double cost, String displayName) {
        this.cost = cost;
        this.displayName = displayName;
    }
}
