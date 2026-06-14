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
 * Multi-bank GUI.
 * Normal players: 5 banks (54-slot GUI, rows 1-2)
 * Admin/OP: 20 banks (double-page or scrollable via page system)
 * Markt-Bank always shown as a special gold slot.
 */
public class BankGUI {

    public static final String GUI_TITLE       = "§9§lDeine Banken";
    public static final String GUI_TITLE_P2    = "§9§lDeine Banken §7(Seite 2)";
    public static final int    ZINSEN_SLOT     = 4;

    private final OpServerPlugin plugin;

    public BankGUI(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 1);
    }

    public void open(Player player, int page) {
        int maxBanks = plugin.getBankManager().getMaxBanks(player);
        // Page 1 shows banks 1-10 (or 1-5 for normal players), page 2 shows 11-20
        int banksPerPage = maxBanks <= 5 ? 5 : 10;
        int startBank = (page - 1) * banksPerPage + 1;
        int endBank   = Math.min(startBank + banksPerPage - 1, maxBanks);

        String title = page == 1 ? GUI_TITLE : GUI_TITLE_P2;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack filler = makeItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        UUID uuid = player.getUniqueId();
        BankManager bm = plugin.getBankManager();
        int activeSlot = bm.getActiveBank(uuid);

        // Row 0 (slots 0-8): banks in this page — up to 8 per row with Zinsen at slot 4
        // For page 1: slots 0,1,2,3 | Zinsen=4 | slots 5,6,7,8  → up to 8 banks
        // We lay banks in slots: 0,1,2,3,5,6,7,8 (skip slot 4 for Zinsen on page 1)
        int[] bankGuiSlots = page == 1
                ? new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 10}   // 10 slots available
                : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};    // all 10 for page 2

        int slotIdx = 0;
        for (int bankNum = startBank; bankNum <= endBank && slotIdx < bankGuiSlots.length; bankNum++, slotIdx++) {
            int guiSlot = bankGuiSlots[slotIdx];
            renderBank(inv, uuid, bm, bankNum, guiSlot, activeSlot);
        }

        // Zinsen-Konto (page 1 only, slot 4)
        if (page == 1) {
            double zinsenBalance = bm.getZinsen(uuid);
            inv.setItem(ZINSEN_SLOT, makeItem(Material.GOLD_BLOCK,
                    "§6§lZinsen-Konto",
                    Arrays.asList(
                            "§7Gesammelte Zinsen: §6" + fmt(zinsenBalance) + " Coins",
                            "§aLinksklick: Abholen"
                    )));
        }

        // Markt-Bank — persönliche Verkaufseinnahmen (slot 13)
        double marktBal = bm.getMarktBalance(uuid);
        inv.setItem(13, makeItem(Material.ORANGE_STAINED_GLASS,
                "§6§l🏪 Markt-Bank",
                Arrays.asList(
                        "§7Einnahmen aus §6/shop §7Verkäufen",
                        "§7Guthaben: §a" + fmt(marktBal) + " Coins",
                        "",
                        "§aLinksklick: Auf aktive Bank übertragen"
                )));

        // Shop-Einnahmen (nur für Admins/OPs, slot 14)
        if (player.isOp() || player.hasPermission("opserver.admin")) {
            double shopEarnings = bm.getShopEarnings();
            inv.setItem(14, makeItem(Material.GOLD_INGOT,
                    "§e§l💰 Shop-Einnahmen",
                    Arrays.asList(
                            "§7Coins die Spieler im §6/shop §7ausgegeben haben",
                            "§7Guthaben: §a" + fmt(shopEarnings) + " Coins",
                            "",
                            "§aLinksklick: Auf aktive Bank übertragen"
                    )));
        }

        // Einzahlen / Auszahlen (row 3)
        inv.setItem(22, makeItem(Material.EMERALD_BLOCK, "§6Einzahlen",
                Arrays.asList("§7Linksklick: Betrag eingeben")));
        inv.setItem(23, makeItem(Material.RED_CONCRETE, "§cAuszahlen",
                Arrays.asList("§7Linksklick: Betrag eingeben")));

        // Overview (slot 40)
        List<String> overviewLore = new ArrayList<>();
        double total = 0;
        for (int i = 1; i <= maxBanks; i++) {
            if (bm.isBankUnlocked(uuid, i)) {
                double b = bm.getBankBalance(uuid, i);
                total += b;
                overviewLore.add("§7Bank " + i + ": §a" + fmt(b) + (i == activeSlot ? " §e(Aktiv)" : ""));
            } else {
                overviewLore.add("§7Bank " + i + ": §8[Gesperrt]");
            }
        }
        overviewLore.add("§7Zinsen-Konto: §6" + fmt(bm.getZinsen(uuid)));
        overviewLore.add("§7Markt-Bank: §6" + fmt(bm.getMarktBalance(uuid)));
        overviewLore.add("§7Gesamt: §a" + fmt(total));
        inv.setItem(40, makeItem(Material.PAPER, "§eKonto-Übersicht", overviewLore));

        // Page navigation for admin
        if (maxBanks > 5) {
            if (page > 1) {
                inv.setItem(45, makeItem(Material.ARROW, "§7◀ Seite 1", null));
            }
            if (endBank < maxBanks) {
                inv.setItem(53, makeItem(Material.ARROW, "§7Seite 2 ▶", null));
            }
        }

        // Close button
        inv.setItem(49, makeItem(Material.BARRIER, "§cSchließen", null));

        player.openInventory(inv);
    }

    private void renderBank(Inventory inv, UUID uuid, BankManager bm, int bankNum, int guiSlot, int activeSlot) {
        boolean unlocked = bm.isBankUnlocked(uuid, bankNum);
        double balance   = bm.getBankBalance(uuid, bankNum);

        if (unlocked) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Guthaben: §a" + fmt(balance) + " Coins");
            lore.add(bankNum == activeSlot ? "§e(Aktiv)" : "§7Klicken zum Aktivieren");
            inv.setItem(guiSlot, makeItem(Material.BLUE_STAINED_GLASS, "§9Bank " + bankNum, lore));
        } else {
            inv.setItem(guiSlot, makeItem(Material.RED_STAINED_GLASS,
                    "§cBank " + bankNum + " §7[Gesperrt]",
                    Arrays.asList("§7Freischalten für §6250.000 Coins", "§7Klicken zum Freischalten")));
        }
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }

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
