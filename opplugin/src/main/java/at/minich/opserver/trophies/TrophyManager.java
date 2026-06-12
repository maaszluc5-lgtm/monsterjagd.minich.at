package at.minich.opserver.trophies;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.jobs.Job;
import at.minich.opserver.util.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Manages the Custom Trophy (Pokal) system.
 * Each player's trophies are stored in plugins/OpServer/trophies/<uuid>.yml
 */
public class TrophyManager {

    private final OpServerPlugin plugin;
    private final DataManager dataManager;
    private final NamespacedKey keyTrophyType;
    private final NamespacedKey keyTrophyOwner;

    public TrophyManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.keyTrophyType = new NamespacedKey(plugin, "trophy_type");
        this.keyTrophyOwner = new NamespacedKey(plugin, "trophy_owner");
        dataManager.ensureDir("trophies");
    }

    // -------------------------------------------------------------------------
    // File helpers
    // -------------------------------------------------------------------------

    private String path(UUID uuid) {
        return "trophies/" + uuid + ".yml";
    }

    private YamlConfiguration load(UUID uuid) {
        return dataManager.loadYaml(path(uuid));
    }

    private void save(UUID uuid, YamlConfiguration cfg) {
        dataManager.saveYaml(cfg, path(uuid));
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    /**
     * Returns true if the player already has the given trophy.
     */
    public boolean hasTrophy(UUID uuid, TrophyType type) {
        YamlConfiguration cfg = load(uuid);
        List<String> list = cfg.getStringList("trophies");
        return list.contains(type.name());
    }

    /**
     * Returns all trophy types the player has earned.
     */
    public List<TrophyType> getTrophies(UUID uuid) {
        YamlConfiguration cfg = load(uuid);
        List<String> list = cfg.getStringList("trophies");
        List<TrophyType> result = new ArrayList<>();
        for (String s : list) {
            try {
                result.add(TrophyType.valueOf(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Award
    // -------------------------------------------------------------------------

    /**
     * Awards a trophy to the player. Saves to file, gives the item, sends a message,
     * and checks for OVERLORD.
     */
    public void awardTrophy(Player player, TrophyType type) {
        UUID uuid = player.getUniqueId();
        if (hasTrophy(uuid, type)) return;

        // Save to file
        YamlConfiguration cfg = load(uuid);
        List<String> list = new ArrayList<>(cfg.getStringList("trophies"));
        list.add(type.name());
        cfg.set("trophies", list);
        save(uuid, cfg);

        // Give item
        ItemStack item = buildTrophyItem(type, player);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));

        // Message to player
        player.sendMessage("§6§l[Pokal] §r§aDu hast den Pokal §f" + type.getDisplayName()
                + " §aerhalten!");

        // Broadcast if enabled
        if (plugin.getConfig().getBoolean("trophies.broadcast-on-award", true)) {
            Bukkit.broadcastMessage("§6§l[Pokal] §r§e" + player.getName()
                    + " §ahat den Pokal §f" + type.getDisplayName() + " §aerhalten!");
        }

        // Check OVERLORD (only if this wasn't already OVERLORD)
        if (type != TrophyType.OVERLORD) {
            checkAndGiveOverlord(player);
        }
    }

    /**
     * Checks if the player has all non-OVERLORD trophies and awards OVERLORD if so.
     */
    private void checkAndGiveOverlord(Player player) {
        UUID uuid = player.getUniqueId();
        for (TrophyType type : TrophyType.values()) {
            if (type == TrophyType.OVERLORD) continue;
            if (!hasTrophy(uuid, type)) return;
        }
        // Player has all trophies — award OVERLORD
        if (!hasTrophy(uuid, TrophyType.OVERLORD)) {
            // Save first
            YamlConfiguration cfg = load(uuid);
            List<String> list = new ArrayList<>(cfg.getStringList("trophies"));
            list.add(TrophyType.OVERLORD.name());
            cfg.set("trophies", list);
            save(uuid, cfg);

            // Give item
            ItemStack item = buildTrophyItem(TrophyType.OVERLORD, player);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            leftovers.values().forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));

            // Player message
            player.sendMessage("§4§l[Pokal] §r§aDu hast alle Pokale gesammelt und bist OVERLORD!");

            // Server broadcast
            Bukkit.broadcastMessage("§4§l✦ §e" + player.getName()
                    + " §4§lhat alle Pokale gesammelt und ist OVERLORD! §4§l✦");
        }
    }

    // -------------------------------------------------------------------------
    // Build item
    // -------------------------------------------------------------------------

    /**
     * Creates the trophy ItemStack for the given type and player.
     */
    public ItemStack buildTrophyItem(TrophyType type, Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        meta.setDisplayName(type.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("§8§m--------------------");
        lore.add("§7Pokal: §f" + type.getDescription());
        lore.add("§7Vergeben an: §e" + player.getName());
        lore.add("§7Datum: §f" + date);
        lore.add("§8§m--------------------");
        lore.add("§8Einzigartiger Server-Pokal");
        meta.setLore(lore);

        // Glow via LUCK enchant
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Store trophy data in PDC
        meta.getPersistentDataContainer().set(keyTrophyType, PersistentDataType.STRING, type.name());
        meta.getPersistentDataContainer().set(keyTrophyOwner, PersistentDataType.STRING, player.getName());

        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // Job trophy integration
    // -------------------------------------------------------------------------

    /**
     * Maps a Job to its TrophyType and awards the trophy if the player is at level 100.
     */
    public void checkAndGiveJobTrophy(Player player, Job job) {
        TrophyType type = jobToTrophyType(job);
        if (type == null) return;
        int level = plugin.getJobManager().getLevel(player.getUniqueId(), job);
        if (level >= 100) {
            awardTrophy(player, type);
        }
    }

    private TrophyType jobToTrophyType(Job job) {
        return switch (job) {
            case JAEGER -> TrophyType.JAGER_MEISTER;
            case MIENENARBEITER -> TrophyType.BERGMANN_LEGENDE;
            case FARMER -> TrophyType.FARM_KONIG;
            case FISHER -> TrophyType.FISCHER_PROFI;
            case GRAEBER -> TrophyType.GRABER_CHAMPION;
            case BUILDER -> TrophyType.BAUMSTER_ELITE;
        };
    }

    // -------------------------------------------------------------------------
    // Check all trophies for a player (general)
    // -------------------------------------------------------------------------

    /**
     * Checks all applicable trophy conditions for the given player and awards any
     * that are newly earned. Intended to be called from relevant event handlers.
     */
    public void checkTrophies(Player player) {
        UUID uuid = player.getUniqueId();

        // REICHSTER_SPIELER: 10,000,000 coins
        double balance = plugin.getEconomyManager().getBalance(uuid);
        if (balance >= 10_000_000) {
            awardTrophy(player, TrophyType.REICHSTER_SPIELER);
        }

        // GOTT_SPIELER: GOTT rank (200h playtime = 720000 seconds)
        String playerPath = "players/" + uuid + ".yml";
        YamlConfiguration playerCfg = dataManager.loadYaml(playerPath);
        long playtimeSeconds = playerCfg.getLong("playtime-seconds", 0);
        if (playtimeSeconds >= 200L * 3600L) {
            awardTrophy(player, TrophyType.GOTT_SPIELER);
        }
    }

    // -------------------------------------------------------------------------
    // ERSTER_SPIELER helper
    // -------------------------------------------------------------------------

    /**
     * Returns true if any player already has the ERSTER_SPIELER trophy.
     */
    public boolean anyoneHasErsterSpieler() {
        File dir = dataManager.ensureDir("trophies");
        File[] files = dir.listFiles((f, name) -> name.endsWith(".yml"));
        if (files == null) return false;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            List<String> list = cfg.getStringList("trophies");
            if (list.contains(TrophyType.ERSTER_SPIELER.name())) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Getters for PDC keys (used by GUI etc.)
    // -------------------------------------------------------------------------

    public NamespacedKey getKeyTrophyType() {
        return keyTrophyType;
    }

    public NamespacedKey getKeyTrophyOwner() {
        return keyTrophyOwner;
    }
}
