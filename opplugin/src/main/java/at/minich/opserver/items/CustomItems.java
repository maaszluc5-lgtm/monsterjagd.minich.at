package at.minich.opserver.items;

import at.minich.opserver.enchants.CustomEnchant;
import at.minich.opserver.enchants.EnchantManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Registry of custom OP items.
 */
public enum CustomItems {

    GOD_SWORD("God Sword", Material.DIAMOND_SWORD),
    GOD_PICKAXE("God Pickaxe", Material.DIAMOND_PICKAXE),
    GOD_HELMET("God Helmet", Material.DIAMOND_HELMET),
    GOD_CHESTPLATE("God Chestplate", Material.DIAMOND_CHESTPLATE),
    GOD_LEGGINGS("God Leggings", Material.DIAMOND_LEGGINGS),
    GOD_BOOTS("God Boots", Material.DIAMOND_BOOTS),
    STARTER_SWORD("Starter Sword", Material.STONE_SWORD),
    STARTER_PICKAXE("Starter Pickaxe", Material.IRON_PICKAXE);

    private final String displayName;
    private final Material material;

    CustomItems(String displayName, Material material) {
        this.displayName = displayName;
        this.material = material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    /**
     * Builds the ItemStack for this custom item, applying all enchants.
     */
    public ItemStack build(EnchantManager em) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName("§6" + displayName);
        meta.setLore(new ArrayList<>());
        item.setItemMeta(meta);

        switch (this) {
            case GOD_SWORD -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 5);
                item = em.setEnchant(item, CustomEnchant.LIFESTEAL, 100);
                item = em.setEnchant(item, CustomEnchant.THUNDER, 50);
                item = em.setEnchant(item, CustomEnchant.BURN, 100);
                item = em.setEnchant(item, CustomEnchant.BERSERKER, 100);
                item = em.setEnchant(item, CustomEnchant.SHOCKWAVE, 50);
            }
            case GOD_PICKAXE -> {
                applyVanillaEnchant(item, Enchantment.EFFICIENCY, 5);
                applyVanillaEnchant(item, Enchantment.FORTUNE, 3);
                item = em.setEnchant(item, CustomEnchant.VEINMINER, 5);
                item = em.setEnchant(item, CustomEnchant.AUTOSMELT, 1);
                item = em.setEnchant(item, CustomEnchant.WEALTH, 50);
                item = em.setEnchant(item, CustomEnchant.EXPERIENCE, 100);
            }
            case GOD_HELMET, GOD_CHESTPLATE, GOD_LEGGINGS, GOD_BOOTS -> {
                applyVanillaEnchant(item, Enchantment.PROTECTION, 4);
                applyVanillaEnchant(item, Enchantment.UNBREAKING, 3);
                item = em.setEnchant(item, CustomEnchant.TITAN, 200);
            }
            case STARTER_SWORD -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 1);
                item = em.setEnchant(item, CustomEnchant.LIFESTEAL, 5);
            }
            case STARTER_PICKAXE -> {
                applyVanillaEnchant(item, Enchantment.EFFICIENCY, 2);
                item = em.setEnchant(item, CustomEnchant.WEALTH, 1);
            }
        }

        return item;
    }

    private void applyVanillaEnchant(ItemStack item, Enchantment enchant, int level) {
        item.addUnsafeEnchantment(enchant, level);
    }

    /** Returns the full God Armor set as an array: helmet, chestplate, leggings, boots. */
    public static ItemStack[] buildGodArmorSet(EnchantManager em) {
        return new ItemStack[]{
            GOD_HELMET.build(em),
            GOD_CHESTPLATE.build(em),
            GOD_LEGGINGS.build(em),
            GOD_BOOTS.build(em)
        };
    }

    /** Returns the starter kit items. */
    public static ItemStack[] buildStarterKit(EnchantManager em) {
        return new ItemStack[]{
            STARTER_SWORD.build(em),
            STARTER_PICKAXE.build(em),
            new ItemStack(Material.BREAD, 16)
        };
    }

    /** Case-insensitive lookup by enum name. */
    public static CustomItems fromString(String name) {
        for (CustomItems ci : values()) {
            if (ci.name().equalsIgnoreCase(name.replace(" ", "_"))) {
                return ci;
            }
        }
        return null;
    }
}
