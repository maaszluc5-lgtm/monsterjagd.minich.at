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

public class JobManager {

    private final OpServerPlugin plugin;
    private final DataManager dataManager;

    public JobManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        dataManager.ensureDir("jobs");
    }

    public long getRequiredActions(int level) {
        return Math.round(100 * Math.pow(10, (level - 1) * 6.0 / 99.0));
    }

    private String path(UUID uuid) { return "jobs/" + uuid + ".yml"; }
    private YamlConfiguration load(UUID uuid) { return dataManager.loadYaml(path(uuid)); }
    private void save(UUID uuid, YamlConfiguration cfg) { dataManager.saveYaml(cfg, path(uuid)); }

    // All jobs are always active — returns all jobs
    public Set<Job> getJobs(UUID uuid) {
        return EnumSet.allOf(Job.class);
    }

    // Legacy compat
    public Job getJob(UUID uuid) { return null; }

    public int getLevel(UUID uuid, Job job) {
        YamlConfiguration cfg = load(uuid);
        return cfg.getInt("level_" + job.name(), 1);
    }

    public long getActions(UUID uuid, Job job) {
        YamlConfiguration cfg = load(uuid);
        return cfg.getLong("actions_" + job.name(), 0);
    }

    public long getTotalActions(UUID uuid, Job job) {
        YamlConfiguration cfg = load(uuid);
        return cfg.getLong("total_" + job.name(), 0);
    }

    // Coins per action: 2.0 + level * 0.10
    public double getCoinsPerAction(int level) {
        return 2.0 + level * 0.10;
    }

    public void addActions(UUID uuid, Job job, int amount) {
        YamlConfiguration cfg = load(uuid);

        int currentLevel = cfg.getInt("level_" + job.name(), 1);
        if (currentLevel >= 100) {
            // Still give coins at max level
            double coins = getCoinsPerAction(currentLevel) * amount;
            plugin.getEconomyManager().deposit(uuid, coins);
            return;
        }

        long currentActions = cfg.getLong("actions_" + job.name(), 0);
        long totalActions = cfg.getLong("total_" + job.name(), 0);

        // Give coins per action
        double coins = getCoinsPerAction(currentLevel) * amount;
        plugin.getEconomyManager().deposit(uuid, coins);

        currentActions += amount;
        totalActions += amount;

        while (currentLevel < 100) {
            long required = getRequiredActions(currentLevel);
            if (currentActions >= required) {
                currentActions -= required;
                currentLevel++;
                checkAndGiveRewards(uuid, job, currentLevel);
            } else {
                break;
            }
        }

        if (currentLevel >= 100) {
            currentLevel = 100;
            currentActions = 0;
        }

        cfg.set("level_" + job.name(), currentLevel);
        cfg.set("actions_" + job.name(), currentActions);
        cfg.set("total_" + job.name(), totalActions);
        save(uuid, cfg);
    }

    // Legacy compat
    public void addActions(UUID uuid, int amount) {
        // no-op, use addActions(uuid, job, amount) instead
    }

    public void checkAndGiveRewards(UUID uuid, Job job, int newLevel) {
        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            player.sendMessage("§6§l[Beruf] §r§aGlückwunsch! §e" + job.getDisplayName()
                    + " §aLevel §e" + newLevel + " §aerreicht!");
        }

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

            double rewardCoins = entry.getDouble("coins", 0);
            if (rewardCoins > 0) {
                plugin.getEconomyManager().deposit(uuid, rewardCoins);
                if (player != null) {
                    player.sendMessage("§a+§e" + String.format("%.0f", rewardCoins)
                            + " §aCoins als Berufsbelohnung für Level " + newLevel + "!");
                }
            }

            List<String> itemStrings = entry.getStringList("items");
            List<ItemStack> toGive = new ArrayList<>();
            for (String s : itemStrings) {
                String[] parts = s.split(":");
                try {
                    Material mat = Material.valueOf(parts[0].toUpperCase());
                    int amt = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    toGive.add(new ItemStack(mat, amt));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Unknown material in job reward: " + s);
                }
            }

            List<String> customItems = entry.getStringList("custom-items");
            for (String ci : customItems) {
                if (ci.equalsIgnoreCase("GOD_ARMOR_SET")) {
                    for (ItemStack armor : CustomItems.buildGodArmorSet(plugin.getEnchantManager())) {
                        toGive.add(armor);
                    }
                } else {
                    CustomItems customItem = CustomItems.fromString(ci);
                    if (customItem != null) toGive.add(customItem.build(plugin.getEnchantManager()));
                }
            }

            if (player != null && !toGive.isEmpty()) {
                Map<Integer, ItemStack> leftovers = player.getInventory()
                        .addItem(toGive.toArray(new ItemStack[0]));
                leftovers.values().forEach(item ->
                        player.getWorld().dropItem(player.getLocation(), item));
                player.sendMessage("§aGegenstände für Level " + newLevel + " erhalten!");
            }

            String msg = entry.getString("message", null);
            if (msg != null && !msg.isEmpty()) {
                String finalMsg = msg.replace("<player>",
                        player != null ? player.getName() : uuid.toString());
                Bukkit.broadcastMessage(finalMsg);
            }
        }

        if (newLevel >= 100 && player != null) {
            plugin.getTrophyManager().checkAndGiveJobTrophy(player, job);
        }
    }

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
                levelMap.put(uuid, cfg.getInt("level_" + job.name(), 1));
            } catch (IllegalArgumentException ignored) {}
        }

        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(levelMap.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }
}
