package at.minich.opserver.economy;

import at.minich.opserver.util.DataManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * Manages player bank balances. Persists to bank.yml.
 * Interest is applied by BankInterestTask.
 */
public class BankManager {

    private static final String FILE = "bank.yml";

    private final DataManager dataManager;
    private final Map<UUID, Double> bankBalances = new HashMap<>();

    public BankManager(DataManager dataManager) {
        this.dataManager = dataManager;
        load();
    }

    public void load() {
        bankBalances.clear();
        YamlConfiguration cfg = dataManager.loadYaml(FILE);
        for (String key : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                bankBalances.put(uuid, cfg.getDouble(key, 0.0));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : bankBalances.entrySet()) {
            cfg.set(entry.getKey().toString(), entry.getValue());
        }
        dataManager.saveYaml(cfg, FILE);
    }

    public double getBalance(UUID uuid) {
        return bankBalances.getOrDefault(uuid, 0.0);
    }

    public void setBalance(UUID uuid, double amount) {
        bankBalances.put(uuid, Math.max(0, amount));
    }

    /** Deposit coins from wallet into bank. Returns false on bad amount. */
    public boolean deposit(UUID uuid, double amount, EconomyManager economy) {
        if (amount <= 0) return false;
        if (!economy.has(uuid, amount)) return false;
        economy.withdraw(uuid, amount);
        bankBalances.put(uuid, getBalance(uuid) + amount);
        return true;
    }

    /** Withdraw coins from bank to wallet. Returns false on insufficient funds. */
    public boolean withdraw(UUID uuid, double amount, EconomyManager economy) {
        if (amount <= 0) return false;
        double current = getBalance(uuid);
        if (current < amount) return false;
        bankBalances.put(uuid, current - amount);
        economy.deposit(uuid, amount);
        return true;
    }

    /** Apply interest to all accounts. Called hourly. */
    public void applyInterest(double ratePercent) {
        double multiplier = ratePercent / 100.0;
        for (Map.Entry<UUID, Double> entry : bankBalances.entrySet()) {
            double interest = entry.getValue() * multiplier;
            entry.setValue(entry.getValue() + interest);
        }
    }

    public void initPlayer(UUID uuid) {
        bankBalances.putIfAbsent(uuid, 0.0);
    }
}
