package at.minich.opserver.ah;

import java.util.Comparator;

/**
 * Sort modes for the Auktionshaus GUI.
 */
public enum AuctionSort {
    ENDET_BALD("Endet bald"),
    ENDET_SPAETER("Endet später"),
    PREIS_ASC("Preis ↑"),
    PREIS_DESC("Preis ↓");

    private final String displayName;

    AuctionSort(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public AuctionSort next() {
        AuctionSort[] vals = values();
        return vals[(ordinal() + 1) % vals.length];
    }

    public Comparator<AuctionEntry> comparator() {
        switch (this) {
            case ENDET_BALD:    return Comparator.comparingLong(AuctionEntry::getEndsAt);
            case ENDET_SPAETER: return Comparator.comparingLong(AuctionEntry::getEndsAt).reversed();
            case PREIS_ASC:     return Comparator.comparingDouble(AuctionEntry::getCurrentBid);
            case PREIS_DESC:    return Comparator.comparingDouble(AuctionEntry::getCurrentBid).reversed();
            default:            return Comparator.comparingLong(AuctionEntry::getEndsAt);
        }
    }
}
