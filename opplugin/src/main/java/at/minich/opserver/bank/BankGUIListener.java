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

public class BankGUIListener implements Listener {

    private enum InputMode { DEPOSIT, WITHDRAW }

    private final OpServerPlugin plugin;
    private final BankGUI bankGUI;

    private final Map<UUID, InputMode> pendingInput = new HashMap<>();
    // page state per player (for admin paging)
    private final Map<UUID, Integer> playerPage = new HashMap<>();

    public BankGUIListener(OpServerPlugin plugin, BankGUI bankGUI) {
        this.plugin = plugin;
        this.bankGUI = bankGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();
        boolean isPage1 = BankGUI.GUI_TITLE.equals(title);
        boolean isPage2 = BankGUI.GUI_TITLE_P2.equals(title);
        if (!isPage1 && !isPage2) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        int page = isPage2 ? 2 : 1;
        BankManager bm = plugin.getBankManager();
        EconomyManager em = plugin.getEconomyManager();
        UUID uuid = player.getUniqueId();
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
        int maxBanks = bm.getMaxBanks(player);

        // --- Page navigation ---
        if (slot == 45 && page == 2) { bankGUI.open(player, 1); return; }
        if (slot == 53 && page == 1 && maxBanks > 5) { bankGUI.open(player, 2); return; }

        // --- Zinsen-Konto (slot 4, page 1 only) ---
        if (slot == BankGUI.ZINSEN_SLOT && page == 1) {
            double collected = bm.collectZinsen(uuid);
            if (collected > 0) {
                player.sendMessage(prefix + "§6" + fmt(collected)
                        + " Coins §7Zinsen auf §9Bank " + bm.getActiveBank(uuid) + " §7übertragen.");
                bankGUI.open(player, 1);
            } else {
                player.sendMessage(prefix + "§7Keine Zinsen zum Abholen.");
            }
            return;
        }

        // --- Markt-Bank (slot 13) ---
        if (slot == 13) {
            double collected = bm.collectMarktBalance(uuid);
            if (collected > 0) {
                player.sendMessage(prefix + "§6" + fmt(collected)
                        + " Coins §7aus der Markt-Bank auf §9Bank " + bm.getActiveBank(uuid) + " §7übertragen.");
                bankGUI.open(player, page);
            } else {
                player.sendMessage(prefix + "§7Keine Einnahmen in der Markt-Bank.");
            }
            return;
        }

        // --- Bank slots ---
        int banksPerPage = maxBanks <= 5 ? 5 : 10;
        int startBank    = (page - 1) * banksPerPage + 1;
        // slots used for banks on each page
        int[] slotsPage1 = {0, 1, 2, 3, 5, 6, 7, 8, 9, 10};
        int[] slotsPage2 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] bankSlots  = page == 1 ? slotsPage1 : slotsPage2;

        for (int i = 0; i < bankSlots.length; i++) {
            if (slot == bankSlots[i]) {
                int bankNum = startBank + i;
                if (bankNum > maxBanks) break;
                handleBankClick(player, uuid, bankNum, bm, em, prefix, page);
                return;
            }
        }

        // --- Einzahlen (slot 22) ---
        if (slot == 22) {
            player.closeInventory();
            pendingInput.put(uuid, InputMode.DEPOSIT);
            playerPage.put(uuid, page);
            player.sendMessage(prefix + "§6Einzahlen §7— Schreibe den Betrag in den Chat:");
            return;
        }

        // --- Auszahlen (slot 23) ---
        if (slot == 23) {
            player.closeInventory();
            pendingInput.put(uuid, InputMode.WITHDRAW);
            playerPage.put(uuid, page);
            player.sendMessage(prefix + "§cAuszahlen §7— Schreibe den Betrag in den Chat:");
            return;
        }

        // --- Schließen (slot 49) ---
        if (slot == 49) { player.closeInventory(); }
    }

    private void handleBankClick(Player player, UUID uuid, int bankNum,
                                  BankManager bm, EconomyManager em, String prefix, int page) {
        if (bm.isBankUnlocked(uuid, bankNum)) {
            bm.setActiveBank(uuid, bankNum);
            player.sendMessage(prefix + "§aBank " + bankNum + " ist jetzt deine aktive Bank.");
            bankGUI.open(player, page);
        } else {
            if (em.has(uuid, BankManager.UNLOCK_COST)) {
                em.withdraw(uuid, BankManager.UNLOCK_COST);
                bm.unlockBank(uuid, bankNum);
                player.sendMessage(prefix + "§aBank " + bankNum + " freigeschaltet! §7(-§6250.000 Coins§7)");
                bankGUI.open(player, page);
            } else {
                player.sendMessage(prefix + "§cNicht genug Coins! Benötigt: §6250.000 Coins§c.");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        InputMode mode = pendingInput.remove(uuid);
        if (mode == null) return;
        int page = playerPage.getOrDefault(uuid, 1);

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
        final double finalAmount = amount;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (mode == InputMode.DEPOSIT) {
                if (bm.depositToBank(uuid, activeSlot, finalAmount, em)) {
                    player.sendMessage(prefix + "§a" + fmt(finalAmount)
                            + " Coins §7auf §9Bank " + activeSlot + " §7eingezahlt.");
                    double tax = plugin.getBankManager().getLastTax(player.getUniqueId());
                    if (tax > 0) player.sendMessage("§e-20% Steuer: §c-" + String.format("%.2f", tax) + " Coins");
                    bankGUI.open(player, page);
                } else {
                    player.sendMessage(prefix + "§cEinzahlung fehlgeschlagen. Nicht genug Wallet-Guthaben?");
                }
            } else {
                if (bm.withdrawFromBank(uuid, activeSlot, finalAmount, em)) {
                    player.sendMessage(prefix + "§a" + fmt(finalAmount)
                            + " Coins §7von §9Bank " + activeSlot + " §7abgehoben.");
                    bankGUI.open(player, page);
                } else {
                    player.sendMessage(prefix + "§cAuszahlung fehlgeschlagen. Nicht genug Bankguthaben?");
                }
            }
        });
    }

    private String fmt(double v) { return String.format("%.2f", v); }
}
