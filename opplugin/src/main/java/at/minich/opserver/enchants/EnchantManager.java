package at.minich.opserver.enchants;

import at.minich.opserver.util.RomanNumerals;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Handles reading and writing custom enchants on ItemStacks via PersistentDataContainer (NBT).
 *
 * <p>Data format stored in PDC: "ENCHANT1:LEVEL1,ENCHANT2:LEVEL2"
 */
public class EnchantManager {

    private static final String PDC_KEY = "custom_enchants";

    private final NamespacedKey key;

    public EnchantManager(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, PDC_KEY);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /**
     * Returns a mutable map of all custom enchants on the given item.
     * Returns an empty map if the item has no custom enchants or is null.
     */
    public Map<CustomEnchant, Integer> getEnchants(ItemStack item) {
        Map<CustomEnchant, Integer> result = new EnumMap<>(CustomEnchant.class);
        if (item == null || !item.hasItemMeta()) return result;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String raw = pdc.get(key, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return result;

        for (String part : raw.split(",")) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            CustomEnchant ce = CustomEnchant.fromString(kv[0]);
            if (ce == null) continue;
            try {
                int level = Integer.parseInt(kv[1]);
                result.put(ce, level);
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    /**
     * Returns the level of the given enchant on the item, or 0 if not present.
     */
    public int getLevel(ItemStack item, CustomEnchant enchant) {
        return getEnchants(item).getOrDefault(enchant, 0);
    }

    /**
     * Returns true if the item has the given enchant at any level.
     */
    public boolean hasEnchant(ItemStack item, CustomEnchant enchant) {
        return getLevel(item, enchant) > 0;
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Sets a custom enchant on an item. Level 0 or below removes the enchant.
     * Also updates the item's lore to reflect all current enchants.
     */
    public ItemStack setEnchant(ItemStack item, CustomEnchant enchant, int level) {
        if (item == null) return null;

        Map<CustomEnchant, Integer> enchants = getEnchants(item);
        if (level <= 0) {
            enchants.remove(enchant);
        } else {
            int clamped = Math.min(level, enchant.getMaxLevel());
            enchants.put(enchant, clamped);
        }
        return applyEnchantsToItem(item, enchants);
    }

    /**
     * Removes a custom enchant from an item.
     */
    public ItemStack removeEnchant(ItemStack item, CustomEnchant enchant) {
        return setEnchant(item, enchant, 0);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Writes the given enchant map to the item's PDC and rebuilds its lore.
     */
    private ItemStack applyEnchantsToItem(ItemStack item, Map<CustomEnchant, Integer> enchants) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Serialize to string
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<CustomEnchant, Integer> entry : enchants.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(entry.getKey().name()).append(":").append(entry.getValue());
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, sb.toString());

        // Rebuild lore: keep non-enchant lines, then append enchant lines
        List<String> oldLore = meta.getLore();
        List<String> newLore = new ArrayList<>();

        if (oldLore != null) {
            for (String line : oldLore) {
                // Skip lines that look like our enchant lore (color code §b followed by enchant display name)
                if (!isEnchantLoreLine(line)) {
                    newLore.add(line);
                }
            }
        }

        // Add enchant lines sorted by enum ordinal for consistent order
        for (CustomEnchant ce : CustomEnchant.values()) {
            if (enchants.containsKey(ce)) {
                int lvl = enchants.get(ce);
                newLore.add("§b" + ce.getDisplayName() + " " + RomanNumerals.display(lvl));
            }
        }

        meta.setLore(newLore);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isEnchantLoreLine(String line) {
        if (!line.startsWith("§b")) return false;
        String stripped = line.substring(2); // remove §b
        for (CustomEnchant ce : CustomEnchant.values()) {
            if (stripped.startsWith(ce.getDisplayName() + " ")) return true;
        }
        return false;
    }
}
