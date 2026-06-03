package at.minich.opserver.mining;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores each player's current area mining size (1, 3, 5, or 7).
 * Data lives in memory only — it resets when the player logs out.
 */
public class AreaMineManager {

    /** Allowed area sizes. Only odd numbers for symmetric cubes. */
    public static final int[] VALID_SIZES = {1, 3, 5, 7};

    private final Map<UUID, Integer> sizes = new HashMap<>();

    /**
     * Returns the current mining size for the given player (default 1 = normal).
     */
    public int getSize(UUID playerId) {
        return sizes.getOrDefault(playerId, 1);
    }

    /**
     * Sets the mining size for the player.
     *
     * @param playerId target player
     * @param size     must be 1, 3, 5, or 7
     * @throws IllegalArgumentException if the size is not valid
     */
    public void setSize(UUID playerId, int size) {
        if (!isValidSize(size)) {
            throw new IllegalArgumentException("Invalid mining size: " + size + ". Must be 1, 3, 5, or 7.");
        }
        sizes.put(playerId, size);
    }

    /**
     * Removes stored size for a player (called on logout to free memory).
     */
    public void remove(UUID playerId) {
        sizes.remove(playerId);
    }

    /**
     * Returns true if {@code size} is one of the allowed values.
     */
    public static boolean isValidSize(int size) {
        for (int v : VALID_SIZES) {
            if (v == size) return true;
        }
        return false;
    }
}
