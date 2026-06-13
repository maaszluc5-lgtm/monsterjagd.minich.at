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

public class SellMagnetListener implements Listener {

    private static final double RADIUS = 8.0;
    private final OpServerPlugin plugin;
    private final NamespacedKey keyActive;
    private final NamespacedKey keyFilter;

    public SellMagnetListener(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.keyActive = new NamespacedKey(plugin, "sellmagnet_active");
        this.keyFilter = new NamespacedKey(plugin, "sellmagnet_filter");
        startTask();
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (!isSellMagnetItem(item)) return;

        event.setCancelled(true);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean active = pdc.getOrDefault(keyActive, PersistentDataType.BYTE, (byte) 1) == 1;
        boolean newActive = !active;
        pdc.set(keyActive, PersistentDataType.BYTE, (byte) (newActive ? 1 : 0));

        updateLore(item, meta, newActive, pdc.getOrDefault(keyFilter, PersistentDataType.STRING, ""));
        item.setItemMeta(meta);
        event.getPlayer().sendMessage(newActive
                ? "§6💰 Verkaufs-Magnet §aaktiviert!"
                : "§6💰 Verkaufs-Magnet §cdeaktiviert!");
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    ItemStack hand = player.getInventory().getItemInOffHand();
                    if (!isSellMagnetItem(hand)) continue;

                    ItemMeta meta = hand.getItemMeta();
                    if (meta == null) continue;
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    if (pdc.getOrDefault(keyActive, PersistentDataType.BYTE, (byte) 1) != 1) continue;

                    String filterStr = pdc.getOrDefault(keyFilter, PersistentDataType.STRING, "");
                    Set<Material> keepMats = parseFilter(filterStr);

                    Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                            player.getLocation(), RADIUS, RADIUS, RADIUS,
                            e -> e instanceof Item
                    );

                    double totalEarned = 0;
                    int soldCount = 0;

                    for (Entity entity : nearby) {
                        Item dropped = (Item) entity;
                        ItemStack stack = dropped.getItemStack();

                        if (keepMats.contains(stack.getType())) {
                            // Keep item — pick up to inventory
                            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack.clone());
                            if (leftovers.isEmpty()) {
                                dropped.remove();
                            } else {
                                dropped.setItemStack(leftovers.values().iterator().next());
                            }
                        } else {
                            // Try to sell
                            ShopItem shopItem = ServerShop.findItem(stack.getType());
                            if (shopItem != null) {
                                double earned = shopItem.sellPrice() * stack.getAmount();
                                totalEarned += earned;
                                soldCount += stack.getAmount();
                                dropped.remove();
                            }
                            // Items not in shop and not in keep-filter: just pick up
                            else {
                                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack.clone());
                                if (leftovers.isEmpty()) {
                                    dropped.remove();
                                } else {
                                    dropped.setItemStack(leftovers.values().iterator().next());
                                }
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

    public void updateLore(ItemStack item, ItemMeta meta, boolean active, String filter) {
        List<String> lore = new ArrayList<>();
        lore.add(active ? "§aAktiv §8| §7Rechtsklick zum Deaktivieren" : "§cInaktiv §8| §7Rechtsklick zum Aktivieren");
        lore.add("§7Radius: §b" + (int) RADIUS + " Blöcke");
        lore.add("§7Modus: §6Alles verkaufen");
        if (filter.isEmpty()) {
            lore.add("§7Behalten-Filter: §eKeine Items");
        } else {
            lore.add("§7Behalten: §e" + filter.replace(",", "§7, §e"));
        }
        lore.add("");
        lore.add("§8/vmagnetfilter <item> §7- Behalten-Filter setzen");
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

    public boolean isSellMagnetItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Verkaufs-Magnet");
    }

    public NamespacedKey getKeyActive() { return keyActive; }
    public NamespacedKey getKeyFilter() { return keyFilter; }
}
