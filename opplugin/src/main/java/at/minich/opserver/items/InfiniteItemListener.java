package at.minich.opserver.items;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InfiniteItemListener implements Listener {

    // Store saved totem for players who died
    private final Map<UUID, ItemStack> savedTotems = new HashMap<>();

    private static final String INFINITE_TAG = "§8∞ Unendlich";

    public static boolean isInfinite(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return false;
        return meta.getLore().stream().anyMatch(l -> l.contains("∞ Unendlich"));
    }

    // --- Drop-Schutz: unendliche Items können nicht weggeworfen werden ---
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isInfinite(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c∞ §7Unendliche Items können nicht weggeworfen werden!");
        }
    }

    // --- Enderperle: restore after throw (via ProjectileLaunchEvent) ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPearlThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        if (!(event.getEntity() instanceof org.bukkit.entity.EnderPearl)) return;

        // Find the infinite ender pearl in inventory
        ItemStack infinitePearl = null;
        for (ItemStack inv : player.getInventory().getContents()) {
            if (inv != null && inv.getType() == Material.ENDER_PEARL && isInfinite(inv)) {
                infinitePearl = inv.clone();
                break;
            }
        }
        if (infinitePearl == null) return;

        final ItemStack toRestore = infinitePearl;
        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.bukkit.Bukkit.getPluginManager().getPlugin("OpServer"),
            () -> {
                // Check if still has one, if not give back
                for (ItemStack inv : player.getInventory().getContents()) {
                    if (inv != null && inv.getType() == Material.ENDER_PEARL && isInfinite(inv)) return;
                }
                toRestore.setAmount(1);
                player.getInventory().addItem(toRestore);
            }, 1L
        );
    }

    // --- Interact fallback (kept for other items) ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        ItemStack item = event.getItem();
        // Ender pearl handled by ProjectileLaunchEvent
        if (item.getType() == Material.ENDER_PEARL) return;
    }

    // --- TNT: restore after placing ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTNTPrime(TNTPrimeEvent event) {
        if (!(event.getPrimingEntity() instanceof Player player)) return;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() != Material.TNT) return;
        if (!isInfinite(held)) return;

        org.bukkit.Bukkit.getScheduler().runTaskLater(
            org.bukkit.Bukkit.getPluginManager().getPlugin("OpServer"),
            () -> {
                // Ensure player still has the infinite TNT
                for (ItemStack inv : player.getInventory().getContents()) {
                    if (inv != null && inv.getType() == Material.TNT && isInfinite(inv)) return;
                }
                ItemStack restored = held.clone();
                restored.setAmount(1);
                player.getInventory().addItem(restored);
            }, 2L
        );
    }

    // --- Rakete: restore after shooting ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack consumable = event.getConsumable();
        if (consumable == null || consumable.getType() != Material.FIREWORK_ROCKET) return;
        if (!isInfinite(consumable)) return;

        event.setConsumeItem(false);
    }

    // --- Totem: restore after death ---
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Check if they used an infinite totem
        for (ItemStack drop : event.getDrops()) {
            if (drop != null && drop.getType() == Material.TOTEM_OF_UNDYING && isInfinite(drop)) {
                // Keep it — remove from drops and save
                event.getDrops().remove(drop);
                savedTotems.put(player.getUniqueId(), drop.clone());
                break;
            }
        }
        // Also check if it was in offhand (totems activate from offhand)
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == Material.TOTEM_OF_UNDYING && isInfinite(offhand)) {
            savedTotems.put(player.getUniqueId(), offhand.clone());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        ItemStack saved = savedTotems.remove(player.getUniqueId());
        if (saved != null) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                org.bukkit.Bukkit.getPluginManager().getPlugin("OpServer"),
                () -> player.getInventory().addItem(saved), 1L
            );
        }
    }
}
