package at.minich.opserver.crates;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class CrateManager {

    private final OpServerPlugin plugin;
    private final NamespacedKey crateTypeKey;
    private final Map<CrateType, List<CrateReward>> rewards = new EnumMap<>(CrateType.class);

    public CrateManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.crateTypeKey = new NamespacedKey(plugin, "crate_type");
        loadRewards();
    }

    private void loadRewards() {
        ConfigurationSection cratesSection = plugin.getConfig().getConfigurationSection("crates");
        if (cratesSection == null) return;

        for (CrateType type : CrateType.values()) {
            List<CrateReward> list = new ArrayList<>();
            ConfigurationSection typeSection = cratesSection.getConfigurationSection(type.getPdcKey());
            if (typeSection == null) continue;

            List<?> rewardList = typeSection.getList("rewards");
            if (rewardList == null) continue;

            for (Object obj : rewardList) {
                if (!(obj instanceof Map<?, ?> map)) continue;
                try {
                    String rewardType = (String) map.get("type");
                    int weight = toInt(map.getOrDefault("weight", 10));
                    String display = String.valueOf(map.getOrDefault("display", "Reward"));
                    Material displayMat = parseMaterial(String.valueOf(map.getOrDefault("display-material", "PAPER")));

                    CrateReward reward = switch (rewardType.toUpperCase()) {
                        case "COINS" -> {
                            long amount = toLong(map.get("amount"));
                            yield new CrateReward(CrateReward.RewardType.COINS, amount, weight, display, displayMat);
                        }
                        case "CRYSTALS" -> {
                            long amount = toLong(map.get("amount"));
                            yield new CrateReward(CrateReward.RewardType.CRYSTALS, amount, weight, display, displayMat);
                        }
                        case "ITEM" -> {
                            Material mat = parseMaterial((String) map.get("material"));
                            long amount = toLong(map.getOrDefault("amount", 1));
                            yield new CrateReward(mat, amount, weight, display, displayMat);
                        }
                        case "CUSTOM_ITEM" -> {
                            // support both "id" and "custom-item-id" keys
                            String id = map.containsKey("custom-item-id")
                                    ? (String) map.get("custom-item-id")
                                    : (String) map.get("id");
                            yield new CrateReward(id, weight, display, displayMat);
                        }
                        default -> null;
                    };

                    if (reward != null) list.add(reward);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse crate reward: " + e.getMessage());
                }
            }
            rewards.put(type, list);
        }
    }

    private Material parseMaterial(String name) {
        if (name == null) return Material.PAPER;
        Material m = Material.matchMaterial(name);
        return m != null ? m : Material.PAPER;
    }

    private long toLong(Object obj) {
        if (obj instanceof Number n) return n.longValue();
        return 0L;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        return 0;
    }

    /** Build a crate ItemStack for the given type with PDC tag. */
    public ItemStack buildCrateItem(CrateType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.getDisplayName());
            meta.setLore(List.of("§7Rechtsklick zum Öffnen"));
            meta.getPersistentDataContainer().set(crateTypeKey, PersistentDataType.STRING, type.getPdcKey());
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Pick a weighted-random reward for the given crate type. */
    public CrateReward pickReward(CrateType type) {
        List<CrateReward> list = rewards.getOrDefault(type, Collections.emptyList());
        if (list.isEmpty()) return null;

        int totalWeight = list.stream().mapToInt(CrateReward::getWeight).sum();
        int roll = new Random().nextInt(totalWeight);
        int cumulative = 0;
        for (CrateReward r : list) {
            cumulative += r.getWeight();
            if (roll < cumulative) return r;
        }
        return list.get(list.size() - 1);
    }

    /** Get all rewards for a crate type (used by the GUI for spin animation). */
    public List<CrateReward> getRewards(CrateType type) {
        return Collections.unmodifiableList(rewards.getOrDefault(type, Collections.emptyList()));
    }

    /** Returns the CrateType stored in the item's PDC, or null. */
    public CrateType getCrateType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String key = meta.getPersistentDataContainer().get(crateTypeKey, PersistentDataType.STRING);
        if (key == null) return null;
        return CrateType.fromPdcKey(key);
    }
}
