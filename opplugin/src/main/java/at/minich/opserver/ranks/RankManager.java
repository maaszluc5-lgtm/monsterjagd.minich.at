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

public class RankManager {

    public static class RankInfo {
        public final String key;
        public final long requiredHours;
        public final double salaryMultiplier;
        public final String prefix;

        public RankInfo(String key, long requiredHours, double salaryMultiplier, String prefix) {
            this.key = key;
            this.requiredHours = requiredHours;
            this.salaryMultiplier = salaryMultiplier;
            this.prefix = prefix;
        }
    }

    private final OpServerPlugin plugin;
    private final DataManager dataManager;
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
            long hours = sec.getLong(key + ".required-hours", 0);
            double multiplier = sec.getDouble(key + ".salary-multiplier", 1.0);
            String prefix = sec.getString(key + ".prefix", "");
            ranks.add(new RankInfo(key, hours, multiplier, prefix));
        }
        ranks.sort(Comparator.comparingLong(r -> r.requiredHours));
    }

    private void setupVaultChat() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Chat> rsp = Bukkit.getServicesManager().getRegistration(Chat.class);
        if (rsp != null) {
            vaultChat = rsp.getProvider();
        }
    }

    /**
     * Returns playtime in hours for a player, reading from their YAML data file.
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

    /**
     * Returns the current rank for a player.
     */
    public RankInfo getRank(UUID uuid) {
        long hours = getPlaytimeHours(uuid);
        RankInfo current = ranks.isEmpty() ? null : ranks.get(0);
        for (RankInfo r : ranks) {
            if (hours >= r.requiredHours) {
                current = r;
            }
        }
        return current;
    }

    /**
     * Returns the next rank after the player's current rank, or null if max rank.
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
     * Returns salary multiplier for a player based on their rank.
     */
    public double getSalaryMultiplier(UUID uuid) {
        RankInfo r = getRank(uuid);
        return r != null ? r.salaryMultiplier : 1.0;
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
                // Vault Chat may not support runtime prefix changes for all backends
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
}
