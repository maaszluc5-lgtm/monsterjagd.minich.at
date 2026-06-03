package at.minich.opserver.trade;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Manages the trade inventory for a pair of players.
 * Inventory layout (54 slots):
 *   Left offer area  (your items): cols 0-3 in each row -> slots 0-3, 9-12, 18-21, 27-30, 36-39, 45-48
 *   Right offer area (their items): cols 5-8 in each row -> slots 5-8, 14-17, 23-26, 32-35, 41-44, 50-53
 *   Slot 4:  confirm button (initiator side)
 *   Slot 49: confirm button (target side)
 *   Slot 13: cancel button (initiator side)
 *   Slot 40: cancel button (target side)
 *   Slot 22: status display
 *   Slot 31: status display
 */
public class TradeGUI {

    // Left offer slots (initiator sees own items here; target sees these as read-only right)
    public static final int[] LEFT_SLOTS  = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39, 45, 46, 47, 48};
    // Right offer slots
    public static final int[] RIGHT_SLOTS = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44, 50, 51, 52, 53};

    public static final int CONFIRM_SLOT_LEFT  = 4;
    public static final int CONFIRM_SLOT_RIGHT = 49;
    public static final int CANCEL_SLOT_LEFT   = 13;
    public static final int CANCEL_SLOT_RIGHT  = 40;
    public static final int STATUS_SLOT_LEFT   = 22;
    public static final int STATUS_SLOT_RIGHT  = 31;

    private final Player initiator;
    private final Player target;

    private final Inventory initiatorInv;
    private final Inventory targetInv;

    private boolean initiatorConfirmed = false;
    private boolean targetConfirmed = false;

    public TradeGUI(Player initiator, Player target) {
        this.initiator = initiator;
        this.target = target;

        String initTitle   = "§6Handel §7mit §e" + target.getName();
        String targetTitle = "§6Handel §7mit §e" + initiator.getName();

        initiatorInv = Bukkit.createInventory(null, 54, initTitle);
        targetInv    = Bukkit.createInventory(null, 54, targetTitle);

        setupButtons(initiatorInv, false);
        setupButtons(targetInv, false);
    }

    private void setupButtons(Inventory inv, boolean anyConfirmed) {
        inv.setItem(CONFIRM_SLOT_LEFT,  makeWool(Material.LIME_WOOL,  "§aHandel bestätigen"));
        inv.setItem(CONFIRM_SLOT_RIGHT, makeWool(Material.LIME_WOOL,  "§aHandel bestätigen"));
        inv.setItem(CANCEL_SLOT_LEFT,   makeWool(Material.RED_WOOL,   "§cAbbrechen"));
        inv.setItem(CANCEL_SLOT_RIGHT,  makeWool(Material.RED_WOOL,   "§cAbbrechen"));
        inv.setItem(STATUS_SLOT_LEFT,   makeWool(Material.GRAY_WOOL,  "§7Status"));
        inv.setItem(STATUS_SLOT_RIGHT,  makeWool(Material.GRAY_WOOL,  "§7Status"));
    }

    private ItemStack makeWool(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Returns true if both confirmed and trade should execute. */
    public boolean confirm(Player player) {
        if (player.getUniqueId().equals(initiator.getUniqueId())) {
            initiatorConfirmed = true;
        } else {
            targetConfirmed = true;
        }
        updateStatusDisplays();
        return initiatorConfirmed && targetConfirmed;
    }

    private void updateStatusDisplays() {
        Material initMat  = initiatorConfirmed ? Material.LIME_WOOL  : Material.YELLOW_WOOL;
        Material targMat  = targetConfirmed    ? Material.LIME_WOOL  : Material.YELLOW_WOOL;
        String initStatus = initiatorConfirmed ? "§a" + initiator.getName() + " bereit" : "§e" + initiator.getName() + " wartet...";
        String targStatus = targetConfirmed    ? "§a" + target.getName()    + " bereit" : "§e" + target.getName()    + " wartet...";

        // Both inventories show both statuses
        for (Inventory inv : Arrays.asList(initiatorInv, targetInv)) {
            inv.setItem(STATUS_SLOT_LEFT,  makeWool(initMat, initStatus));
            inv.setItem(STATUS_SLOT_RIGHT, makeWool(targMat, targStatus));
        }
    }

    /** Collect items placed by a player in their left offer slots. */
    public ItemStack[] collectOffer(boolean fromInitiator) {
        Inventory inv = fromInitiator ? initiatorInv : targetInv;
        ItemStack[] offer = new ItemStack[LEFT_SLOTS.length];
        for (int i = 0; i < LEFT_SLOTS.length; i++) {
            offer[i] = inv.getItem(LEFT_SLOTS[i]);
        }
        return offer;
    }

    /** Sync target's offer into initiator's right slots and vice-versa (view only). */
    public void syncOffers() {
        // Copy initiator left -> target right
        for (int i = 0; i < LEFT_SLOTS.length; i++) {
            ItemStack item = initiatorInv.getItem(LEFT_SLOTS[i]);
            targetInv.setItem(RIGHT_SLOTS[i], item == null ? null : item.clone());
        }
        // Copy target left -> initiator right
        for (int i = 0; i < LEFT_SLOTS.length; i++) {
            ItemStack item = targetInv.getItem(LEFT_SLOTS[i]);
            initiatorInv.setItem(RIGHT_SLOTS[i], item == null ? null : item.clone());
        }
    }

    public void open() {
        initiator.openInventory(initiatorInv);
        target.openInventory(targetInv);
    }

    public void close() {
        initiator.closeInventory();
        target.closeInventory();
    }

    public Player getInitiator() { return initiator; }
    public Player getTarget()    { return target; }
    public Inventory getInitiatorInv() { return initiatorInv; }
    public Inventory getTargetInv()    { return targetInv; }
    public boolean isInitiatorConfirmed() { return initiatorConfirmed; }
    public boolean isTargetConfirmed()    { return targetConfirmed; }

    public static boolean isControlSlot(int slot) {
        return slot == CONFIRM_SLOT_LEFT || slot == CONFIRM_SLOT_RIGHT
            || slot == CANCEL_SLOT_LEFT  || slot == CANCEL_SLOT_RIGHT
            || slot == STATUS_SLOT_LEFT  || slot == STATUS_SLOT_RIGHT;
    }

    public static boolean isRightSlot(int slot) {
        for (int s : RIGHT_SLOTS) if (s == slot) return true;
        return false;
    }
}
