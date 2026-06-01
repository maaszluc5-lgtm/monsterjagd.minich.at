package at.minich.opserver.kits;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.economy.EconomyManager;
import at.minich.opserver.items.CustomItems;
import at.minich.opserver.util.DataManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Manages kit definitions from config.yml and per-player cooldowns in player YAML files.
 */
public class KitManager {

    public static class KitInfo {
        public final String name;
        public final String permission;  // null = no permission required
        public final long cooldownHours;
        public final List<ItemStack> items;
        public final double coins;

        public KitInfo(String name, String permission, long cooldownHours, List<ItemStack> items, double coins) {
            this.name = name;
            this.permission = permission;
            this.cooldownHours = cooldownHours;
            this.items = Collections.unmodifiableList(items);
            this.coins = coins;
        }
    }

    private final OpServerPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final Map<String, KitInfo> kits = new LinkedHashMap<>();

    public KitManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.economyManager = plugin.getEconomyManager();
        loadKits();
    }

    public void loadKits() {
        kits.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("kits");
        if (sec == null) return;

        for (String kitName : sec.getKeys(false)) {
            ConfigurationSection ks = sec.getConfigurationSection(kitName);
            if (ks == null) continue;

            String permission = ks.getString("permission", null);
            long cooldownHours = ks.getLong("cooldown-hours", 24);
            double coins = ks.getDouble("coins", 0.0);

            List<ItemStack> items = new ArrayList<>();

            // Parse standard items list (FORMAT: MATERIAL:AMOUNT)
            List<String> itemStrings = ks.getStringList("items");
            for (String entry : itemStrings) {
                String[] parts = entry.split(":");
                try {
                    Material mat = Material.valueOf(parts[0].toUpperCase());
                    int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    items.add(new ItemStack(mat, amount));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown material in kit '" + kitName + "': " + parts[0]);
                }
            }

            // Parse custom items (GOD_SWORD, GOD_ARMOR_SET)
            List<String> customItems = ks.getStringList("custom-items");
            for (String ci : customItems) {
                if (ci.equalsIgnoreCase("GOD_SWORD")) {
                    items.add(CustomItems.GOD_SWORD.build(plugin.getEnchantManager()));
                } else if (ci.equalsIgnoreCase("GOD_ARMOR_SET")) {
                    for (ItemStack armor : CustomItems.buildGodArmorSet(plugin.getEnchantManager())) {
                        items.add(armor);
                    }
                } else {
                    CustomItems customItem = CustomItems.fromString(ci);
                    if (customItem != null) {
                        items.add(customItem.build(plugin.getEnchantManager()));
                    } else {
                        plugin.getLogger().warning("Unknown custom item in kit '" + kitName + "': " + ci);
                    }
                }
            }

            kits.put(kitName.toLowerCase(), new KitInfo(kitName, permission, cooldownHours, items, coins));
        }
    }

    /**
     * Attempts to give a kit to the player.
     * Returns true on success, false on failure (cooldown, permission, unknown kit).
     */
    public boolean giveKit(Player player, String kitName) {
        KitInfo kit = kits.get(kitName.toLowerCase());
        if (kit == null) {
            player.sendMessage("§cKit §e" + kitName + "§c does not exist. Use §f/kitlist§c to see available kits.");
            return false;
        }

        // Permission check
        if (kit.permission != null && !player.hasPermission(kit.permission)) {
            player.sendMessage("§cYou don't have permission to use the §e" + kit.name + "§c kit.");
            return false;
        }

        UUID uuid = player.getUniqueId();

        // Cooldown check
        long cooldownRemaining = getCooldownSeconds(uuid, kit.name);
        if (cooldownRemaining > 0) {
            long hoursLeft = cooldownRemaining / 3600;
            long minsLeft = (cooldownRemaining % 3600) / 60;
            player.sendMessage("§cKit §e" + kit.name + "§c is on cooldown! Available in §e"
                    + hoursLeft + "h " + minsLeft + "m§c.");
            return false;
        }

        // Give items
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(kit.items.toArray(new ItemStack[0]));
        if (!leftovers.isEmpty()) {
            // Drop leftovers at player's feet
            leftovers.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
            player.sendMessage("§7(Some items were dropped because your inventory was full.)");
        }

        // Give coins
        if (kit.coins > 0) {
            economyManager.deposit(uuid, kit.coins);
        }

        // Save cooldown
        setCooldown(uuid, kit.name);

        // Notify
        player.sendMessage("§a§l★ Kit Claimed: §r§6" + kit.name);
        if (kit.coins > 0) {
            player.sendMessage("§a+¢" + String.format("%.0f", kit.coins) + " §7coins added!");
        }
        player.sendMessage("§7Items added to your inventory.");
        return true;
    }

    /**
     * Returns seconds remaining on the kit cooldown, or 0 if available.
     */
    public long getCooldownSeconds(UUID uuid, String kitName) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        String key = "kits." + kitName.toLowerCase() + ".last-claim";
        String lastClaimStr = cfg.getString(key, null);
        if (lastClaimStr == null) return 0;

        KitInfo kit = kits.get(kitName.toLowerCase());
        if (kit == null) return 0;

        Instant lastClaim = Instant.parse(lastClaimStr);
        Instant available = lastClaim.plus(kit.cooldownHours, ChronoUnit.HOURS);
        long remaining = ChronoUnit.SECONDS.between(Instant.now(), available);
        return Math.max(0, remaining);
    }

    private void setCooldown(UUID uuid, String kitName) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        cfg.set("kits." + kitName.toLowerCase() + ".last-claim", Instant.now().toString());
        dataManager.saveYaml(cfg, path);
    }

    public Map<String, KitInfo> getKits() {
        return Collections.unmodifiableMap(kits);
    }

    public KitInfo getKit(String name) {
        return kits.get(name.toLowerCase());
    }
}
