package at.minich.opserver.shop;

import org.bukkit.Material;
import java.util.*;

/**
 * All server shop items with buy prices. Sell = 50% of buy.
 */
public class ServerShop {

    public enum Category {
        BLOECKE("§8Blöcke", Material.STONE, 0),
        ERZE("§7Erze & Mineralien", Material.DIAMOND_ORE, 1),
        HOLZ("§6Holz & Pflanzen", Material.OAK_LOG, 2),
        WERKZEUGE("§bWerkzeuge", Material.IRON_PICKAXE, 3),
        RUESTUNG("§aRüstung", Material.IRON_CHESTPLATE, 4),
        WAFFEN("§cWaffen", Material.IRON_SWORD, 5),
        NAHRUNG("§eNahrung", Material.COOKED_BEEF, 6),
        REDSTONE("§4Redstone", Material.REDSTONE, 7),
        NETHER("§5Nether", Material.NETHERRACK, 8),
        SONSTIGES("§dSonstiges", Material.CHEST, 9);

        public final String display;
        public final Material icon;
        public final int index;

        Category(String display, Material icon, int index) {
            this.display = display;
            this.icon = icon;
            this.index = index;
        }
    }

    private static final Map<Category, List<ShopItem>> ITEMS = new LinkedHashMap<>();

