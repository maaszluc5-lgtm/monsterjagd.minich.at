package at.minich.opserver.chestshop;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Manages all chest shops: load/save, creation, removal, hologram lifecycle.
 */
public class ChestShopManager {

    private static final String FILE = "chestshops.yml";

    private final OpServerPlugin plugin;

    /** signLoc → ChestShop */
    private final Map<Location, ChestShop> bySign = new HashMap<>();
    /** chestLoc → ChestShop */
    private final Map<Location, ChestShop> byChest = new HashMap<>();

    public ChestShopManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    public void load() {
        bySign.clear();
        byChest.clear();

        YamlConfiguration cfg = plugin.getDataManager().loadYaml(FILE);
        ConfigurationSection shops = cfg.getConfigurationSection("shops");
        if (shops == null) return;

        for (String idStr : shops.getKeys(false)) {
            ConfigurationSection s = shops.getConfigurationSection(idStr);
            if (s == null) continue;
            try {
                UUID id = UUID.fromString(idStr);
                UUID ownerUuid = UUID.fromString(s.getString("owner-uuid"));
                String ownerName = s.getString("owner-name", "Unknown");
                Location chestLoc = deserializeLocation(s.getString("chest-loc"));
                Location signLoc = deserializeLocation(s.getString("sign-loc"));
                if (chestLoc == null || signLoc == null) continue;

                ItemStack item = s.getItemStack("item");
                if (item == null) continue;

                int amount = s.getInt("amount", 1);
                double buyPrice = s.getDouble("buy-price", 0);
                double sellPrice = s.getDouble("sell-price", 0);

                ChestShop shop = new ChestShop(id, ownerUuid, ownerName,
                        chestLoc, signLoc, item, amount, buyPrice, sellPrice);

                String holoStr = s.getString("hologram-entity");
                if (holoStr != null) {
                    try { shop.setHologramEntityId(UUID.fromString(holoStr)); }
                    catch (IllegalArgumentException ignored) {}
                }

                bySign.put(signLoc, shop);
                byChest.put(chestLoc, shop);
            } catch (Exception e) {
                plugin.getLogger().warning("[ChestShop] Could not load shop " + idStr + ": " + e.getMessage());
            }
        }

        // Re-spawn holograms after world loads (slight delay so worlds are ready)
        Bukkit.getScheduler().runTaskLater(plugin, this::respawnAllHolograms, 40L);
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (ChestShop shop : bySign.values()) {
            String base = "shops." + shop.getId();
            cfg.set(base + ".owner-uuid", shop.getOwnerUuid().toString());
            cfg.set(base + ".owner-name", shop.getOwnerName());
            cfg.set(base + ".chest-loc", serializeLocation(shop.getChestLoc()));
            cfg.set(base + ".sign-loc", serializeLocation(shop.getSignLoc()));
            cfg.set(base + ".item", shop.getItem());
            cfg.set(base + ".amount", shop.getAmount());
            cfg.set(base + ".buy-price", shop.getBuyPrice());
            cfg.set(base + ".sell-price", shop.getSellPrice());
            if (shop.getHologramEntityId() != null) {
                cfg.set(base + ".hologram-entity", shop.getHologramEntityId().toString());
            }
        }
        plugin.getDataManager().saveYaml(cfg, FILE);
    }

    // -------------------------------------------------------------------------
    // Creation / removal
    // -------------------------------------------------------------------------

    /**
     * Creates a new shop and persists it.
     * @return the created ChestShop, or null if creation failed
     */
    public ChestShop createShop(UUID ownerUuid, String ownerName,
                                 Location chestLoc, Location signLoc,
                                 ItemStack item, int amount,
                                 double buyPrice, double sellPrice) {
        ChestShop shop = new ChestShop(UUID.randomUUID(), ownerUuid, ownerName,
                chestLoc, signLoc, item, amount, buyPrice, sellPrice);
        bySign.put(signLoc, shop);
        byChest.put(chestLoc, shop);
        spawnHologram(shop);
        save();
        return shop;
    }

    public void removeShop(ChestShop shop) {
        removeHologram(shop);
        bySign.remove(shop.getSignLoc());
        byChest.remove(shop.getChestLoc());
        save();
    }

    // -------------------------------------------------------------------------
    // Lookups
    // -------------------------------------------------------------------------

    public ChestShop getBySign(Location loc) {
        return bySign.get(loc);
    }

