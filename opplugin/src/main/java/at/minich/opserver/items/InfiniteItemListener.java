package at.minich.opserver.items;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InfiniteItemListener implements Listener {

    private final Plugin plugin;
    private final Map<UUID, ItemStack> savedTotems = new HashMap<>();

    public InfiniteItemListener(Plugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isInfinite(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return false;
        return meta.getLore().stream().anyMatch(l -> l.contains("∞ Unendlich"));
    }

    // --- Drop-Schutz ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isInfinite(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c∞ §7Unendliche Items können nicht weggeworfen werden!");
        }
    }

    // --- Rechtsklick: Item merken und 1 Tick später wiedergeben ---
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (!event.getAction().name().contains("RIGHT")) return;

        ItemStack item = event.getItem();
        if (!isInfinite(item)) return;

        Player player = event.getPlayer();
        ItemStack saved = item.clone();
        saved.setAmount(1);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Check if the item is still in inventory
            for (ItemStack inv : player.getInventory().getContents()) {
                if (inv != null && inv.getType() == saved.getType() && isInfinite(inv)) return;
            }
            // Also check offhand
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand.getType() == saved.getType() && isInfinite(offhand)) return;
            // Item was consumed — restore it
            player.getInventory().addItem(saved);
        }, 1L);
    }

    // --- Tod: Totem wiedergeben ---
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == Material.TOTEM_OF_UNDYING && isInfinite(offhand)) {
            savedTotems.put(player.getUniqueId(), offhand.clone());
        }
        event.getDrops().removeIf(drop ->
            drop != null && drop.getType() == Material.TOTEM_OF_UNDYING && isInfinite(drop)
        );
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        ItemStack saved = savedTotems.remove(event.getPlayer().getUniqueId());
        if (saved != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> event.getPlayer().getInventory().addItem(saved), 1L);
        }
    }
}
