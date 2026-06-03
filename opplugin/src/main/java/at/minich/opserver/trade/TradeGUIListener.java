package at.minich.opserver.trade;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class TradeGUIListener implements Listener {

    private final OpServerPlugin plugin;
    private final Map<UUID, TradeGUI> activeTrades;

    public TradeGUIListener(OpServerPlugin plugin, Map<UUID, TradeGUI> activeTrades) {
        this.plugin = plugin;
        this.activeTrades = activeTrades;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        TradeGUI trade = activeTrades.get(uuid);
        if (trade == null) return;

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) {
            event.setCancelled(true);
            return;
        }

        // Only interact with the trade inventory, not player's own inventory
        if (clickedInv.equals(player.getInventory())) return;

        int slot = event.getSlot();
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        // Block editing right (view-only) slots
        if (TradeGUI.isRightSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        // Handle control slots
        if (TradeGUI.isControlSlot(slot)) {
            event.setCancelled(true);

            if (slot == TradeGUI.CANCEL_SLOT_LEFT || slot == TradeGUI.CANCEL_SLOT_RIGHT) {
                cancelTrade(trade, player, prefix);
                return;
            }

            if (slot == TradeGUI.CONFIRM_SLOT_LEFT || slot == TradeGUI.CONFIRM_SLOT_RIGHT) {
                // Sync offers before confirming
                trade.syncOffers();
                boolean bothReady = trade.confirm(player);
                if (bothReady) {
                    executeTrade(trade, prefix);
                } else {
                    player.sendMessage(prefix + "§aWarte auf die Bestätigung des anderen Spielers...");
                    // Notify the other player
                    Player other = player.getUniqueId().equals(trade.getInitiator().getUniqueId())
                            ? trade.getTarget() : trade.getInitiator();
                    if (other != null && other.isOnline()) {
                        other.sendMessage(prefix + "§e" + player.getName() + " §ahat den Handel bestätigt. Bestätige auch du!");
                    }
                }
                return;
            }
        }

        // Regular left-slot interaction — sync offers to the other player's view
        plugin.getServer().getScheduler().runTask(plugin, trade::syncOffers);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        TradeGUI trade = activeTrades.get(uuid);
        if (trade == null) return;

        // If both confirmed, the trade was already executed; ignore
        if (trade.isInitiatorConfirmed() && trade.isTargetConfirmed()) return;

        // Otherwise treat as cancel
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
        cancelTrade(trade, player, prefix);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        TradeGUI trade = activeTrades.get(uuid);
        if (trade == null) return;
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
        cancelTrade(trade, event.getPlayer(), prefix);
    }

    private void executeTrade(TradeGUI trade, String prefix) {
        ItemStack[] initOffer = trade.collectOffer(true);
        ItemStack[] targOffer = trade.collectOffer(false);

        // Remove items from trade inventories
        for (int slot : TradeGUI.LEFT_SLOTS) {
            trade.getInitiatorInv().setItem(slot, null);
            trade.getTargetInv().setItem(slot, null);
        }

        activeTrades.remove(trade.getInitiator().getUniqueId());
        activeTrades.remove(trade.getTarget().getUniqueId());

        trade.close();

        // Give initiator what target offered, and vice-versa
        giveItems(trade.getInitiator(), targOffer);
        giveItems(trade.getTarget(), initOffer);

        trade.getInitiator().sendMessage(prefix + "§aHandel erfolgreich abgeschlossen!");
        trade.getTarget().sendMessage(prefix + "§aHandel erfolgreich abgeschlossen!");
    }

    private void cancelTrade(TradeGUI trade, Player canceller, String prefix) {
        activeTrades.remove(trade.getInitiator().getUniqueId());
        activeTrades.remove(trade.getTarget().getUniqueId());

        // Return items to their owners
        ItemStack[] initOffer = trade.collectOffer(true);
        ItemStack[] targOffer = trade.collectOffer(false);

        // Clear trade inv before closing to avoid double-return via close event
        for (int slot : TradeGUI.LEFT_SLOTS) {
            trade.getInitiatorInv().setItem(slot, null);
            trade.getTargetInv().setItem(slot, null);
        }

        trade.close();

        giveItems(trade.getInitiator(), initOffer);
        giveItems(trade.getTarget(), targOffer);

        trade.getInitiator().sendMessage(prefix + "§cHandel abgebrochen.");
        trade.getTarget().sendMessage(prefix + "§cHandel abgebrochen.");
    }

    private void giveItems(Player player, ItemStack[] items) {
        if (items == null || !player.isOnline()) return;
        for (ItemStack item : items) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }
}
