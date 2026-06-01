package at.minich.opserver.jobs;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.items.CustomItems;
import at.minich.opserver.util.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

/**
 * Manages job data for all players.
 * Each player's job data is stored in plugins/OpServer/jobs/<uuid>.yml
 */
public class JobManager {

    private final OpServerPlugin plugin;
    private final DataManager dataManager;

    public JobManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        dataManager.ensureDir("jobs");
    }

    // -------------------------------------------------------------------------
    // Level formula
    // -------------------------------------------------------------------------

    /**
     * Actions required to be AT this level (i.e. to have reached level).
     * Level 1 = 100, Level 100 = 100,000,000 (exponential).
     */
    public long getRequiredActions(int level) {
        return Math.round(100 * Math.pow(10, (level - 1) * 6.0 / 99.0));
    }

    // -------------------------------------------------------------------------
    // Data access
    // -------------------------------------------------------------------------

    private String path(UUID uuid) {
        return "jobs/" + uuid + ".yml";
    }

    private YamlConfiguration load(UUID uuid) {
        return dataManager.loadYaml(path(uuid));
    }

    private void save(UUID uuid, YamlConfiguration cfg) {
        dataManager.saveYaml(cfg, path(uuid));
    }

    /** Returns the player's current job, or null if none selected. */
    public Job getJob(UUID uuid) {
        YamlConfiguration cfg = load(uuid);
        String jobName = cfg.getString("job", null);
        if (jobName == null) return null;
        try {
            return Job.valueOf(jobName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Returns the player's level for their current job (1-100). */
    public int getLevel(UUID uuid) {
        YamlConfiguration cfg = load(uuid);
        return cfg.getInt("level", 1);
    }

    /** Returns the player's action count for their current level. */
    public long getActions(UUID uuid) {
        YamlConfiguration cfg = load(uuid);
        return cfg.getLong("actions", 0);
    }

    /** Returns total lifetime actions for the player's current job. */
    public long getTotalActions(UUID uuid) {
        YamlConfiguration cfg = load(uuid);
        return cfg.getLong("total-actions", 0);
    }

    /**
     * Sets the player's job. Resets level/actions to 1/0.
     */
    public void setJob(UUID uuid, Job job) {
        YamlConfiguration cfg = load(uuid);
        cfg.set("job", job == null ? null : job.name());
        cfg.set("level", 1);
        cfg.set("actions", 0);
        cfg.set("total-actions", 0);
        save(uuid, cfg);
    }

    /**
     * Adds actions to the player's job counter, handles level-up and rewards.
     * @param uuid player
     * @param amount how many actions to add
     */
    public void addActions(UUID uuid, int amount) {
        YamlConfiguration cfg = load(uuid);
        Job job = getJob(uuid);
        if (job == null) return;

        int currentLevel = cfg.getInt("level", 1);
        if (currentLevel >= 100) return; // already max

        long currentActions = cfg.getLong("actions", 0);
        long totalActions = cfg.getLong("total-actions", 0);

        currentActions += amount;
        totalActions += amount;

        // Check for level-up(s)
        while (currentLevel < 100) {
            long required = getRequiredActions(currentLevel);
            if (currentActions >= required) {
                currentActions -= required;
                currentLevel++;
                // Give rewards for this new level
                checkAndGiveRewards(uuid, currentLevel);
            } else {
                break;
            }
        }

        if (currentLevel >= 100) {
            currentLevel = 100;
            currentActions = 0;
        }

        cfg.set("level", currentLevel);
        cfg.set("actions", currentActions);
        cfg.set("total-actions", totalActions);
        save(uuid, cfg);
    }

    /**
     * Checks if any reward should be given for the newly reached level and distributes it.
     */
    public void checkAndGiveRewards(UUID uuid, int newLevel) {
        Player player = Bukkit.getPlayer(uuid);
        Job job = getJob(uuid);
        if (job == null) return;

        // Level-up announcement
        if (player != null) {
            player.sendMessage("§6§l[Beruf] §r§aGlückwunsch! Du hast " + job.getDisplayName()
                    + " §aLevel §e" + newLevel + " §aerreicht!");
        }

        // Only distribute rewards at multiples of 5
        if (newLevel % 5 != 0) return;

        String jobKey = job.name().toLowerCase();
        ConfigurationSection rewardsSec = plugin.getConfig()
                .getConfigurationSection("jobs.rewards." + jobKey);
        if (rewardsSec == null) return;

        for (String key : rewardsSec.getKeys(false)) {
            ConfigurationSection entry = rewardsSec.getConfigurationSection(key);
            if (entry == null) continue;
            int rewardLevel = entry.getInt("level", -1);
            if (rewardLevel != newLevel) continue;

            // Coins
            double coins = entry.getDouble("coins", 0);
            if (coins > 0) {
                plugin.getEconomyManager().deposit(uuid, coins);
                if (player != null) {
                    player.sendMessage("§a+§e" + String.format("%.0f", coins)
                            + " §aCoins als Berufsbelohnung für Level " + newLevel + "!");
                }
            }

            // Vanilla items
            List<String> itemStrings = entry.getStringList("items");
            List<ItemStack> toGive = new ArrayList<>();
            for (String entry2 : itemStrings) {
                String[] parts = entry2.split(":");
                try {
                    Material mat = Material.valueOf(parts[0].toUpperCase());
                    int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    toGive.add(new ItemStack(mat, amount));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Unknown material in job reward: " + entry2);
                }
            }

            // Custom items
            List<String> customItems = entry.getStringList("custom-items");
            for (String ci : customItems) {
                if (ci.equalsIgnoreCase("GOD_ARMOR_SET")) {
                    for (ItemStack armor : CustomItems.buildGodArmorSet(plugin.getEnchantManager())) {
                        toGive.add(armor);
                    }
                } else {
                    CustomItems customItem = CustomItems.fromString(ci);
                    if (customItem != null) {
                        toGive.add(customItem.build(plugin.getEnchantManager()));
                    } else {
                        plugin.getLogger().warning("Unknown custom item in job reward: " + ci);
                    }
                }
            }

            if (player != null && !toGive.isEmpty()) {
                Map<Integer, ItemStack> leftovers = player.getInventory()
                        .addItem(toGive.toArray(new ItemStack[0]));
                leftovers.values().forEach(item ->
                        player.getWorld().dropItem(player.getLocation(), item));
                player.sendMessage("§aGegenstände für Level " + newLevel + " erhalten!");
            }

            // Broadcast message
            String msg = entry.getString("message", null);
            if (msg != null && !msg.isEmpty()) {
                String finalMsg = msg.replace("<player>",
                        player != null ? player.getName() : uuid.toString());
                Bukkit.broadcastMessage(finalMsg);
            }
        }

        // Trophy check at level 100
        if (newLevel >= 100 && player != null && job != null) {
            plugin.getTrophyManager().checkAndGiveJobTrophy(player, job);
        }
    }

    // -------------------------------------------------------------------------
    // Top players
    // -------------------------------------------------------------------------

    /**
     * Returns a list of (UUID, level) sorted descending by level for the given job.
     * Scans all job yml files.
     */
    public List<Map.Entry<UUID, Integer>> getTopPlayers(Job job, int limit) {
        Map<UUID, Integer> levelMap = new HashMap<>();
        java.io.File dir = dataManager.ensureDir("jobs");
        java.io.File[] files = dir.listFiles((f, name) -> name.endsWith(".yml"));
        if (files == null) return Collections.emptyList();

        for (java.io.File file : files) {
            String name = file.getName().replace(".yml", "");
            try {
                UUID uuid = UUID.fromString(name);
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                String jobName = cfg.getString("job", null);
                if (jobName == null) continue;
                try {
                    Job fileJob = Job.valueOf(jobName);
                    if (fileJob == job) {
                        levelMap.put(uuid, cfg.getInt("level", 1));
                    }
                } catch (IllegalArgumentException ignored) {
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(levelMap.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }
}
