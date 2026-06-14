package at.minich.opserver.economy;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Applies interest to all bank accounts every hour.
 * Interest is credited to each player's Zinsen-Konto instead of directly
 * to bank balances — players must collect it manually via the Bank GUI.
 */
public class BankInterestTask extends BukkitRunnable {

    private final BankManager bankManager;
    private final EconomyManager economyManager;
    private final double interestRatePercent;

    public BankInterestTask(BankManager bankManager, EconomyManager economyManager, double interestRatePercent) {
        this.bankManager = bankManager;
        this.economyManager = economyManager;
        this.interestRatePercent = interestRatePercent;
    }

    @Override
    public void run() {
        // Calculate total interest to pay out
        double totalInterest = bankManager.calculateTotalInterest(interestRatePercent);

        // Only pay if bank account has enough
        economyManager.initPlayer(ServerAccount.BANK);
        double bankBalance = economyManager.getBalance(ServerAccount.BANK);
        if (bankBalance < totalInterest) return;

        economyManager.withdraw(ServerAccount.BANK, totalInterest);
        bankManager.applyInterest(interestRatePercent);
        bankManager.save();
    }
}
