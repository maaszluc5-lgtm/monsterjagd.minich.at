package at.minich.opserver.lootbox;

import org.bukkit.Material;

public record LootboxReward(RewardType type, double amount, Material material) {
    public enum RewardType { COINS, ITEM }
}
