package at.minich.opserver.rewards;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.economy.EconomyManager;
import at.minich.opserver.util.DataManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class DailyRewardManager {

    private static final long COOLDOWN_HOURS = 24;

    private final OpServerPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final Random random = new Random();

    // Default loot table – can be expanded via config later
    private static final List<ItemStack> LOOT_TABLE = Arrays.asList(
            new ItemStack(Material.DIAMOND, 1),
            new ItemStack(Material.EMERALD, 3),
            new ItemStack(Material.GOLDEN_APPLE, 1),
            new ItemStack(Material.IRON_INGOT, 8),
            new ItemStack(Material.GOLD_INGOT, 4),
            new ItemStack(Material.ENDER_PEARL, 2),
            new ItemStack(Material.EXPERIENCE_BOTTLE, 5),
            new ItemStack(Material.NETHERITE_SCRAP, 1)
    );

    public DailyRewardManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.economyManager = plugin.getEconomyManager();
    }

    /**
     * Attempts to give the daily reward to the player.
     * Returns true if the reward was given, false if on cooldown.
     */
    public boolean claimDaily(Player player) {
        UUID uuid = player.getUniqueId();
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);

        Instant now = Instant.now();
        String lastClaimStr = cfg.getString("daily.last-claim", null);
        int streak = cfg.getInt("daily.streak", 0);

        if (lastClaimStr != null) {
            Instant lastClaim = Instant.parse(lastClaimStr);
            long hoursSince = ChronoUnit.HOURS.between(lastClaim, now);

            if (hoursSince < COOLDOWN_HOURS) {
                long minutesLeft = ChronoUnit.MINUTES.between(now,
                        lastClaim.plus(COOLDOWN_HOURS, ChronoUnit.HOURS));
                long hoursLeft = minutesLeft / 60;
                long minsLeft = minutesLeft % 60;
                player.sendMessage("§cYou already claimed your daily reward! Come back in §e"
                        + hoursLeft + "h " + minsLeft + "m§c.");
                return false;
            }

            // Reset streak if more than 48h have passed
            if (hoursSince >= 48) {
                streak = 0;
            }
        }

        streak++;
        cfg.set("daily.last-claim", now.toString());
        cfg.set("daily.streak", streak);
        dataManager.saveYaml(cfg, path);

        // Give base coins
        double baseCoins = plugin.getConfig().getDouble("daily-reward.base-coins", 500.0);
        economyManager.deposit(uuid, baseCoins);

        // Give random loot item
        ItemStack reward = LOOT_TABLE.get(random.nextInt(LOOT_TABLE.size()));
        player.getInventory().addItem(reward.clone());

        // Streak bonus on day 7
        double streakBonus = 0;
        if (streak % 7 == 0) {
            streakBonus = plugin.getConfig().getDouble("daily-reward.streak-7-bonus", 2000.0);
            economyManager.deposit(uuid, streakBonus);
        }

        // Notify player
        player.sendMessage("§6§l★ Daily Reward Claimed! §r§7(Day §e" + streak + "§7)");
        player.sendMessage("§a+¢" + String.format("%.0f", baseCoins) + " §7coins");
        player.sendMessage("§a+ §f" + reward.getAmount() + "x " + formatMaterial(reward.getType()));
        if (streakBonus > 0) {
            player.sendMessage("§d§l★ 7-Day Streak Bonus: §r§a+¢" + String.format("%.0f", streakBonus));
        }
        return true;
    }

    /**
     * Returns seconds remaining on the cooldown, or 0 if available.
     */
    public long getCooldownSeconds(UUID uuid) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        String lastClaimStr = cfg.getString("daily.last-claim", null);
        if (lastClaimStr == null) return 0;
        Instant lastClaim = Instant.parse(lastClaimStr);
        Instant available = lastClaim.plus(COOLDOWN_HOURS, ChronoUnit.HOURS);
        long remaining = ChronoUnit.SECONDS.between(Instant.now(), available);
        return Math.max(0, remaining);
    }

    private String formatMaterial(Material mat) {
        String name = mat.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }
}
