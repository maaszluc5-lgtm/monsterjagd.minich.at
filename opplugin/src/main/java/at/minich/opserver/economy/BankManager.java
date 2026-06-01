package at.minich.opserver.economy;

import at.minich.opserver.util.DataManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * Multi-bank manager. Each player can own 1-5 bank slots.
 * Slot 1 is always unlocked. Slots 2-5 cost 250,000 coins to unlock.
 * Persists to bank.yml under players.<uuid>.banks.<slot>.
 */
public class BankManager {

    private static final String FILE = "bank.yml";
    public static final double UNLOCK_COST = 250_000.0;
    public static final int MAX_BANKS = 5;

    private final DataManager dataManager;

    // uuid -> slot (1-5) -> balance
    private final Map<UUID, Map<Integer, Double>> balances = new HashMap<>();
    // uuid -> slot (1-5) -> unlocked
    private final Map<UUID, Map<Integer, Boolean>> unlocked = new HashMap<>();
    // uuid -> active bank slot
    private final Map<UUID, Integer> activeBank = new HashMap<>();
    // uuid -> total interest received (for GUI display)
    private final Map<UUID, Double> totalInterest = new HashMap<>();

    public BankManager(DataManager dataManager) {
        this.dataManager = dataManager;
        load();
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    public void load() {
        balances.clear();
        unlocked.clear();
        activeBank.clear();
        totalInterest.clear();

        YamlConfiguration cfg = dataManager.loadYaml(FILE);
        ConfigurationSection players = cfg.getConfigurationSection("players");
        if (players == null) return;

        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException e) { continue; }

            ConfigurationSection pSec = players.getConfigurationSection(uuidStr);
            if (pSec == null) continue;

            Map<Integer, Double> bal = new HashMap<>();
            Map<Integer, Boolean> unl = new HashMap<>();
            for (int i = 1; i <= MAX_BANKS; i++) {
                bal.put(i, pSec.getDouble("banks." + i + ".balance", 0.0));
                unl.put(i, pSec.getBoolean("banks." + i + ".unlocked", i == 1));
            }
            balances.put(uuid, bal);
            unlocked.put(uuid, unl);
            activeBank.put(uuid, pSec.getInt("active-bank", 1));
            totalInterest.put(uuid, pSec.getDouble("total-interest", 0.0));
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (UUID uuid : balances.keySet()) {
            String base = "players." + uuid;
            Map<Integer, Double> bal = balances.get(uuid);
            Map<Integer, Boolean> unl = unlocked.get(uuid);
            for (int i = 1; i <= MAX_BANKS; i++) {
                cfg.set(base + ".banks." + i + ".balance", bal.getOrDefault(i, 0.0));
                cfg.set(base + ".banks." + i + ".unlocked", unl.getOrDefault(i, i == 1));
            }
            cfg.set(base + ".active-bank", activeBank.getOrDefault(uuid, 1));
            cfg.set(base + ".total-interest", totalInterest.getOrDefault(uuid, 0.0));
        }
        dataManager.saveYaml(cfg, FILE);
    }

    // -------------------------------------------------------------------------
    // Player initialisation
    // -------------------------------------------------------------------------

    public void initPlayer(UUID uuid) {
        balances.computeIfAbsent(uuid, k -> {
            Map<Integer, Double> m = new HashMap<>();
            for (int i = 1; i <= MAX_BANKS; i++) m.put(i, 0.0);
            return m;
        });
        unlocked.computeIfAbsent(uuid, k -> {
            Map<Integer, Boolean> m = new HashMap<>();
            for (int i = 1; i <= MAX_BANKS; i++) m.put(i, i == 1);
            return m;
        });
        activeBank.putIfAbsent(uuid, 1);
        totalInterest.putIfAbsent(uuid, 0.0);
    }

    // -------------------------------------------------------------------------
    // Bank slot methods
    // -------------------------------------------------------------------------

    public boolean isBankUnlocked(UUID uuid, int slot) {
        initPlayer(uuid);
        return unlocked.get(uuid).getOrDefault(slot, slot == 1);
    }

    public void unlockBank(UUID uuid, int slot) {
        initPlayer(uuid);
        unlocked.get(uuid).put(slot, true);
    }

    public double getBankBalance(UUID uuid, int slot) {
        initPlayer(uuid);
        return balances.get(uuid).getOrDefault(slot, 0.0);
    }

    public double getTotalBankBalance(UUID uuid) {
        initPlayer(uuid);
        double total = 0;
        Map<Integer, Boolean> unl = unlocked.get(uuid);
        Map<Integer, Double> bal = balances.get(uuid);
        for (int i = 1; i <= MAX_BANKS; i++) {
            if (unl.getOrDefault(i, i == 1)) {
                total += bal.getOrDefault(i, 0.0);
            }
        }
        return total;
    }

    public boolean depositToBank(UUID uuid, int slot, double amount, EconomyManager economy) {
        if (amount <= 0) return false;
        if (!isBankUnlocked(uuid, slot)) return false;
        if (!economy.has(uuid, amount)) return false;
        economy.withdraw(uuid, amount);
        balances.get(uuid).merge(slot, amount, Double::sum);
        return true;
    }

    public boolean withdrawFromBank(UUID uuid, int slot, double amount, EconomyManager economy) {
        if (amount <= 0) return false;
        if (!isBankUnlocked(uuid, slot)) return false;
        double current = getBankBalance(uuid, slot);
        if (current < amount) return false;
        balances.get(uuid).put(slot, current - amount);
        economy.deposit(uuid, amount);
        return true;
    }

    public void setActiveBank(UUID uuid, int slot) {
        initPlayer(uuid);
        activeBank.put(uuid, slot);
    }

    public int getActiveBank(UUID uuid) {
        initPlayer(uuid);
        return activeBank.getOrDefault(uuid, 1);
    }

    public double getTotalInterest(UUID uuid) {
        initPlayer(uuid);
        return totalInterest.getOrDefault(uuid, 0.0);
    }

    /** Apply interest to all unlocked bank slots for all players. Called hourly. */
    public void applyInterest(double ratePercent) {
        double multiplier = ratePercent / 100.0;
        for (UUID uuid : balances.keySet()) {
            Map<Integer, Double> bal = balances.get(uuid);
            Map<Integer, Boolean> unl = unlocked.get(uuid);
            double totalGained = 0;
            for (int i = 1; i <= MAX_BANKS; i++) {
                if (unl.getOrDefault(i, i == 1)) {
                    double interest = bal.getOrDefault(i, 0.0) * multiplier;
                    bal.merge(i, interest, Double::sum);
                    totalGained += interest;
                }
            }
            totalInterest.merge(uuid, totalGained, Double::sum);
        }
    }

    // -------------------------------------------------------------------------
    // Legacy compatibility (active-bank slot is used for deposit/withdraw)
    // -------------------------------------------------------------------------

    /** Deposit to the active bank slot. */
    public boolean deposit(UUID uuid, double amount, EconomyManager economy) {
        return depositToBank(uuid, getActiveBank(uuid), amount, economy);
    }

    /** Withdraw from the active bank slot. */
    public boolean withdraw(UUID uuid, double amount, EconomyManager economy) {
        return withdrawFromBank(uuid, getActiveBank(uuid), amount, economy);
    }

    /** Balance of the active bank slot. */
    public double getBalance(UUID uuid) {
        return getBankBalance(uuid, getActiveBank(uuid));
    }
}
