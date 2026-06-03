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

    // -------------------------------------------------------------------------
    // Swords
    // -------------------------------------------------------------------------
    GOD_SWORD("God Sword", Material.DIAMOND_SWORD),
    SHADOW_BLADE("§8§lSchattenschwert", Material.NETHERITE_SWORD),
    DRAGON_SWORD("§4§lDrachenschwert", Material.NETHERITE_SWORD),
    THUNDER_BLADE("§e§lDonnerklinge", Material.DIAMOND_SWORD),
    VAMPIRE_SWORD("§5§lVampirklinge", Material.DIAMOND_SWORD),
    BERSERKER_AXE("§c§lBerserkeraxt", Material.NETHERITE_AXE),
    POISON_DAGGER("§2§lGiftdolch", Material.DIAMOND_SWORD),
    HOLY_SWORD("§f§l✦ Heiliges Schwert", Material.DIAMOND_SWORD),
    INFERNO_BLADE("§6§lHöllenschwert", Material.NETHERITE_SWORD),

    // -------------------------------------------------------------------------
    // Pickaxes
    // -------------------------------------------------------------------------
    GOD_PICKAXE("God Pickaxe", Material.DIAMOND_PICKAXE),
    MINING_KING("§6§lMining King", Material.NETHERITE_PICKAXE),
    SHADOW_PICKAXE("§8§lSchattenpickel", Material.NETHERITE_PICKAXE),
    SPEED_DRILLER("§b§lSpeed-Bohrer", Material.DIAMOND_PICKAXE),

    // -------------------------------------------------------------------------
    // Axes
    // -------------------------------------------------------------------------
    LUMBER_GOD("§a§lHolzgott-Axt", Material.NETHERITE_AXE),
    BATTLE_AXE("§c§lKampfaxt", Material.NETHERITE_AXE),

    // -------------------------------------------------------------------------
    // Shovels
    // -------------------------------------------------------------------------
    SUPER_SHOVEL("§e§lSuper-Schaufel", Material.NETHERITE_SHOVEL),

    // -------------------------------------------------------------------------
    // God Armor (individual pieces — kept for backward compat)
    // -------------------------------------------------------------------------
    GOD_HELMET("God Helmet", Material.DIAMOND_HELMET),
    GOD_CHESTPLATE("God Chestplate", Material.DIAMOND_CHESTPLATE),
    GOD_LEGGINGS("God Leggings", Material.DIAMOND_LEGGINGS),
    GOD_BOOTS("God Boots", Material.DIAMOND_BOOTS),

    // Shadow Armor
    SHADOW_HELMET("§8Schatten-Helm", Material.NETHERITE_HELMET),
    SHADOW_CHESTPLATE("§8Schatten-Brustplatte", Material.NETHERITE_CHESTPLATE),
    SHADOW_LEGGINGS("§8Schatten-Hosen", Material.NETHERITE_LEGGINGS),
    SHADOW_BOOTS("§8Schatten-Stiefel", Material.NETHERITE_BOOTS),

    // Dragon Armor
    DRAGON_HELMET("§4Drachen-Helm", Material.NETHERITE_HELMET),
    DRAGON_CHESTPLATE("§4Drachen-Brustplatte", Material.NETHERITE_CHESTPLATE),
    DRAGON_LEGGINGS("§4Drachen-Hosen", Material.NETHERITE_LEGGINGS),
    DRAGON_BOOTS("§4Drachen-Stiefel", Material.NETHERITE_BOOTS),

    // Berserker Armor
    BERSERKER_HELMET("§cBerserker-Helm", Material.DIAMOND_HELMET),
    BERSERKER_CHESTPLATE("§cBerserker-Brustplatte", Material.DIAMOND_CHESTPLATE),
    BERSERKER_LEGGINGS("§cBerserker-Hosen", Material.DIAMOND_LEGGINGS),
    BERSERKER_BOOTS("§cBerserker-Stiefel", Material.DIAMOND_BOOTS),

    // Speed Armor
    SPEED_HELMET("§bSpeed-Helm", Material.DIAMOND_HELMET),
    SPEED_CHESTPLATE("§bSpeed-Brustplatte", Material.DIAMOND_CHESTPLATE),
    SPEED_LEGGINGS("§bSpeed-Hosen", Material.DIAMOND_LEGGINGS),
    SPEED_BOOTS("§bSpeed-Stiefel", Material.DIAMOND_BOOTS),

    // -------------------------------------------------------------------------
    // Special items
    // -------------------------------------------------------------------------
    LUCKY_TOTEM("§6§lGlückstotem", Material.TOTEM_OF_UNDYING),
    HEALING_WAND("§a§lHeilstab", Material.BLAZE_ROD),
    XP_BOTTLE_STACK("§5§lXP-Paket", Material.EXPERIENCE_BOTTLE),
    WAENDEZERSTOERER("§4§l⚡ Wändezerstörer", Material.NETHERITE_PICKAXE),

    // -------------------------------------------------------------------------
    // Starter kit items
    // -------------------------------------------------------------------------
    STARTER_SWORD("Starter Sword", Material.STONE_SWORD),
    STARTER_PICKAXE("Starter Pickaxe", Material.IRON_PICKAXE);

    // =========================================================================

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

        // XP_BOTTLE_STACK is 64 bottles
        if (this == XP_BOTTLE_STACK) {
            item = new ItemStack(material, 64);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(displayName);
        meta.setLore(new ArrayList<>());
        item.setItemMeta(meta);

        switch (this) {

            // -----------------------------------------------------------------
            // Swords
            // -----------------------------------------------------------------
            case GOD_SWORD -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 5);
                item = em.setEnchant(item, CustomEnchant.LIFESTEAL, 100);
                item = em.setEnchant(item, CustomEnchant.THUNDER, 50);
                item = em.setEnchant(item, CustomEnchant.BURN, 100);
                item = em.setEnchant(item, CustomEnchant.BERSERKER, 100);
                item = em.setEnchant(item, CustomEnchant.SHOCKWAVE, 50);
            }
            case SHADOW_BLADE -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 150);
                item = em.setEnchant(item, CustomEnchant.LIFESTEAL, 75);
                item = em.setEnchant(item, CustomEnchant.POISON, 30);
                setLore(item, "§7Aus der Dunkelheit geschmiedet");
            }
            case DRAGON_SWORD -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 200);
                applyVanillaEnchant(item, Enchantment.FIRE_ASPECT, 2);
                item = em.setEnchant(item, CustomEnchant.THUNDER, 40);
                item = em.setEnchant(item, CustomEnchant.BURN, 80);
                setLore(item, "§7Mit Drachenblut getränkt");
            }
            case THUNDER_BLADE -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 100);
                item = em.setEnchant(item, CustomEnchant.THUNDER, 50);
                item = em.setEnchant(item, CustomEnchant.SHOCKWAVE, 30);
                setLore(item, "§7Der Blitz folgt jedem Schlag");
            }
            case VAMPIRE_SWORD -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 80);
                item = em.setEnchant(item, CustomEnchant.LIFESTEAL, 100);
                setLore(item, "§7Stehlt die Lebenskraft deiner Feinde");
            }
            case BERSERKER_AXE -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 120);
                item = em.setEnchant(item, CustomEnchant.BERSERKER, 100);
                item = em.setEnchant(item, CustomEnchant.SHOCKWAVE, 20);
                setLore(item, "§7Je weniger HP, desto mächtiger");
            }
            case POISON_DAGGER -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 60);
                item = em.setEnchant(item, CustomEnchant.POISON, 50);
                item = em.setEnchant(item, CustomEnchant.SPEED_BOOST, 20);
                setLore(item, "§7Ein Tropfen genügt");
            }
            case HOLY_SWORD -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 100);
                applyVanillaEnchant(item, Enchantment.KNOCKBACK, 5);
                item = em.setEnchant(item, CustomEnchant.LIFESTEAL, 50);
                setLore(item, "§7Vom Himmel gesegnet");
            }
            case INFERNO_BLADE -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 180);
                applyVanillaEnchant(item, Enchantment.FIRE_ASPECT, 2);
                item = em.setEnchant(item, CustomEnchant.BURN, 100);
                setLore(item, "§7Direkt aus dem Nether");
            }

            // -----------------------------------------------------------------
            // Pickaxes
            // -----------------------------------------------------------------
            case GOD_PICKAXE -> {
                applyVanillaEnchant(item, Enchantment.EFFICIENCY, 5);
                applyVanillaEnchant(item, Enchantment.FORTUNE, 3);
                item = em.setEnchant(item, CustomEnchant.VEINMINER, 5);
                item = em.setEnchant(item, CustomEnchant.AUTOSMELT, 1);
                item = em.setEnchant(item, CustomEnchant.WEALTH, 50);
                item = em.setEnchant(item, CustomEnchant.EXPERIENCE, 100);
            }
            case MINING_KING -> {
                applyVanillaEnchant(item, Enchantment.EFFICIENCY, 200);
                applyVanillaEnchant(item, Enchantment.FORTUNE, 10);
                item = em.setEnchant(item, CustomEnchant.VEINMINER, 5);
                item = em.setEnchant(item, CustomEnchant.WEALTH, 50);
                setLore(item, "§7Der König der Minen");
            }
            case SHADOW_PICKAXE -> {
                applyVanillaEnchant(item, Enchantment.EFFICIENCY, 150);
                item = em.setEnchant(item, CustomEnchant.AUTOSMELT, 1);
                item = em.setEnchant(item, CustomEnchant.VEINMINER, 3);
                setLore(item, "§7Schmilzt alles was es berührt");
            }
            case SPEED_DRILLER -> {
                applyVanillaEnchant(item, Enchantment.EFFICIENCY, 100);
                item = em.setEnchant(item, CustomEnchant.SPEED_BOOST, 30);
                setLore(item, "§7Schneller als der Schall");
            }

            // -----------------------------------------------------------------
            // Axes
            // -----------------------------------------------------------------
            case LUMBER_GOD -> {
                applyVanillaEnchant(item, Enchantment.EFFICIENCY, 200);
                applyVanillaEnchant(item, Enchantment.FORTUNE, 10);
                setLore(item, "§7Fällt ganze Wälder mit einem Schlag");
            }
            case BATTLE_AXE -> {
                applyVanillaEnchant(item, Enchantment.SHARPNESS, 150);
                item = em.setEnchant(item, CustomEnchant.BERSERKER, 50);
                item = em.setEnchant(item, CustomEnchant.SHOCKWAVE, 40);
                setLore(item, "§7Für den echten Krieger");
            }

            // -----------------------------------------------------------------
            // Shovels
            // -----------------------------------------------------------------
            case SUPER_SHOVEL -> {
                applyVanillaEnchant(item, Enchantment.EFFICIENCY, 200);
                applyVanillaEnchant(item, Enchantment.FORTUNE, 10);
                setLore(item, "§7Gräbt schneller als ein Bagger");
            }

            // -----------------------------------------------------------------
            // God Armor
            // -----------------------------------------------------------------
            case GOD_HELMET, GOD_CHESTPLATE, GOD_LEGGINGS, GOD_BOOTS -> {
                applyVanillaEnchant(item, Enchantment.PROTECTION, 4);
                applyVanillaEnchant(item, Enchantment.UNBREAKING, 3);
                item = em.setEnchant(item, CustomEnchant.TITAN, 200);
            }

            // -----------------------------------------------------------------
            // Shadow Armor
            // -----------------------------------------------------------------
            case SHADOW_HELMET, SHADOW_CHESTPLATE, SHADOW_LEGGINGS, SHADOW_BOOTS -> {
                applyVanillaEnchant(item, Enchantment.PROTECTION, 150);
                item = em.setEnchant(item, CustomEnchant.SPEED_BOOST, 20);
                item = em.setEnchant(item, CustomEnchant.JUMP_BOOST, 15);
            }

            // -----------------------------------------------------------------
            // Dragon Armor
            // -----------------------------------------------------------------
            case DRAGON_HELMET, DRAGON_CHESTPLATE, DRAGON_LEGGINGS, DRAGON_BOOTS -> {
                applyVanillaEnchant(item, Enchantment.PROTECTION, 200);
                applyVanillaEnchant(item, Enchantment.FIRE_PROTECTION, 10);
                item = em.setEnchant(item, CustomEnchant.TITAN, 100);
            }

            // -----------------------------------------------------------------
            // Berserker Armor
            // -----------------------------------------------------------------
            case BERSERKER_HELMET, BERSERKER_CHESTPLATE, BERSERKER_LEGGINGS, BERSERKER_BOOTS -> {
                applyVanillaEnchant(item, Enchantment.PROTECTION, 100);
                item = em.setEnchant(item, CustomEnchant.BERSERKER, 50);
            }

            // -----------------------------------------------------------------
            // Speed Armor
            // -----------------------------------------------------------------
            case SPEED_HELMET, SPEED_CHESTPLATE, SPEED_LEGGINGS, SPEED_BOOTS -> {
                applyVanillaEnchant(item, Enchantment.PROTECTION, 80);
                item = em.setEnchant(item, CustomEnchant.SPEED_BOOST, 50);
                item = em.setEnchant(item, CustomEnchant.JUMP_BOOST, 50);
            }

            // -----------------------------------------------------------------
            // Special items (decoration only)
            // -----------------------------------------------------------------
            case LUCKY_TOTEM -> setLore(item, "§7Schützt dich vor dem Tod");
            case HEALING_WAND -> setLore(item, "§7/heal um dich zu heilen");
            case XP_BOTTLE_STACK -> setLore(item, "§7Massenhaft Erfahrung");
            case WAENDEZERSTOERER -> {
                applyVanillaEnchant(item, Enchantment.EFFICIENCY, 10);
                applyVanillaEnchant(item, Enchantment.FORTUNE, 3);
                applyVanillaEnchant(item, Enchantment.UNBREAKING, 10);
                applyVanillaEnchant(item, Enchantment.MENDING, 1);
                item = em.setEnchant(item, CustomEnchant.VEINMINER, 5);
                setLore(item,
                    "§7Baut eine §c5x5 §7Fläche ab (§6/mining 5§7)",
                    "§7Effizienz X | Glück III | Haltbarkeit X | Reparatur",
                    "§7Veinminer V",
                    "",
                    "§cHauptgewinn der §6Legendären Kiste§c!"
                );
            }

            // -----------------------------------------------------------------
            // Starter kit
            // -----------------------------------------------------------------
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

    // =========================================================================
    // Armor set helpers
    // =========================================================================

    /** Returns the full God Armor set as an array: helmet, chestplate, leggings, boots. */
    public static ItemStack[] buildGodArmorSet(EnchantManager em) {
        return new ItemStack[]{
            GOD_HELMET.build(em),
            GOD_CHESTPLATE.build(em),
            GOD_LEGGINGS.build(em),
            GOD_BOOTS.build(em)
        };
    }

    /** Returns the Shadow Armor set. */
    public static List<ItemStack> buildShadowArmorSet(EnchantManager em) {
        return List.of(
            SHADOW_HELMET.build(em),
            SHADOW_CHESTPLATE.build(em),
            SHADOW_LEGGINGS.build(em),
            SHADOW_BOOTS.build(em)
        );
    }

    /** Returns the Dragon Armor set. */
    public static List<ItemStack> buildDragonArmorSet(EnchantManager em) {
        return List.of(
            DRAGON_HELMET.build(em),
            DRAGON_CHESTPLATE.build(em),
            DRAGON_LEGGINGS.build(em),
            DRAGON_BOOTS.build(em)
        );
    }

    /** Returns the Berserker Armor set. */
    public static List<ItemStack> buildBerserkerArmorSet(EnchantManager em) {
        return List.of(
            BERSERKER_HELMET.build(em),
            BERSERKER_CHESTPLATE.build(em),
            BERSERKER_LEGGINGS.build(em),
            BERSERKER_BOOTS.build(em)
        );
    }

    /** Returns the Speed Armor set. */
    public static List<ItemStack> buildSpeedArmorSet(EnchantManager em) {
        return List.of(
            SPEED_HELMET.build(em),
            SPEED_CHESTPLATE.build(em),
            SPEED_LEGGINGS.build(em),
            SPEED_BOOTS.build(em)
        );
    }

    /** Returns the starter kit items. */
    public static ItemStack[] buildStarterKit(EnchantManager em) {
        return new ItemStack[]{
            STARTER_SWORD.build(em),
            STARTER_PICKAXE.build(em),
            new ItemStack(Material.BREAD, 16)
        };
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private void applyVanillaEnchant(ItemStack item, Enchantment enchant, int level) {
        item.addUnsafeEnchantment(enchant, level);
    }

    /**
     * Prepends a lore line BEFORE any enchant lines that EnchantManager may later append.
     * Since this is called before em.setEnchant, the lore list is either empty or contains
     * only the lines we set directly.
     */
    private void setLore(ItemStack item, String... lines) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        // Prepend descriptor lines at the top
        lore.addAll(0, Arrays.asList(lines));
        meta.setLore(lore);
        item.setItemMeta(meta);
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
