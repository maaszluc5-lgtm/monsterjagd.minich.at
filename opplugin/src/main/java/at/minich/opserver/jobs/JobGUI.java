package at.minich.opserver.jobs;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobGUI {

    public static final String GUI_TITLE = "§6§lBerufe";
    private static final Material GLASS = Material.GRAY_STAINED_GLASS_PANE;

    // All 6 jobs shown — slots 10,12,14,16,19,21
    private static final int[] JOB_SLOTS = {10, 12, 14, 16, 19, 21};
    private static final Job[] JOB_ORDER = {
            Job.GRAEBER, Job.MIENENARBEITER, Job.FARMER,
            Job.FISHER, Job.JAEGER, Job.BUILDER
    };

    private final OpServerPlugin plugin;
    private final JobManager jobManager;

    public JobGUI(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        UUID uuid = player.getUniqueId();

        ItemStack glass = makeGlass();
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        // All jobs shown with their individual levels
        for (int i = 0; i < JOB_ORDER.length; i++) {
            Job job = JOB_ORDER[i];
            int level = jobManager.getLevel(uuid, job);
            long actions = jobManager.getActions(uuid, job);
            inv.setItem(JOB_SLOTS[i], makeJobIcon(job, level, actions));
        }

        // Player stats head (slot 28) — shows total earnings
        inv.setItem(28, makeStatsItem(uuid));

        // Upcoming rewards for each job (slots 36-44)
        int[] rewardSlots = {36, 37, 38, 39, 40, 41};
        for (int i = 0; i < JOB_ORDER.length; i++) {
            Job job = JOB_ORDER[i];
            int level = jobManager.getLevel(uuid, job);
            int nextRewardLevel = nextRewardLevel(level);
            if (nextRewardLevel > 0 && i < rewardSlots.length) {
                inv.setItem(rewardSlots[i], makeRewardItem(job, nextRewardLevel));
            }
        }

        // Coins per action info (slot 49)
        inv.setItem(49, makeEarningsInfo(uuid));

        player.openInventory(inv);
    }

    private ItemStack makeGlass() {
        ItemStack item = new ItemStack(GLASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack makeJobIcon(Job job, int level, long actions) {
        ItemStack item = new ItemStack(job.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(job.getDisplayName() + " §8| §7Level §e" + level);
        List<String> lore = new ArrayList<>();
        lore.add("§7" + job.getDescription());
        lore.add("");
        long required = jobManager.getRequiredActions(level);
        if (level < 100) {
            lore.add("§7Fortschritt: §e" + actions + " §8/ §e" + required);
            lore.add("§7" + buildProgressBar(actions, required));
        } else {
            lore.add("§a§lMAX LEVEL erreicht!");
        }
        lore.add("");
        double coinsPerAction = jobManager.getCoinsPerAction(level);
        lore.add("§7Verdienst pro Aktion: §e" + String.format("%.2f", coinsPerAction) + " Coins");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeStatsItem(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;
        if (p != null) meta.setOwningPlayer(p);
        meta.setDisplayName("§6§l" + (p != null ? p.getName() : "Spieler") + " §7— Statistiken");
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (Job job : JOB_ORDER) {
            int level = jobManager.getLevel(uuid, job);
            lore.add(job.getDisplayName() + " §8» §7Level §e" + level);
        }
        lore.add("");
        lore.add("§7Alle Jobs sind gleichzeitig aktiv!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeEarningsInfo(UUID uuid) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§6Coins pro Aktion");
        List<String> lore = new ArrayList<>();
        lore.add("§7Formel: §e2.00 + Level × 0.10");
        lore.add("");
        for (Job job : JOB_ORDER) {
            int level = jobManager.getLevel(uuid, job);
            double coins = jobManager.getCoinsPerAction(level);
            lore.add(job.getDisplayName() + " §8» §e" + String.format("%.2f", coins) + " Coins");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeRewardItem(Job job, int rewardLevel) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(job.getDisplayName() + " §8| §6Nächste Belohnung: Level " + rewardLevel);
        List<String> lore = new ArrayList<>();
        ConfigurationSection rewardsSec = plugin.getConfig()
                .getConfigurationSection("jobs.rewards." + job.name().toLowerCase());
        if (rewardsSec != null) {
            for (String key : rewardsSec.getKeys(false)) {
                ConfigurationSection entry = rewardsSec.getConfigurationSection(key);
                if (entry == null || entry.getInt("level", -1) != rewardLevel) continue;
                double coins = entry.getDouble("coins", 0);
                if (coins > 0) lore.add("§e+" + String.format("%.0f", coins) + " Coins");
                for (String s : entry.getStringList("items")) {
                    String[] parts = s.split(":");
                    int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    lore.add("§7" + amount + "x " + formatMaterial(parts[0]));
                }
                for (String ci : entry.getStringList("custom-items")) {
                    lore.add("§6★ " + ci.replace("_", " "));
                }
            }
        }
        if (lore.isEmpty()) lore.add("§7Münzen + Items");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int nextRewardLevel(int currentLevel) {
        for (int lvl = 5; lvl <= 100; lvl += 5) {
            if (lvl > currentLevel) return lvl;
        }
        return -1;
    }

    private String buildProgressBar(long current, long max) {
        int totalBars = 20;
        int filled = max > 0 ? (int) Math.round((double) current / max * totalBars) : 0;
        filled = Math.min(filled, totalBars);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < totalBars; i++) sb.append(i < filled ? "§a|" : "§8|");
        sb.append("§7]");
        return sb.toString();
    }

    private String formatMaterial(String mat) {
        String lower = mat.replace("_", " ").toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public static int[] getJobSlots() { return JOB_SLOTS; }
    public static Job[] getJobOrder() { return JOB_ORDER; }
    public static String getGuiTitle() { return GUI_TITLE; }
}