    public ChestShop getByChest(Location loc) {
        return byChest.get(loc);
    }

    public Collection<ChestShop> getAllShops() {
        return bySign.values();
    }

    /** Count how many shops the given player owns. */
    public int countShopsForPlayer(UUID uuid) {
        int count = 0;
        for (ChestShop shop : bySign.values()) {
            if (shop.getOwnerUuid().equals(uuid)) count++;
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // Hologram management
    // -------------------------------------------------------------------------

    private void respawnAllHolograms() {
        for (ChestShop shop : bySign.values()) {
            // Kill any existing entity first
            removeHologram(shop);
            spawnHologram(shop);
        }
        save();
    }

    public void spawnHologram(ChestShop shop) {
        Location chestLoc = shop.getChestLoc();
        World world = chestLoc.getWorld();
        if (world == null) return;

        // Hologram floats 1.5 blocks above the chest centre
        Location holoLoc = chestLoc.clone().add(0.5, 1.5, 0.5);

        ArmorStand stand = world.spawn(holoLoc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setMarker(true);
            as.setCustomNameVisible(true);
            as.setSmall(true);
            as.setBasePlate(false);
            as.setArms(false);
            as.setCustomName(buildHologramText(shop));
        });

        shop.setHologramEntityId(stand.getUniqueId());
    }

    public void removeHologram(ChestShop shop) {
        UUID entityId = shop.getHologramEntityId();
        if (entityId == null) return;
        Entity entity = Bukkit.getEntity(entityId);
        if (entity != null) entity.remove();
        shop.setHologramEntityId(null);
    }

    public void updateHologram(ChestShop shop) {
        UUID entityId = shop.getHologramEntityId();
        if (entityId != null) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity instanceof ArmorStand as) {
                as.setCustomName(buildHologramText(shop));
                return;
            }
        }
        // Not found – re-spawn
        spawnHologram(shop);
        save();
    }

    /** Builds the hologram display text for a shop. */
    public String buildHologramText(ChestShop shop) {
        int stock = getStock(shop);
        String itemName = getItemDisplayName(shop.getItem());

        StringBuilder sb = new StringBuilder();
        sb.append("§6[Shop] §e").append(shop.getOwnerName()).append("\n");
        sb.append("§f").append(shop.getAmount()).append("x §b").append(itemName).append("\n");
        sb.append("§aKaufen: §f").append(formatCoins(shop.getBuyPrice()));
        if (shop.hasSellPrice()) {
            sb.append(" §7| §cVerkaufen: §f").append(formatCoins(shop.getSellPrice()));
        }
        sb.append("\n");
        if (stock <= 0) {
            sb.append("§cAusverkauft!");
        } else {
            sb.append("§7Vorrat: §f").append(stock);
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Stock helpers
    // -------------------------------------------------------------------------

    /** Returns how many full purchase-amounts are available in the chest. */
    public int getStock(ChestShop shop) {
        Location chestLoc = shop.getChestLoc();
        if (chestLoc.getWorld() == null) return 0;
        org.bukkit.block.Block block = chestLoc.getBlock();
        if (!(block.getState() instanceof org.bukkit.block.Chest chest)) return 0;

        ItemStack template = shop.getItem();
        int total = 0;
        for (ItemStack stack : chest.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(template)) {
                total += stack.getAmount();
            }
        }
        return total / shop.getAmount();
    }

    /** Remove `amount` items from the chest. Returns true on success. */
    public boolean removeFromChest(ChestShop shop, int amountToRemove) {
        org.bukkit.block.Block block = shop.getChestLoc().getBlock();
        if (!(block.getState() instanceof org.bukkit.block.Chest chest)) return false;

        ItemStack template = shop.getItem();
        int remaining = amountToRemove;
        ItemStack[] contents = chest.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.isSimilar(template)) {
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
                if (stack.getAmount() == 0) contents[i] = null;
            }
        }
        chest.getInventory().setContents(contents);
        return remaining == 0;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private String getItemDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        // Convert material name to friendly title case
        String raw = item.getType().name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String formatCoins(double amount) {
        if (amount == (long) amount) return String.valueOf((long) amount) + " Coins";
        return String.format("%.2f Coins", amount);
    }

    // -------------------------------------------------------------------------
    // Location serialization  (world:x:y:z)
    // -------------------------------------------------------------------------

    private static String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private static Location deserializeLocation(String s) {
        if (s == null) return null;
        String[] parts = s.split(":");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
