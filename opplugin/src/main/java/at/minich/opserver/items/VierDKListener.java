package at.minich.opserver.items;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.util.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 4D-Kiste: a personal 54-slot portable inventory.
 * Right-click with the item to open. Contents are persisted per player UUID.
 */
public class VierDKListener implements Listener {

    private static final String INVENTORY_TITLE_PREFIX = "§6§l4D-Kiste von ";
    private static final int INVENTORY_SIZE = 54;
    private static final String DATA_DIR = "4dk";

    private final OpServerPlugin plugin;
    private final DataManager dataManager;

    /** Maps player UUID → currently open 4DK inventory */
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public VierDKListener(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only right-click actions
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (!meta.getDisplayName().contains("4D-Kiste")) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE,
                INVENTORY_TITLE_PREFIX + player.getName());

        // Load saved contents
        YamlConfiguration config = dataManager.loadYaml(DATA_DIR + "/" + uuid + ".yml");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (config.contains("slot." + i)) {
                ItemStack stored = config.getItemStack("slot." + i);
                if (stored != null) {
                    inv.setItem(i, stored);
                }
            }
        }

        openInventories.put(uuid, inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        Inventory tracked = openInventories.get(uuid);
        if (tracked == null) return;

        // Verify this is the same inventory instance
        if (!event.getInventory().equals(tracked)) return;

        openInventories.remove(uuid);

        // Persist contents
        YamlConfiguration config = new YamlConfiguration();
        ItemStack[] contents = tracked.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                config.set("slot." + i, contents[i]);
            }
        }
        dataManager.saveYaml(config, DATA_DIR + "/" + uuid + ".yml");
    }
}
