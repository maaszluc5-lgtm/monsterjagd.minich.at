package at.minich.opserver.bank;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.economy.BankManager;
import at.minich.opserver.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Opens the multi-bank GUI for a player.
 * Title: §9§lDeine Banken (54 slots)
 *
 * Layout (top row):
 *   Slot 0 → Bank 1
 *   Slot 2 → Bank 2
 *   Slot 4 → Zinsen-Konto (gold block, center)
 *   Slot 6 → Bank 3
 *   Slot 8 → Bank 4
 *
 * Bank 5 is accessible but displayed in the overview / still switchable via
 * the active-bank concept; the GUI shows only 4 banks in the top row to make
 * room for the Zinsen-Konto.  Banks 1-4 occupy slots 0,2,6,8; Bank 5 is shown
 * in the Konto-Übersicht and players can still activate it by command.
 */
public class BankGUI {

    public static final String GUI_TITLE = "§9§lDeine Banken";

    /**
     * GUI slots for banks 1-4 (bank 5 is not shown as a top-row button
     * to keep the Zinsen-Konto centred at slot 4).
     * Index 0 → bank 1, index 1 → bank 2, index 2 → bank 3, index 3 → bank 4.
     */
    private static final int[] BANK_SLOTS = {0, 2, 6, 8};

    /** Inventory slot for the Zinsen-Konto. */
    public static final int ZINSEN_SLOT = 4;

    private final OpServerPlugin plugin;

    public BankGUI(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Fill with light blue stained glass panes
        ItemStack filler = makeItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        UUID uuid = player.getUniqueId();
        BankManager bm = plugin.getBankManager();
        EconomyManager em = plugin.getEconomyManager();

        // --- Banks 1-4 in slots 0,2,6,8 ---
        int activeSlot = bm.getActiveBank(uuid);
        for (int i = 0; i < 4; i++) {
            int bankNum = i + 1;
            int guiSlot = BANK_SLOTS[i];
            boolean unlocked = bm.isBankUnlocked(uuid, bankNum);
            double balance = bm.getBankBalance(uuid, bankNum);

            if (unlocked) {
                List<String> lore = new ArrayList<>();
                lore.add("§7Guthaben: §a" + String.format("%.2f", balance) + " Coins");
                if (bankNum == activeSlot) lore.add("§7(Aktiv)");
                else lore.add("§7Klicken zum Aktivieren");
                inv.setItem(guiSlot, makeItem(Material.BLUE_STAINED_GLASS, "§9Bank " + bankNum, lore));
            } else {
                inv.setItem(guiSlot, makeItem(Material.RED_STAINED_GLASS,
                        "§cBank " + bankNum + " §7[Gesperrt]",
                        Arrays.asList(
                                "§7Freischalten für §6250.000 Coins",
                                "§7Klicken zum Freischalten"
                        )));
            }
        }

        // --- Bank 5 shown separately at slot 9 (second row, leftmost) ---
        {
            int bankNum = 5;
            boolean unlocked = bm.isBankUnlocked(uuid, bankNum);
            double balance = bm.getBankBalance(uuid, bankNum);
            if (unlocked) {
                List<String> lore = new ArrayList<>();
                lore.add("§7Guthaben: §a" + String.format("%.2f", balance) + " Coins");
                if (bankNum == activeSlot) lore.add("§7(Aktiv)");
                else lore.add("§7Klicken zum Aktivieren");
                inv.setItem(9, makeItem(Material.BLUE_STAINED_GLASS, "§9Bank 5", lore));
            } else {
                inv.setItem(9, makeItem(Material.RED_STAINED_GLASS,
                        "§cBank 5 §7[Gesperrt]",
                        Arrays.asList(
                                "§7Freischalten für §6250.000 Coins",
                                "§7Klicken zum Freischalten"
                        )));
            }
        }

        // --- Slot 4 (centre of top row): Zinsen-Konto ---
        double zinsenBalance = bm.getZinsen(uuid);
        inv.setItem(ZINSEN_SLOT, makeItem(Material.GOLD_BLOCK,
                "§6§lZinsen-Konto",
                Arrays.asList(
                        "§7Gesammelte Zinsen: §6" + String.format("%.2f", zinsenBalance) + " Coins",
                        "§aLinksklick: Abholen"
                )));

        // --- Slot 22: Einzahlen ---
        inv.setItem(22, makeItem(Material.EMERALD_BLOCK, "§6Einzahlen",
                Arrays.asList("§7Linksklick: Betrag eingeben")));

        // --- Slot 23: Auszahlen ---
        inv.setItem(23, makeItem(Material.RED_CONCRETE, "§cAuszahlen",
                Arrays.asList("§7Linksklick: Betrag eingeben")));

        // --- Slot 40: Konto-Übersicht ---
        List<String> overviewLore = new ArrayList<>();
        double total = 0;
        for (int i = 1; i <= BankManager.MAX_BANKS; i++) {
            if (bm.isBankUnlocked(uuid, i)) {
                double b = bm.getBankBalance(uuid, i);
                total += b;
                overviewLore.add("§7Bank " + i + ": §a" + String.format("%.2f", b) + " Coins"
                        + (i == activeSlot ? " §e(Aktiv)" : ""));
            } else {
                overviewLore.add("§7Bank " + i + ": §8[Gesperrt]");
            }
        }
        overviewLore.add("§7Zinsen-Konto: §6" + String.format("%.2f", zinsenBalance) + " Coins");
        overviewLore.add("§7Gesamt (Banken): §a" + String.format("%.2f", total) + " Coins");
        inv.setItem(40, makeItem(Material.PAPER, "§eKonto-Übersicht", overviewLore));

        // --- Slot 49: Schließen ---
        inv.setItem(49, makeItem(Material.BARRIER, "§cSchließen",
                Arrays.asList("§7Klicken zum Schließen")));

        player.openInventory(inv);
    }

    // -------------------------------------------------------------------------

    private ItemStack makeItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
