package at.minich.opserver.items;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.shop.ServerShop;
import at.minich.opserver.shop.ShopItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SellLeggingListener implements Listener {

    private static final double RADIUS = 8.0;
    private final OpServerPlugin plugin;
    private final NamespacedKey keyFilter;

    public SellLeggingListener(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.keyFilter = new NamespacedKey(plugin, "selllegging_filter");
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    ItemStack legs = player.getInventory().getLeggings();
                    if (!isSellLegging(legs)) continue;

                    ItemMeta meta = legs.getItemMeta();
                    if (meta == null) continue;
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    String filterStr = pdc.getOrDefault(keyFilter, PersistentDataType.STRING, "");
                    Set<Material> keepMats = parseFilter(filterStr);

                    Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                            player.getLocation(), RADIUS, RADIUS, RADIUS, e -> e instanceof Item);

                    double totalEarned = 0;
                    int soldCount = 0;

                    for (Entity entity : nearby) {
                        Item dropped = (Item) entity;
                        ItemStack stack = dropped.getItemStack();

                        if (keepMats.contains(stack.getType())) {
                            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack.clone());
                            if (leftovers.isEmpty()) dropped.remove();
                            else dropped.setItemStack(leftovers.values().iterator().next());
                        } else {
                            ShopItem shopItem = ServerShop.findItem(stack.getType());
                            if (shopItem != null) {
                                totalEarned += shopItem.sellPrice() * stack.getAmount();
                                soldCount += stack.getAmount();
                                dropped.remove();
                            } else {
                                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack.clone());
                                if (leftovers.isEmpty()) dropped.remove();
                                else dropped.setItemStack(leftovers.values().iterator().next());
                            }
                        }
                    }

                    if (totalEarned > 0) {
                        plugin.getEconomyManager().deposit(player.getUniqueId(), totalEarned);
                        player.sendMessage("§6💰 §e" + soldCount + " §aItems automatisch verkauft für §e"
                                + String.format("%.1f", totalEarned) + " §aCoins!");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    public void updateLore(ItemStack item, ItemMeta meta, String filter) {
        List<String> lore = new ArrayList<>();
        lore.add("§aAktiv §8| §7Anziehen zum Aktivieren");
        lore.add("§7Radius: §b" + (int) RADIUS + " Blöcke");
        lore.add("§7Modus: §6Alles verkaufen");
        if (filter.isEmpty()) lore.add("§7Behalten-Filter: §eKeine Items");
        else lore.add("§7Behalten: §e" + filter.replace(",", "§7, §e"));
        lore.add("");
        lore.add("§8/vleggingfilter <item> §7- Behalten-Filter setzen");
        lore.add("§8Custom Item");
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private Set<Material> parseFilter(String filterStr) {
        Set<Material> result = new HashSet<>();
        if (filterStr == null || filterStr.isEmpty()) return result;
        for (String s : filterStr.split(",")) {
            Material m = Material.matchMaterial(s.trim());
            if (m != null) result.add(m);
        }
        return result;
    }

    public boolean isSellLegging(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Verkaufs-Leggings");
    }

    public NamespacedKey getKeyFilter() { return keyFilter; }
}