    static {
        // ── BLÖCKE ──────────────────────────────────────────────────────────
        List<ShopItem> bloecke = new ArrayList<>();
        add(bloecke, Material.STONE, 2);
        add(bloecke, Material.COBBLESTONE, 1);
        add(bloecke, Material.GRANITE, 2);
        add(bloecke, Material.DIORITE, 2);
        add(bloecke, Material.ANDESITE, 2);
        add(bloecke, Material.DEEPSLATE, 3);
        add(bloecke, Material.COBBLED_DEEPSLATE, 2);
        add(bloecke, Material.TUFF, 2);
        add(bloecke, Material.CALCITE, 3);
        add(bloecke, Material.OBSIDIAN, 20);
        add(bloecke, Material.CRYING_OBSIDIAN, 30);
        add(bloecke, Material.DIRT, 1);
        add(bloecke, Material.GRASS_BLOCK, 2);
        add(bloecke, Material.GRAVEL, 1);
        add(bloecke, Material.SAND, 2);
        add(bloecke, Material.RED_SAND, 3);
        add(bloecke, Material.SANDSTONE, 4);
        add(bloecke, Material.RED_SANDSTONE, 5);
        add(bloecke, Material.CLAY, 3);
        add(bloecke, Material.TERRACOTTA, 4);
        add(bloecke, Material.WHITE_TERRACOTTA, 5);
        add(bloecke, Material.WHITE_CONCRETE, 6);
        add(bloecke, Material.WHITE_WOOL, 4);
        add(bloecke, Material.ICE, 5);
        add(bloecke, Material.PACKED_ICE, 8);
        add(bloecke, Material.BLUE_ICE, 15);
        add(bloecke, Material.SNOW_BLOCK, 3);
        add(bloecke, Material.GLASS, 4);
        add(bloecke, Material.BRICKS, 8);
        add(bloecke, Material.STONE_BRICKS, 5);
        add(bloecke, Material.MOSSY_STONE_BRICKS, 8);
        add(bloecke, Material.CRACKED_STONE_BRICKS, 6);
        add(bloecke, Material.CHISELED_STONE_BRICKS, 7);
        add(bloecke, Material.BOOKSHELF, 20);
        ITEMS.put(Category.BLOECKE, bloecke);

        // ── ERZE ──────────────────────────────────────────────────────────
        List<ShopItem> erze = new ArrayList<>();
        add(erze, Material.COAL, 4);
        add(erze, Material.COAL_ORE, 8);
        add(erze, Material.IRON_INGOT, 10);
        add(erze, Material.IRON_ORE, 12);
        add(erze, Material.RAW_IRON, 10);
        add(erze, Material.COPPER_INGOT, 8);
        add(erze, Material.RAW_COPPER, 6);
        add(erze, Material.GOLD_INGOT, 30);
        add(erze, Material.GOLD_ORE, 35);
        add(erze, Material.RAW_GOLD, 28);
        add(erze, Material.DIAMOND, 100);
        add(erze, Material.DIAMOND_ORE, 120);
        add(erze, Material.EMERALD, 80);
        add(erze, Material.EMERALD_ORE, 100);
        add(erze, Material.LAPIS_LAZULI, 15);
        add(erze, Material.REDSTONE, 5);
        add(erze, Material.QUARTZ, 10);
        add(erze, Material.NETHERITE_INGOT, 1000);
        add(erze, Material.NETHERITE_SCRAP, 300);
        add(erze, Material.AMETHYST_SHARD, 20);
        add(erze, Material.IRON_BLOCK, 90);
        add(erze, Material.GOLD_BLOCK, 270);
        add(erze, Material.DIAMOND_BLOCK, 900);
        add(erze, Material.EMERALD_BLOCK, 720);
        ITEMS.put(Category.ERZE, erze);

        // ── HOLZ ──────────────────────────────────────────────────────────
        List<ShopItem> holz = new ArrayList<>();
        add(holz, Material.OAK_LOG, 3);
        add(holz, Material.SPRUCE_LOG, 3);
        add(holz, Material.BIRCH_LOG, 3);
        add(holz, Material.JUNGLE_LOG, 4);
        add(holz, Material.ACACIA_LOG, 4);
        add(holz, Material.DARK_OAK_LOG, 4);
        add(holz, Material.CHERRY_LOG, 6);
        add(holz, Material.MANGROVE_LOG, 5);
        add(holz, Material.OAK_PLANKS, 2);
        add(holz, Material.OAK_SAPLING, 2);
        add(holz, Material.SPRUCE_SAPLING, 2);
        add(holz, Material.WHEAT, 2);
        add(holz, Material.WHEAT_SEEDS, 1);
        add(holz, Material.CARROT, 2);
        add(holz, Material.POTATO, 2);
        add(holz, Material.BEETROOT, 2);
        add(holz, Material.BEETROOT_SEEDS, 1);
        add(holz, Material.MELON, 3);
        add(holz, Material.PUMPKIN, 3);
        add(holz, Material.SUGAR_CANE, 2);
        add(holz, Material.BAMBOO, 1);
        add(holz, Material.CACTUS, 2);
        add(holz, Material.VINE, 3);
        add(holz, Material.LILY_PAD, 4);
        add(holz, Material.NETHER_WART, 5);
        add(holz, Material.COCOA_BEANS, 4);
        add(holz, Material.DANDELION, 1);
        add(holz, Material.POPPY, 1);
        ITEMS.put(Category.HOLZ, holz);

        // ── WERKZEUGE ──────────────────────────────────────────────────────
        List<ShopItem> werkzeuge = new ArrayList<>();
        add(werkzeuge, Material.WOODEN_PICKAXE, 10);
        add(werkzeuge, Material.STONE_PICKAXE, 20);
        add(werkzeuge, Material.IRON_PICKAXE, 50);
        add(werkzeuge, Material.GOLDEN_PICKAXE, 80);
        add(werkzeuge, Material.DIAMOND_PICKAXE, 300);
        add(werkzeuge, Material.NETHERITE_PICKAXE, 1200);
        add(werkzeuge, Material.IRON_SHOVEL, 40);
        add(werkzeuge, Material.DIAMOND_SHOVEL, 250);
        add(werkzeuge, Material.IRON_AXE, 45);
        add(werkzeuge, Material.DIAMOND_AXE, 280);
        add(werkzeuge, Material.IRON_HOE, 40);
        add(werkzeuge, Material.DIAMOND_HOE, 250);
        add(werkzeuge, Material.FISHING_ROD, 30);
        add(werkzeuge, Material.BOW, 40);
        add(werkzeuge, Material.CROSSBOW, 60);
        add(werkzeuge, Material.FLINT_AND_STEEL, 25);
        add(werkzeuge, Material.SHEARS, 20);
        add(werkzeuge, Material.COMPASS, 30);
        add(werkzeuge, Material.CLOCK, 50);
        add(werkzeuge, Material.BUCKET, 20);
        add(werkzeuge, Material.WATER_BUCKET, 25);
        add(werkzeuge, Material.LAVA_BUCKET, 30);
        add(werkzeuge, Material.ELYTRA, 5000);
        ITEMS.put(Category.WERKZEUGE, werkzeuge);

        // ── RÜSTUNG ──────────────────────────────────────────────────────────
        List<ShopItem> ruestung = new ArrayList<>();
        add(ruestung, Material.LEATHER_HELMET, 20);
        add(ruestung, Material.LEATHER_CHESTPLATE, 30);
        add(ruestung, Material.LEATHER_LEGGINGS, 25);
        add(ruestung, Material.LEATHER_BOOTS, 20);
        add(ruestung, Material.IRON_HELMET, 60);
        add(ruestung, Material.IRON_CHESTPLATE, 90);
        add(ruestung, Material.IRON_LEGGINGS, 75);
        add(ruestung, Material.IRON_BOOTS, 55);
        add(ruestung, Material.GOLDEN_HELMET, 80);
        add(ruestung, Material.GOLDEN_CHESTPLATE, 120);
        add(ruestung, Material.DIAMOND_HELMET, 350);
        add(ruestung, Material.DIAMOND_CHESTPLATE, 500);
        add(ruestung, Material.DIAMOND_LEGGINGS, 430);
        add(ruestung, Material.DIAMOND_BOOTS, 330);
        add(ruestung, Material.NETHERITE_HELMET, 1400);
        add(ruestung, Material.NETHERITE_CHESTPLATE, 2000);
        add(ruestung, Material.NETHERITE_LEGGINGS, 1800);
        add(ruestung, Material.NETHERITE_BOOTS, 1400);
        add(ruestung, Material.SHIELD, 50);
        add(ruestung, Material.TOTEM_OF_UNDYING, 2000);
        ITEMS.put(Category.RUESTUNG, ruestung);

        // ── WAFFEN ──────────────────────────────────────────────────────────
        List<ShopItem> waffen = new ArrayList<>();
        add(waffen, Material.WOODEN_SWORD, 8);
        add(waffen, Material.STONE_SWORD, 15);
        add(waffen, Material.IRON_SWORD, 45);
        add(waffen, Material.GOLDEN_SWORD, 70);
        add(waffen, Material.DIAMOND_SWORD, 280);
        add(waffen, Material.NETHERITE_SWORD, 1100);
        add(waffen, Material.ARROW, 1);
        add(waffen, Material.SPECTRAL_ARROW, 3);
        add(waffen, Material.TIPPED_ARROW, 5);
        add(waffen, Material.TRIDENT, 500);
        add(waffen, Material.TNT, 30);
        add(waffen, Material.SNOWBALL, 1);
        add(waffen, Material.EGG, 1);
        add(waffen, Material.FIRE_CHARGE, 10);
        ITEMS.put(Category.WAFFEN, waffen);

        // ── NAHRUNG ──────────────────────────────────────────────────────────
        List<ShopItem> nahrung = new ArrayList<>();
        add(nahrung, Material.BREAD, 5);
        add(nahrung, Material.COOKED_BEEF, 8);
        add(nahrung, Material.BEEF, 4);
        add(nahrung, Material.COOKED_PORKCHOP, 8);
        add(nahrung, Material.COOKED_CHICKEN, 6);
        add(nahrung, Material.COOKED_MUTTON, 7);
        add(nahrung, Material.COOKED_RABBIT, 7);
        add(nahrung, Material.COOKED_SALMON, 7);
        add(nahrung, Material.COOKED_COD, 6);
        add(nahrung, Material.BAKED_POTATO, 4);
        add(nahrung, Material.COOKIE, 2);
        add(nahrung, Material.CAKE, 20);
        add(nahrung, Material.PUMPKIN_PIE, 8);
        add(nahrung, Material.GOLDEN_APPLE, 50);
        add(nahrung, Material.ENCHANTED_GOLDEN_APPLE, 2000);
        add(nahrung, Material.HONEY_BOTTLE, 10);
        add(nahrung, Material.MUSHROOM_STEW, 6);
        add(nahrung, Material.RABBIT_STEW, 10);
        add(nahrung, Material.BEETROOT_SOUP, 8);
        ITEMS.put(Category.NAHRUNG, nahrung);

        // ── REDSTONE ──────────────────────────────────────────────────────────
        List<ShopItem> redstone = new ArrayList<>();
        add(redstone, Material.REDSTONE, 5);
        add(redstone, Material.REDSTONE_BLOCK, 45);
        add(redstone, Material.REDSTONE_TORCH, 3);
        add(redstone, Material.REPEATER, 8);
        add(redstone, Material.COMPARATOR, 12);
        add(redstone, Material.PISTON, 15);
        add(redstone, Material.STICKY_PISTON, 20);
        add(redstone, Material.OBSERVER, 18);
        add(redstone, Material.DISPENSER, 25);
        add(redstone, Material.DROPPER, 20);
        add(redstone, Material.HOPPER, 30);
        add(redstone, Material.LEVER, 3);
        add(redstone, Material.STONE_BUTTON, 2);
        add(redstone, Material.STONE_PRESSURE_PLATE, 3);
        add(redstone, Material.TRIPWIRE_HOOK, 5);
        add(redstone, Material.DAYLIGHT_DETECTOR, 20);
        add(redstone, Material.TARGET, 15);
        add(redstone, Material.NOTE_BLOCK, 15);
        add(redstone, Material.JUKEBOX, 40);
        ITEMS.put(Category.REDSTONE, redstone);

        // ── NETHER ──────────────────────────────────────────────────────────
        List<ShopItem> nether = new ArrayList<>();
        add(nether, Material.NETHERRACK, 1);
        add(nether, Material.SOUL_SAND, 3);
        add(nether, Material.SOUL_SOIL, 3);
        add(nether, Material.BASALT, 2);
        add(nether, Material.BLACKSTONE, 2);
        add(nether, Material.NETHER_BRICKS, 5);
        add(nether, Material.NETHER_QUARTZ_ORE, 15);
        add(nether, Material.QUARTZ_BLOCK, 12);
        add(nether, Material.GLOWSTONE, 10);
        add(nether, Material.GLOWSTONE_DUST, 3);
        add(nether, Material.BLAZE_ROD, 20);
        add(nether, Material.BLAZE_POWDER, 12);
        add(nether, Material.GHAST_TEAR, 30);
        add(nether, Material.MAGMA_CREAM, 15);
        add(nether, Material.NETHER_STAR, 2000);
        add(nether, Material.WITHER_SKELETON_SKULL, 500);
        add(nether, Material.ENDER_PEARL, 20);
        add(nether, Material.ENDER_EYE, 40);
        add(nether, Material.SHULKER_SHELL, 100);
        add(nether, Material.DRAGON_BREATH, 200);
        ITEMS.put(Category.NETHER, nether);

        // ── SONSTIGES ──────────────────────────────────────────────────────────
        List<ShopItem> sonstiges = new ArrayList<>();
        add(sonstiges, Material.STRING, 3);
        add(sonstiges, Material.FEATHER, 3);
        add(sonstiges, Material.LEATHER, 8);
        add(sonstiges, Material.BONE, 4);
        add(sonstiges, Material.BONE_MEAL, 2);
        add(sonstiges, Material.INK_SAC, 3);
        add(sonstiges, Material.SLIME_BALL, 10);
        add(sonstiges, Material.SPIDER_EYE, 5);
        add(sonstiges, Material.GUNPOWDER, 8);
        add(sonstiges, Material.ROTTEN_FLESH, 1);
        add(sonstiges, Material.ENDER_CHEST, 200);
        add(sonstiges, Material.CHEST, 10);
        add(sonstiges, Material.BARREL, 15);
        add(sonstiges, Material.CRAFTING_TABLE, 8);
        add(sonstiges, Material.FURNACE, 15);
        add(sonstiges, Material.BLAST_FURNACE, 40);
        add(sonstiges, Material.SMOKER, 40);
        add(sonstiges, Material.ANVIL, 100);
        add(sonstiges, Material.ENCHANTING_TABLE, 300);
        add(sonstiges, Material.EXPERIENCE_BOTTLE, 20);
        add(sonstiges, Material.NAME_TAG, 50);
        add(sonstiges, Material.SADDLE, 40);
        add(sonstiges, Material.LEAD, 15);
        add(sonstiges, Material.BOOK, 10);
        add(sonstiges, Material.PAPER, 2);
        add(sonstiges, Material.MAP, 8);
        ITEMS.put(Category.SONSTIGES, sonstiges);
    }

    private static void add(List<ShopItem> list, Material mat, double price) {
        list.add(new ShopItem(mat, price, price / 2.0));
    }

    public static List<ShopItem> getItems(Category category) {
        return ITEMS.getOrDefault(category, List.of());
    }

    public static ShopItem findItem(Material material) {
        for (List<ShopItem> items : ITEMS.values()) {
            for (ShopItem item : items) {
                if (item.material() == material) return item;
            }
        }
        return null;
    }
}
