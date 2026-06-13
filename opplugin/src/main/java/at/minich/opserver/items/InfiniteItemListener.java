package at.minich.opserver.items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class InfiniteItemListener implements Listener {

    private final Plugin plugin;

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

    // --- Rechtsklick: Vorher zählen, nachher vergleichen ---
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (!event.getAction().name().contains("RIGHT")) return;

        ItemStack item = event.getItem();
        if (item.getType() == Material.TNT) return; // handled by BlockPlaceEvent
        if (!isInfinite(item)) return;

        Player player = event.getPlayer();
        Material type = item.getType();
        ItemStack saved = item.clone();
        saved.setAmount(1);

        int before = countInfinite(player, type);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int after = countInfinite(player, type);
            for (int i = 0; i < (before - after); i++) {
                player.getInventory().addItem(saved.clone());
            }
        }, 1L);
    }

    // --- TNT: automatisch zünden + wiedergeben ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTNTPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.TNT) return;
        ItemStack item = event.getItemInHand();
        if (!isInfinite(item)) return;

        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        ItemStack saved = item.clone();
        saved.setAmount(1);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Replace TNT block with primed TNT
            if (loc.getBlock().getType() == Material.TNT) {
                loc.getBlock().setType(Material.AIR);
                loc.getWorld().spawn(loc.add(0.5, 0, 0.5), TNTPrimed.class, tnt -> {
                    tnt.setFuseTicks(80);
                    tnt.setSource(player);
                });
            }
            // Restore item
            int after = countInfinite(player, Material.TNT);
            int before = countInfinite(player, Material.TNT) + 1;
            for (ItemStack inv : player.getInventory().getContents()) {
                if (inv != null && inv.getType() == Material.TNT && isInfinite(inv)) return;
            }
            player.getInventory().addItem(saved);
        }, 1L);
    }

    // --- Totem: EntityResurrectEvent (aktiviert sich beim fast-Tod) ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemStack offhand = player.getInventory().getItemInOffHand();

        ItemStack totem = null;
        if (hand.getType() == Material.TOTEM_OF_UNDYING && isInfinite(hand)) totem = hand.clone();
        else if (offhand.getType() == Material.TOTEM_OF_UNDYING && isInfinite(offhand)) totem = offhand.clone();

        if (totem == null) return;
        final ItemStack savedTotem = totem;
        savedTotem.setAmount(1);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Check if totem is gone and restore
            ItemStack h = player.getInventory().getItemInMainHand();
            ItemStack o = player.getInventory().getItemInOffHand();
            boolean stillHas = (h.getType() == Material.TOTEM_OF_UNDYING && isInfinite(h))
                    || (o.getType() == Material.TOTEM_OF_UNDYING && isInfinite(o));
            if (!stillHas) {
                player.getInventory().addItem(savedTotem);
            }
        }, 2L);
    }

    private int countInfinite(Player player, Material type) {
        int count = 0;
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv != null && inv.getType() == type && isInfinite(inv)) count += inv.getAmount();
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == type && isInfinite(offhand)) count += offhand.getAmount();
        return count;
    }
}
