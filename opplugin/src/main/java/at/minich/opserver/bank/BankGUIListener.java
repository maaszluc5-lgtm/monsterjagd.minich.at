package at.minich.opserver.bank;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.economy.BankManager;
import at.minich.opserver.economy.EconomyManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all click events inside the Bank GUI and chat-input prompts for
 * deposit / withdrawal amounts.
 */
public class BankGUIListener implements Listener {

    private enum InputMode { DEPOSIT, WITHDRAW }

    private final OpServerPlugin plugin;
    private final BankGUI bankGUI;

    /** Players currently waiting to type a deposit or withdrawal amount. */
    private final Map<UUID, InputMode> pendingInput = new HashMap<>();

    public BankGUIListener(OpServerPlugin plugin, BankGUI bankGUI) {
        this.plugin = plugin;
        this.bankGUI = bankGUI;
    }

    // -------------------------------------------------------------------------
    // Inventory click handling
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        if (!BankGUI.GUI_TITLE.equals(inv.getTitle())) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null
                || event.getCurrentItem().getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        BankManager bm = plugin.getBankManager();
        EconomyManager em = plugin.getEconomyManager();
        UUID uuid = player.getUniqueId();
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        // --- Bank slots (0, 2, 4, 6, 8) ---
        for (int i = 0; i < 5; i++) {
            if (slot == i * 2) {
                int bankNum = i + 1;
                if (bm.isBankUnlocked(uuid, bankNum)) {
                    bm.setActiveBank(uuid, bankNum);
                    player.sendMessage(prefix + "§aBank " + bankNum + " ist jetzt deine aktive Bank.");
                    bankGUI.open(player); // refresh
                } else {
                    // Try to unlock
                    if (em.has(uuid, BankManager.UNLOCK_COST)) {
                        em.withdraw(uuid, BankManager.UNLOCK_COST);
                        bm.unlockBank(uuid, bankNum);
                        player.sendMessage(prefix + "§aBank " + bankNum
                                + " wurde freigeschaltet! §7(-§6250.000 Coins§7)");
                        bankGUI.open(player); // refresh
                    } else {
                        player.sendMessage(prefix + "§cNicht genug Coins! Benötigt: §6250.000 Coins§c.");
                    }
                }
                return;
            }
        }

        // --- Einzahlen (slot 22) ---
        if (slot == 22) {
            player.closeInventory();
            pendingInput.put(uuid, InputMode.DEPOSIT);
            player.sendMessage(prefix + "§6Einzahlen §7— Schreibe den Betrag in den Chat:");
            return;
        }

        // --- Auszahlen (slot 23) ---
        if (slot == 23) {
            player.closeInventory();
            pendingInput.put(uuid, InputMode.WITHDRAW);
            player.sendMessage(prefix + "§cAuszahlen §7— Schreibe den Betrag in den Chat:");
            return;
        }

        // --- Schließen (slot 49) ---
        if (slot == 49) {
            player.closeInventory();
        }
    }

    // -------------------------------------------------------------------------
    // Chat input for deposit / withdraw amount
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        InputMode mode = pendingInput.remove(uuid);
        if (mode == null) return;

        event.setCancelled(true);

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
        double amount;
        try {
            amount = Double.parseDouble(event.getMessage().replace(",", "."));
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§cUngültiger Betrag.");
            return;
        }

        if (amount <= 0) {
            player.sendMessage(prefix + "§cBetrag muss größer als 0 sein.");
            return;
        }

        BankManager bm = plugin.getBankManager();
        EconomyManager em = plugin.getEconomyManager();
        int activeSlot = bm.getActiveBank(uuid);

        // Run on the main thread because economy touches non-thread-safe maps
        final double finalAmount = amount;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (mode == InputMode.DEPOSIT) {
                if (bm.depositToBank(uuid, activeSlot, finalAmount, em)) {
                    player.sendMessage(prefix + "§a" + String.format("%.2f", finalAmount)
                            + " Coins §7wurden auf §9Bank " + activeSlot + " §7eingezahlt.");
                } else {
                    player.sendMessage(prefix + "§cEinzahlung fehlgeschlagen. Nicht genug Wallet-Guthaben?");
                }
            } else {
                if (bm.withdrawFromBank(uuid, activeSlot, finalAmount, em)) {
                    player.sendMessage(prefix + "§a" + String.format("%.2f", finalAmount)
                            + " Coins §7wurden von §9Bank " + activeSlot + " §7abgehoben.");
                } else {
                    player.sendMessage(prefix + "§cAuszahlung fehlgeschlagen. Nicht genug Bankguthaben?");
                }
            }
        });
    }
}
