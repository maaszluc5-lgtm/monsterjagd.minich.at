package at.minich.opserver.economy;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Applies interest to all bank accounts every hour.
 * Interest is credited to each player's Zinsen-Konto instead of directly
 * to bank balances — players must collect it manually via the Bank GUI.
 */
public class BankInterestTask extends BukkitRunnable {

    private final BankManager bankManager;
    private final double interestRatePercent;

    public BankInterestTask(BankManager bankManager, double interestRatePercent) {
        this.bankManager = bankManager;
        this.interestRatePercent = interestRatePercent;
    }

    @Override
    public void run() {
        bankManager.applyInterest(interestRatePercent);
        bankManager.save();
    }
}
