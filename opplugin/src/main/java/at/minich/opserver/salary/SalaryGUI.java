package at.minich.opserver.salary;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.ranks.RankManager;
import at.minich.opserver.rewards.DailyRewardManager;
import at.minich.opserver.util.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Opens the Lohn (Salary) GUI for a player.
 */
public class SalaryGUI {

    private final OpServerPlugin plugin;

    public SalaryGUI(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lDein Lohn");

        // Fill with gray stained glass panes
        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        UUID uuid = player.getUniqueId();
        DataManager dm = plugin.getDataManager();
        RankManager rm = plugin.getRankManager();
        DailyRewardManager drm = plugin.getDailyRewardManager();

        // Slot 4 — Player head
        RankManager.RankInfo rank = rm.getRank(uuid);
        String rankName = rank != null ? rank.prefix : "§7[Neuling]";
        double multiplier = rm.getSalaryMultiplier(uuid);
        long playtimeSeconds = getPlaytimeSeconds(uuid, dm, player);
        long hours = playtimeSeconds / 3600;
        long minutes = (playtimeSeconds % 3600) / 60;

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName("§e" + player.getName());
            skullMeta.setLore(Arrays.asList(
                    "§7Rang: §f" + rankName,
                    "§7Spielzeit: §f" + hours + "h " + minutes + "m",
                    "§7Lohn-Multiplikator: §f" + multiplier + "x"
            ));
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(4, skull);

        // Slot 13 — Gold ingot: current salary info
        double salary = plugin.getConfig().getDouble("salary.coins-per-minute", 10.0) * multiplier;
        boolean inFarm = player.getWorld().getName().equals("lohnfarm");
        if (inFarm) salary *= 3.0;

        double totalEarned = getTotalEarned(uuid, dm);
        long cooldownSecs = 60; // salary fires every 60 seconds, simplified countdown
        long cdMins = cooldownSecs / 60;
        long cdSecs2 = cooldownSecs % 60;

        inv.setItem(13, makeItem(Material.GOLD_INGOT, "§6Aktueller Lohn", Arrays.asList(
                "§7Du verdienst §6" + String.format("%.0f", salary) + " Coins§7 pro Minute",
                "§7Nächste Auszahlung in: §f" + cdMins + "m " + cdSecs2 + "s",
                "§7Gesamt verdient: §f" + String.format("%.0f", totalEarned) + " Coins"
        )));

        // Slot 20 — Emerald: bonus rewards
        inv.setItem(20, makeItem(Material.EMERALD, "§aBonus-Belohnungen", Arrays.asList(
                "§7Täglich online: §6+500 Coins",
                "§7Wöchentlich: §6+5000 Coins",
                "§7Streak-Bonus: §6+2000 Coins (7 Tage)"
        )));

        // Slot 22 — Clock: next daily reward
        long dailyCooldown = drm.getCooldownSeconds(uuid);
        String dailyTime;
        if (dailyCooldown <= 0) {
            dailyTime = "§aJetzt verfügbar!";
        } else {
            long dh = dailyCooldown / 3600;
            long dm2 = (dailyCooldown % 3600) / 60;
            dailyTime = dh + "h " + dm2 + "m";
        }
        inv.setItem(22, makeItem(Material.CLOCK, "§eNächste Belohnung", Arrays.asList(
                "§7Zeit bis zur nächsten täglichen Belohnung:",
                "§f" + dailyTime
        )));

        // Slot 24 — Diamond: salary history
        List<String> lastPayouts = getLastPayouts(uuid, dm);
        List<String> historyLore = new java.util.ArrayList<>();
        historyLore.add("§7Letzte Auszahlungen:");
        if (lastPayouts.isEmpty()) {
            historyLore.add("§8Noch keine Auszahlungen.");
        } else {
            for (String s : lastPayouts) {
                historyLore.add("§6+ " + s);
            }
        }
        inv.setItem(24, makeItem(Material.DIAMOND, "§bLohn-Verlauf", historyLore));

        // Slot 40 — Barrier: close
        inv.setItem(40, makeItem(Material.BARRIER, "§cSchließen", Arrays.asList("§7Klicken zum Schließen")));

        player.openInventory(inv);
    }

    // -------------------------------------------------------------------------

    private long getPlaytimeSeconds(UUID uuid, DataManager dm, Player player) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dm.loadYaml(path);
        return cfg.getLong("playtime-seconds", 0);
    }

    private double getTotalEarned(UUID uuid, DataManager dm) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dm.loadYaml(path);
        return cfg.getDouble("salary.total-earned", 0.0);
    }

    private List<String> getLastPayouts(UUID uuid, DataManager dm) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dm.loadYaml(path);
        return cfg.getStringList("salary.last-payouts");
    }

    private ItemStack makeItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
