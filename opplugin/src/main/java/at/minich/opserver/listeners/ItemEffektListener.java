package at.minich.opserver.listeners;

import at.minich.opserver.commands.ItemEffektCommand;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Every 2 seconds: checks main hand and offhand for item effects and applies them.
 */
public class ItemEffektListener {

    private final JavaPlugin plugin;

    public ItemEffektListener(JavaPlugin plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    applyEffectsFromItem(player, player.getInventory().getItemInMainHand(), "hand");
                    applyEffectsFromItem(player, player.getInventory().getItemInOffHand(), "offhand");
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // every 2 seconds
    }

    private void applyEffectsFromItem(Player player, ItemStack item, String currentSlot) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (PotionEffectType type : PotionEffectType.values()) {
            NamespacedKey key = new NamespacedKey(plugin, ItemEffektCommand.PDC_KEY_PREFIX + type.getName());
            String val = pdc.get(key, PersistentDataType.STRING);
            if (val == null) continue;

            String[] parts = val.split(":");
            int level = Integer.parseInt(parts[0]) - 1; // Bukkit uses 0-based
            String slot = parts.length > 1 ? parts[1] : "both";

            // Check if this slot matches
            if (slot.equals("hand") && !currentSlot.equals("hand")) continue;
            if (slot.equals("offhand") && !currentSlot.equals("offhand")) continue;

            // Apply for 3 seconds (60 ticks) so it stays active on next check
            player.addPotionEffect(new PotionEffect(type, 60, level, true, false, true));
        }
    }
}
