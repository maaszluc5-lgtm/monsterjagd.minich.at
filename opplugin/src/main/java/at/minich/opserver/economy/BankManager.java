package at.minich.opserver.economy;

import at.minich.opserver.util.DataManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * Multi-bank manager. Each player can own 1-5 bank slots plus one special
 * Zinsen-Konto that accumulates interest from all banks.
 * Slot 1-5 are regular banks. The Zinsen-Konto is separate (not a numbered slot).
 * Persists to bank.yml under players.<uuid>.
 */
public class BankManager {

    private static final String FILE = "bank.yml";
    public static final double UNLOCK_COST = 250_000.0;
    public static final int MAX_BANKS = 5;
    public static final int MAX_BANKS_ADMIN = 20;

    private final DataManager dataManager;

    // uuid -> slot (1-5) -> balance
    private final Map<UUID, Map<Integer, Double>> balances = new HashMap<>();
    // uuid -> slot (1-5) -> unlocked
    private final Map<UUID, Map<Integer, Boolean>> unlocked = new HashMap<>();
    // uuid -> active bank slot
    private final Map<UUID, Integer> activeBank = new HashMap<>();
    // uuid -> accumulated interest (Zinsen-Konto)
    private final Map<UUID, Double> zinsen = new HashMap<>();
    // uuid -> markt bank balance
    private final Map<UUID, Double> marktBalance = new HashMap<>();
    private final Map<UUID, Double> lastTax = new HashMap<>();

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
        zinsen.clear();
        marktBalance.clear();

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
            for (int i = 1; i <= MAX_BANKS_ADMIN; i++) {
                bal.put(i, pSec.getDouble("banks." + i + ".balance", 0.0));
                unl.put(i, pSec.getBoolean("banks." + i + ".unlocked", i == 1));
            }
            balances.put(uuid, bal);
            unlocked.put(uuid, unl);
            activeBank.put(uuid, pSec.getInt("active-bank", 1));
            zinsen.put(uuid, pSec.getDouble("zinsen", 0.0));
            marktBalance.put(uuid, pSec.getDouble("markt-balance", 0.0));
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (UUID uuid : balances.keySet()) {
            String base = "players." + uuid;
            Map<Integer, Double> bal = balances.get(uuid);
            Map<Integer, Boolean> unl = unlocked.get(uuid);
            for (int i = 1; i <= MAX_BANKS_ADMIN; i++) {
                cfg.set(base + ".banks." + i + ".balance", bal.getOrDefault(i, 0.0));
                cfg.set(base + ".banks." + i + ".unlocked", unl.getOrDefault(i, i == 1));
            }
            cfg.set(base + ".active-bank", activeBank.getOrDefault(uuid, 1));
            cfg.set(base + ".zinsen", zinsen.getOrDefault(uuid, 0.0));
            cfg.set(base + ".markt-balance", marktBalance.getOrDefault(uuid, 0.0));
        }
        dataManager.saveYaml(cfg, FILE);
    }

    // -------------------------------------------------------------------------
    // Player initialisation
    // -------------------------------------------------------------------------

    public void initPlayer(UUID uuid) {
        balances.computeIfAbsent(uuid, k -> {
            Map<Integer, Double> m = new HashMap<>();
            for (int i = 1; i <= MAX_BANKS_ADMIN; i++) m.put(i, 0.0);
            return m;
        });
        unlocked.computeIfAbsent(uuid, k -> {
            Map<Integer, Boolean> m = new HashMap<>();
            for (int i = 1; i <= MAX_BANKS_ADMIN; i++) m.put(i, i == 1);
            return m;
        });
        activeBank.putIfAbsent(uuid, 1);
        zinsen.putIfAbsent(uuid, 0.0);
        marktBalance.putIfAbsent(uuid, 0.0);
    }

    // -------------------------------------------------------------------------
    // Markt-Bank
    // -------------------------------------------------------------------------

    public double getMarktBalance(UUID uuid) {
        initPlayer(uuid);
        return marktBalance.getOrDefault(uuid, 0.0);
    }

    public void addMarktBalance(UUID uuid, double amount) {
        initPlayer(uuid);
        marktBalance.merge(uuid, amount, Double::sum);
    }

    /**
     * Withdraws (subtracts) from the markt balance without transferring to a bank.
     * Used by ChestShop to pay a buyer when they sell an item back to the shop.
     * Returns true if the balance was sufficient and the withdrawal succeeded.
     */
    public boolean withdrawMarktBalance(UUID uuid, double amount) {
        initPlayer(uuid);
        double current = marktBalance.getOrDefault(uuid, 0.0);
        if (current < amount) return false;
        marktBalance.put(uuid, current - amount);
        return true;
    }

    /** Transfers markt balance to the player's active bank. Returns amount transferred. */
    public double collectMarktBalance(UUID uuid) {
        initPlayer(uuid);
        double amount = marktBalance.getOrDefault(uuid, 0.0);
        if (amount <= 0) return 0.0;
        int slot = getActiveBank(uuid);
        if (!isBankUnlocked(uuid, slot)) return 0.0;
        balances.get(uuid).merge(slot, amount, Double::sum);
        marktBalance.put(uuid, 0.0);
        return amount;
    }

    // -------------------------------------------------------------------------
    // Bank slot methods
    // -------------------------------------------------------------------------

    /** Returns the maximum number of banks a player can unlock (20 for OP, 5 for others). */
    public int getMaxBanks(org.bukkit.entity.Player player) {
        return player.isOp() ? MAX_BANKS_ADMIN : MAX_BANKS;
    }

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
        double tax = amount * 0.2;
        double net = amount - tax;
        lastTax.put(uuid, tax);
        economy.initPlayer(ServerAccount.BANK);
        economy.deposit(ServerAccount.BANK, tax);
        balances.get(uuid).merge(slot, net, Double::sum);
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

    // -------------------------------------------------------------------------
    // Zinsen-Konto methods
    // -------------------------------------------------------------------------

    /**
     * Returns the current Zinsen-Konto balance for a player.
     */
    public double getZinsen(UUID uuid) {
        initPlayer(uuid);
        return zinsen.getOrDefault(uuid, 0.0);
    }

    /**
     * Adds interest to the Zinsen-Konto (called by BankInterestTask).
     */
    public void addZinsen(UUID uuid, double amount) {
        initPlayer(uuid);
        zinsen.merge(uuid, amount, Double::sum);
    }

    /**
     * Moves all Zinsen to the player's active bank slot.
     * Returns the amount collected, or 0.0 if nothing to collect.
     */
    public double collectZinsen(UUID uuid) {
        initPlayer(uuid);
        double amount = zinsen.getOrDefault(uuid, 0.0);
        if (amount <= 0) return 0.0;
        int slot = getActiveBank(uuid);
        if (!isBankUnlocked(uuid, slot)) return 0.0;
        balances.get(uuid).merge(slot, amount, Double::sum);
        zinsen.put(uuid, 0.0);
        return amount;
    }

    /**
     * Apply interest to all unlocked bank slots for all players,
     * routing all interest to the Zinsen-Konto. Called hourly.
     */
    public double calculateTotalInterest(double ratePercent) {
        double multiplier = ratePercent / 100.0;
        double total = 0;
        for (UUID uuid : balances.keySet()) {
            Map<Integer, Double> bal = balances.get(uuid);
            Map<Integer, Boolean> unl = unlocked.get(uuid);
            for (int i = 1; i <= MAX_BANKS; i++) {
                if (unl.getOrDefault(i, i == 1)) {
                    total += bal.getOrDefault(i, 0.0) * multiplier;
                }
            }
        }
        return total;
    }

    public void applyInterest(double ratePercent) {
        double multiplier = ratePercent / 100.0;
        for (UUID uuid : balances.keySet()) {
            Map<Integer, Double> bal = balances.get(uuid);
            Map<Integer, Boolean> unl = unlocked.get(uuid);
            double totalGained = 0;
            for (int i = 1; i <= MAX_BANKS; i++) {
                if (unl.getOrDefault(i, i == 1)) {
                    double interest = bal.getOrDefault(i, 0.0) * multiplier;
                    totalGained += interest;
                }
            }
            if (totalGained > 0) {
                addZinsen(uuid, totalGained);
            }
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

    /**
     * @deprecated Use {@link #getZinsen(UUID)} instead.
     */
    @Deprecated
    public double getTotalInterest(UUID uuid) {
        return getZinsen(uuid);
    }

    public double getLastTax(UUID uuid) {
        return lastTax.getOrDefault(uuid, 0.0);
    }
}
