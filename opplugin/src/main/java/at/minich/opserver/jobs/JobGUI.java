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

/**
 * Builds and opens the 54-slot Jobs GUI.
 *
 * Layout (slot indices, 0-based):
 *   Row 0 (slots 0-8):   decorative glass pane
 *   Row 1 (slots 9-17):  glass | GRAEBER(10) | glass | MIENENARBEITER(12) | glass | FARMER(14) | glass | FISHER(16) | glass
 *   Row 2 (slots 18-26): glass | JAEGER(19) | glass | BUILDER(21) | ... | glass
 *   Row 3 (slots 27-35): current job stats
 *   Row 4 (slots 36-44): next upcoming rewards
 *   Row 5 (slots 45-53): glass ... close button (49)
 */
public class JobGUI {

    private static final String GUI_TITLE = "§6§lBerufe";
    private static final Material GLASS = Material.GRAY_STAINED_GLASS_PANE;

    // Job icon slots
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
        Job currentJob = jobManager.getJob(uuid);
        int currentLevel = jobManager.getLevel(uuid);
        long currentActions = jobManager.getActions(uuid);
        long totalActions = jobManager.getTotalActions(uuid);

        // Fill all slots with glass pane by default
        ItemStack glass = makeGlass();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // ---- Row 1 & 2: Job icons ----
        for (int i = 0; i < JOB_ORDER.length; i++) {
            Job job = JOB_ORDER[i];
            inv.setItem(JOB_SLOTS[i], makeJobIcon(job, currentJob, currentLevel, currentActions));
        }

        // ---- Row 3: Current job stats (slots 27-35) ----
        if (currentJob != null) {
            inv.setItem(28, makeStatsItem(uuid, currentJob, currentLevel, currentActions, totalActions));
        } else {
            inv.setItem(28, makeNoJobItem());
        }

        // ---- Row 4: Next 3 rewards (slots 37,40,43) ----
        if (currentJob != null) {
            List<int[]> upcoming = getUpcomingRewards(currentJob, currentLevel);
            int[] rewardSlots = {37, 40, 43};
            for (int i = 0; i < Math.min(3, upcoming.size()); i++) {
                inv.setItem(rewardSlots[i], makeRewardItem(currentJob, upcoming.get(i)[0]));
            }
        }

        // ---- Row 5: Close button (slot 49) ----
        inv.setItem(49, makeCloseButton());

        player.openInventory(inv);
    }

    // -------------------------------------------------------------------------
    // Icon builders
    // -------------------------------------------------------------------------

    private ItemStack makeGlass() {
        ItemStack item = new ItemStack(GLASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeJobIcon(Job job, Job currentJob, int currentLevel, long currentActions) {
        ItemStack item = new ItemStack(job.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(job.getDisplayName());
        List<String> lore = new ArrayList<>();
        lore.add("§7" + job.getDescription());
        lore.add("");

        if (job == currentJob) {
            lore.add("§aAktiver Beruf");
            lore.add("§7Level: §e" + currentLevel);
            long required = jobManager.getRequiredActions(currentLevel);
            lore.add("§7Fortschritt: §e" + currentActions + " §7/ §e" + required);
            lore.add("§7" + buildProgressBar(currentActions, required));
        } else {
            lore.add("§7Klicken zum Wählen");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeStatsItem(UUID uuid, Job job, int level, long actions, long total) {
        Player p = Bukkit.getPlayer(uuid);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        if (p != null) meta.setOwningPlayer(p);
        meta.setDisplayName("§6§l" + (p != null ? p.getName() : "Spieler"));
        List<String> lore = new ArrayList<>();
        lore.add("§7Beruf: " + job.getDisplayName());
        lore.add("§7Level: §e" + level + " §8/ §e100");
        long required = level < 100 ? jobManager.getRequiredActions(level) : 0;
        if (level < 100) {
            lore.add("§7Fortschritt: §e" + actions + " §8/ §e" + required);
            lore.add("§7" + buildProgressBar(actions, required));
        } else {
            lore.add("§a§lMAX LEVEL!");
        }
        lore.add("§7Gesamt-Aktionen: §e" + total);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeNoJobItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cKein Beruf gewählt");
            List<String> lore = new ArrayList<>();
            lore.add("§7Klicke auf einen Beruf, um ihn zu wählen.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeRewardItem(Job job, int rewardLevel) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName("§6Belohnung bei Level " + rewardLevel);
        List<String> lore = new ArrayList<>();

        ConfigurationSection rewardsSec = plugin.getConfig()
                .getConfigurationSection("jobs.rewards." + job.name().toLowerCase());
        if (rewardsSec != null) {
            for (String key : rewardsSec.getKeys(false)) {
                ConfigurationSection entry = rewardsSec.getConfigurationSection(key);
                if (entry == null) continue;
                if (entry.getInt("level", -1) != rewardLevel) continue;
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
                String msg = entry.getString("message", null);
                if (msg != null && !msg.isEmpty()) lore.add("§b[Broadcast]");
            }
        }

        if (lore.isEmpty()) lore.add("§7Keine Belohnungsdaten gefunden.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cSchließen");
            item.setItemMeta(meta);
        }
        return item;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildProgressBar(long current, long max) {
        int totalBars = 20;
        int filled = max > 0 ? (int) Math.round((double) current / max * totalBars) : 0;
        filled = Math.min(filled, totalBars);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < totalBars; i++) {
            sb.append(i < filled ? "§a|" : "§8|");
        }
        sb.append("§7]");
        return sb.toString();
    }

    /** Returns list of [level] for the next up-to-3 reward levels. */
    private List<int[]> getUpcomingRewards(Job job, int currentLevel) {
        List<int[]> result = new ArrayList<>();
        for (int lvl = 5; lvl <= 100; lvl += 5) {
            if (lvl > currentLevel) {
                result.add(new int[]{lvl});
                if (result.size() >= 3) break;
            }
        }
        return result;
    }

    private String formatMaterial(String mat) {
        return mat.replace("_", " ").toLowerCase()
                .substring(0, 1).toUpperCase()
                + mat.replace("_", " ").toLowerCase().substring(1);
    }

    // -------------------------------------------------------------------------
    // Public helpers used by listener
    // -------------------------------------------------------------------------

    public static String getGuiTitle() {
        return GUI_TITLE;
    }

    public static int[] getJobSlots() {
        return JOB_SLOTS;
    }

    public static Job[] getJobOrder() {
        return JOB_ORDER;
    }
}
