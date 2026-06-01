package at.minich.opserver.economy;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Applies interest to all bank accounts every hour.
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
