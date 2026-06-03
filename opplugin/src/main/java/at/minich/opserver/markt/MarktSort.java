package at.minich.opserver.markt;

import java.util.Comparator;

public enum MarktSort {
    PREIS_ASC("Preis ↑"),
    PREIS_DESC("Preis ↓"),
    NEU("Neu"),
    ALT("Alt");

    private final String displayName;

    MarktSort(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public MarktSort next() {
        MarktSort[] vals = values();
        return vals[(ordinal() + 1) % vals.length];
    }

    public Comparator<MarktListing> comparator() {
        switch (this) {
            case PREIS_ASC:  return Comparator.comparingDouble(MarktListing::getPrice);
            case PREIS_DESC: return Comparator.comparingDouble(MarktListing::getPrice).reversed();
            case NEU:        return Comparator.comparingLong(MarktListing::getListedAt).reversed();
            case ALT:        return Comparator.comparingLong(MarktListing::getListedAt);
            default:         return Comparator.comparingDouble(MarktListing::getPrice);
        }
    }
}
