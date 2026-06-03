package at.minich.opserver.crates;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CrateListener implements Listener {

    private final OpServerPlugin plugin;
    private final Map<UUID, CrateGUI> activeGuis = new HashMap<>();

    public CrateListener(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only right-click actions, ignore off-hand duplicates
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        CrateType type = plugin.getCrateManager().getCrateType(item);
        if (type == null) return;

        event.setCancelled(true);

        // Don't open another crate while one is active
        CrateGUI existing = activeGuis.get(player.getUniqueId());
        if (existing != null && !existing.isFinished()) return;

        // Consume one crate
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        CrateGUI gui = new CrateGUI(plugin, player, type);
        activeGuis.put(player.getUniqueId(), gui);
        gui.open();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        CrateGUI gui = activeGuis.get(player.getUniqueId());
        if (gui == null) return;
        if (event.getInventory().equals(gui.getInventory())) {
            event.setCancelled(true);
        }
    }
}
