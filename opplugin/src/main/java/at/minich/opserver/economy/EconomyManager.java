package at.minich.opserver.economy;

import at.minich.opserver.util.DataManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * Manages player coin balances. Persists to economy.yml.
 */
public class EconomyManager {

    private static final String FILE = "economy.yml";
    private static final double DEFAULT_BALANCE = 500.0;

    private final DataManager dataManager;
    private final Map<UUID, Double> balances = new HashMap<>();

    public EconomyManager(DataManager dataManager) {
        this.dataManager = dataManager;
        load();
    }

    public void load() {
        balances.clear();
        YamlConfiguration cfg = dataManager.loadYaml(FILE);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                double balance = cfg.getDouble(key, DEFAULT_BALANCE);
                balances.put(uuid, balance);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            cfg.set(entry.getKey().toString(), entry.getValue());
        }
        dataManager.saveYaml(cfg, FILE);
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, DEFAULT_BALANCE);
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, amount));
    }

    public boolean deposit(UUID uuid, double amount) {
        if (amount <= 0) return false;
        balances.put(uuid, getBalance(uuid) + amount);
        return true;
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return false;
        double current = getBalance(uuid);
        if (current < amount) return false;
        balances.put(uuid, current - amount);
        return true;
    }

    public boolean has(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    /** Returns a sorted list (descending) of UUID->balance for top-N display. */
    public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>(balances.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    /** Initialises a balance entry for a new player. */
    public void initPlayer(UUID uuid) {
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, DEFAULT_BALANCE);
        }
    }
}
