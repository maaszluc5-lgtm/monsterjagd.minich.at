package at.minich.opserver.ranks;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.util.DataManager;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

/**
 * Purchase-based rank system.
 * Ranks are defined in config.yml under "ranks" with a "price" (in Coins).
 * Players can only move up (purchase next rank), never downgrade.
 * Rank is persisted to each player's YAML data file under the "rank" key.
 */
public class RankManager {

    public static class RankInfo {
        public final String key;
        public final double price;
        public final double salaryMultiplier;
        public final String prefix;

        public RankInfo(String key, double price, double salaryMultiplier, String prefix) {
            this.key = key;
            this.price = price;
            this.salaryMultiplier = salaryMultiplier;
            this.prefix = prefix;
        }
    }

    private final OpServerPlugin plugin;
    private final DataManager dataManager;
    /** Ordered list of ranks from cheapest (index 0) to most expensive. */
    private final List<RankInfo> ranks = new ArrayList<>();
    private Chat vaultChat;

    public RankManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        loadRanks();
        setupVaultChat();
    }

    private void loadRanks() {
        ranks.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("ranks");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            double price = sec.getDouble(key + ".price", 0);
            double multiplier = sec.getDouble(key + ".salary-multiplier", 1.0);
            String prefix = sec.getString(key + ".prefix", "");
            ranks.add(new RankInfo(key, price, multiplier, prefix));
        }
        ranks.sort(Comparator.comparingDouble(r -> r.price));
    }

    private void setupVaultChat() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Chat> rsp = Bukkit.getServicesManager().getRegistration(Chat.class);
        if (rsp != null) {
            vaultChat = rsp.getProvider();
        }
    }

    // -------------------------------------------------------------------------
    // Rank persistence
    // -------------------------------------------------------------------------

    /**
     * Returns the current rank for a player (read from player YAML "rank" field).
     * Defaults to the cheapest rank (index 0) if not set.
     */
    public RankInfo getRank(UUID uuid) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        String key = cfg.getString("rank", ranks.isEmpty() ? null : ranks.get(0).key);
        if (key == null) return ranks.isEmpty() ? null : ranks.get(0);
        for (RankInfo r : ranks) {
            if (r.key.equalsIgnoreCase(key)) return r;
        }
        return ranks.isEmpty() ? null : ranks.get(0);
    }

    /**
     * Saves a player's rank to their YAML data file.
     */
    public void setRank(UUID uuid, RankInfo rank) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        cfg.set("rank", rank.key);
        dataManager.saveYaml(cfg, path);
    }

    // -------------------------------------------------------------------------
    // Rank purchasing
    // -------------------------------------------------------------------------

    /**
     * Returns the next purchasable rank above the player's current rank,
     * or null if the player is already at the top rank.
     */
    public RankInfo getNextRank(UUID uuid) {
        RankInfo current = getRank(uuid);
        if (current == null) return null;
        boolean found = false;
        for (RankInfo r : ranks) {
            if (found) return r;
            if (r.key.equals(current.key)) found = true;
        }
        return null;
    }

    /**
     * Attempts to purchase the next rank up for the given player.
     * Deducts coins via EconomyManager. Players can only go up.
     *
     * @return true on success, false if insufficient funds or already max rank
     */
    public boolean purchaseRank(Player player) {
        UUID uuid = player.getUniqueId();
        RankInfo next = getNextRank(uuid);
        if (next == null) return false; // already max rank

        double balance = plugin.getEconomyManager().getBalance(uuid);
        if (balance < next.price) return false;

        plugin.getEconomyManager().withdraw(uuid, next.price);
        setRank(uuid, next);
        applyRankPrefix(player);
        return true;
    }

    // -------------------------------------------------------------------------
    // Salary & display
    // -------------------------------------------------------------------------

    /**
     * Returns salary multiplier for a player based on their rank.
     */
    public double getSalaryMultiplier(UUID uuid) {
        RankInfo r = getRank(uuid);
        return r != null ? r.salaryMultiplier : 1.0;
    }

    /**
     * Returns the prefix string for a player's current rank.
     */
    public String getPrefix(UUID uuid) {
        RankInfo r = getRank(uuid);
        return r != null ? r.prefix : "";
    }

    /**
     * Applies rank prefix to a player (via Vault Chat or display name fallback).
     */
    public void applyRankPrefix(Player player) {
        RankInfo r = getRank(player.getUniqueId());
        if (r == null) return;
        String prefix = r.prefix;
        if (vaultChat != null) {
            try {
                vaultChat.setPlayerPrefix(player, prefix + " ");
            } catch (Exception e) {
                applyDisplayNameFallback(player, prefix);
            }
        } else {
            applyDisplayNameFallback(player, prefix);
        }
    }

    private void applyDisplayNameFallback(Player player, String prefix) {
        player.setDisplayName(prefix + " " + player.getName() + "§r");
        player.setPlayerListName(prefix + " " + player.getName() + "§r");
    }

    public List<RankInfo> getRanks() {
        return Collections.unmodifiableList(ranks);
    }

    // -------------------------------------------------------------------------
    // Playtime is still tracked for stats purposes but no longer affects rank
    // -------------------------------------------------------------------------

    /**
     * Returns playtime in hours for a player, reading from their YAML data file.
     * Kept for backwards compatibility with StatsCommand etc.
     */
    public long getPlaytimeHours(UUID uuid) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        long seconds = cfg.getLong("playtime-seconds", 0);
        return seconds / 3600;
    }

    /**
     * Increments a player's playtime in seconds.
     */
    public void addPlaytimeSeconds(UUID uuid, long seconds) {
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        long current = cfg.getLong("playtime-seconds", 0);
        cfg.set("playtime-seconds", current + seconds);
        dataManager.saveYaml(cfg, path);
    }

    public int getRankIndex(RankInfo rank) {
        if (rank == null) return 0;
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).key.equalsIgnoreCase(rank.key)) return i;
        }
        return 0;
    }

    public List<RankInfo> getRanks() {
        return Collections.unmodifiableList(ranks);
    }
}
