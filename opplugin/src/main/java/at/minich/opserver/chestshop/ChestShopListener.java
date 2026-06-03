package at.minich.opserver.chestshop;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Handles all chest-shop interactions:
 *  - Sign placement  → create shop
 *  - Right-click sign → buy
 *  - Shift+right-click sign → sell back
 *  - Break sign / chest → remove shop
 */
public class ChestShopListener implements Listener {

    private static final BlockFace[] ADJACENT = {
            BlockFace.UP, BlockFace.DOWN,
            BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST
    };

    private final OpServerPlugin plugin;
    private final ChestShopManager manager;

    public ChestShopListener(OpServerPlugin plugin, ChestShopManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // -------------------------------------------------------------------------
    // Shop creation via sign placement
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        String[] lines = event.getLines();
        if (!lines[0].trim().equalsIgnoreCase("[Shop]")) return;

        Player player = event.getPlayer();

        // Find adjacent chest
        Block signBlock = event.getBlock();
        Block chestBlock = findAdjacentChest(signBlock);
        if (chestBlock == null) {
            player.sendMessage("§cKein Kiste neben/unter/über dem Schild gefunden!");
            event.setCancelled(true);
            return;
        }

        // Only one shop per chest
        if (manager.getByChest(chestBlock.getLocation()) != null) {
            player.sendMessage("§cAn dieser Kiste gibt es bereits einen Shop!");
            event.setCancelled(true);
            return;
        }

        // Parse amount
        int amount;
        try {
            amount = Integer.parseInt(lines[1].trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage("§cUngültige Menge in Zeile 2!");
            event.setCancelled(true);
            return;
        }

        // Parse buy price
        double buyPrice;
        try {
            buyPrice = Double.parseDouble(lines[2].trim());
            if (buyPrice < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage("§cUngültiger Kaufpreis in Zeile 3!");
            event.setCancelled(true);
            return;
        }

        // Parse optional sell price
        double sellPrice = 0;
        String line4 = lines[3].trim();
        if (!line4.isEmpty()) {
            try {
                sellPrice = Double.parseDouble(line4);
                if (sellPrice < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage("§cUngültiger Verkaufspreis in Zeile 4!");
                event.setCancelled(true);
                return;
            }
        }

        // Determine item from chest
        Chest chest = (Chest) chestBlock.getState();
        ItemStack shopItem = null;
        for (ItemStack stack : chest.getInventory().getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                shopItem = stack.clone();
                shopItem.setAmount(1);
                break;
            }
        }
        if (shopItem == null) {
            player.sendMessage("§cDie Kiste muss mindestens ein Item enthalten, das verkauft werden soll!");
            event.setCancelled(true);
            return;
        }

        // Max shops check
        int maxShops = plugin.getConfig().getInt("chestshop.max-per-player", 10);
        if (manager.countShopsForPlayer(player.getUniqueId()) >= maxShops) {
            player.sendMessage("§cDu hast bereits die maximale Anzahl an Shops erreicht (" + maxShops + ")!");
            event.setCancelled(true);
            return;
        }

        // Format sign
        event.setLine(0, "§6[Shop]");
        event.setLine(1, String.valueOf(amount));
        event.setLine(2, "§aKauf: " + formatPrice(buyPrice));
        event.setLine(3, sellPrice > 0 ? "§cVerk: " + formatPrice(sellPrice) : "§7-");

        // Create shop
        manager.createShop(player.getUniqueId(), player.getName(),
                chestBlock.getLocation(), signBlock.getLocation(),
                shopItem, amount, buyPrice, sellPrice);

        player.sendMessage("§aShop erfolgreich erstellt!");
    }

    // -------------------------------------------------------------------------
    // Buy / sell via right-click
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        org.bukkit.event.block.Action action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!isSign(block.getType())) return;

        ChestShop shop = manager.getBySign(block.getLocation());
        if (shop == null) return;

        event.setCancelled(true); // prevent opening chest/sign GUI

        Player player = event.getPlayer();

        if (player.isSneaking()) {
            handleSell(player, shop);
        } else {
            handleBuy(player, shop);
        }
    }

    private void handleBuy(Player player, ChestShop shop) {
        // Owner can't buy from own shop
        if (player.getUniqueId().equals(shop.getOwnerUuid())) {
            player.sendMessage("§cDu kannst nicht in deinem eigenen Shop kaufen.");
            return;
        }

        int stock = manager.getStock(shop);
        if (stock <= 0) {
            player.sendMessage("§cDieser Shop ist ausverkauft!");
            return;
        }

        double price = shop.getBuyPrice();
        if (!plugin.getEconomyManager().has(player.getUniqueId(), price)) {
            player.sendMessage("§cDu hast nicht genug Coins! (Benötigt: " + formatPrice(price) + ")");
            return;
        }

        // Check inventory space
        ItemStack toGive = shop.getItem();
        toGive.setAmount(shop.getAmount());
        if (!hasInventorySpace(player, toGive)) {
            player.sendMessage("§cDein Inventar ist voll!");
            return;
        }

        // Deduct coins from buyer
        plugin.getEconomyManager().withdraw(player.getUniqueId(), price);

        // Remove items from chest
        if (!manager.removeFromChest(shop, shop.getAmount())) {
            // Refund if removal failed
            plugin.getEconomyManager().deposit(player.getUniqueId(), price);
            player.sendMessage("§cFehler beim Entfernen der Items aus der Kiste!");
            return;
        }

        // Give items to buyer
        player.getInventory().addItem(toGive);

        // Pay seller via markt balance
        plugin.getBankManager().addMarktBalance(shop.getOwnerUuid(), price);

        player.sendMessage("§aDu hast §f" + shop.getAmount() + "x §b" + getItemName(shop.getItem())
                + " §afür §f" + formatPrice(price) + " §agekauft.");

        // Update hologram
        manager.updateHologram(shop);
    }

    private void handleSell(Player player, ChestShop shop) {
        if (!shop.hasSellPrice()) {
            player.sendMessage("§cDieser Shop kauft keine Items zurück.");
            return;
        }

        // Owner can't sell to own shop
        if (player.getUniqueId().equals(shop.getOwnerUuid())) {
            player.sendMessage("§cDu kannst nicht in deinem eigenen Shop verkaufen.");
            return;
        }

        double sellPrice = shop.getSellPrice();

        // Check owner has enough markt balance to pay
        double ownerMarkt = plugin.getBankManager().getMarktBalance(shop.getOwnerUuid());
        if (ownerMarkt < sellPrice) {
            player.sendMessage("§cDer Shop-Besitzer hat nicht genug Guthaben um dein Item zu kaufen.");
            return;
        }

        // Check player has the item
        ItemStack template = shop.getItem();
        int needed = shop.getAmount();
        if (countItems(player, template) < needed) {
            player.sendMessage("§cDu hast nicht genug §b" + getItemName(template) + "§c! (Benötigt: " + needed + ")");
            return;
        }

        // Check chest has space
        Block chestBlock = shop.getChestLoc().getBlock();
        if (!(chestBlock.getState() instanceof Chest chest)) {
            player.sendMessage("§cDie Kiste des Shops wurde nicht gefunden!");
            return;
        }
        ItemStack toAdd = template.clone();
        toAdd.setAmount(needed);
        if (!hasChestSpace(chest, toAdd)) {
            player.sendMessage("§cDie Shop-Kiste ist voll!");
            return;
        }

        // Deduct from owner markt balance
        if (!plugin.getBankManager().withdrawMarktBalance(shop.getOwnerUuid(), sellPrice)) {
            player.sendMessage("§cDer Shop-Besitzer hat nicht genug Guthaben.");
            return;
        }

        // Remove items from player
        removeItems(player, template, needed);

        // Add items to chest
        chest.getInventory().addItem(toAdd);

        // Pay player
        plugin.getEconomyManager().deposit(player.getUniqueId(), sellPrice);

        player.sendMessage("§aDu hast §f" + needed + "x §b" + getItemName(template)
                + " §afür §f" + formatPrice(sellPrice) + " §averkauft.");

        manager.updateHologram(shop);
    }

    // -------------------------------------------------------------------------
    // Shop removal
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Breaking a sign
        if (isSign(block.getType())) {
            ChestShop shop = manager.getBySign(block.getLocation());
            if (shop != null) {
                if (!player.getUniqueId().equals(shop.getOwnerUuid()) && !player.hasPermission("opserver.admin")) {
                    player.sendMessage("§cDas ist nicht dein Shop!");
                    event.setCancelled(true);
                    return;
                }
                manager.removeShop(shop);
                player.sendMessage("§aShop entfernt.");
            }
            return;
        }

        // Breaking a chest
        if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
            ChestShop shop = manager.getByChest(block.getLocation());
            if (shop != null) {
                if (!player.getUniqueId().equals(shop.getOwnerUuid()) && !player.hasPermission("opserver.admin")) {
                    player.sendMessage("§cDas ist nicht deine Shop-Kiste! Brich zuerst das Schild, um den Shop zu entfernen.");
                    event.setCancelled(true);
                    return;
                }
                // Remove sign block
                Block signBlock = shop.getSignLoc().getBlock();
                if (isSign(signBlock.getType())) {
                    signBlock.breakNaturally();
                }
                manager.removeShop(shop);
                player.sendMessage("§aShop entfernt.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    private Block findAdjacentChest(Block sign) {
        for (BlockFace face : ADJACENT) {
            Block neighbor = sign.getRelative(face);
            if (neighbor.getType() == Material.CHEST || neighbor.getType() == Material.TRAPPED_CHEST) {
                return neighbor;
            }
        }
        return null;
    }

    private boolean isSign(Material type) {
        return type.name().endsWith("_SIGN") || type.name().endsWith("_WALL_SIGN");
    }

    private boolean hasInventorySpace(Player player, ItemStack item) {
        return player.getInventory().firstEmpty() != -1 ||
               Arrays.stream(player.getInventory().getContents())
                   .filter(s -> s != null && s.isSimilar(item))
                   .mapToInt(s -> s.getMaxStackSize() - s.getAmount())
                   .sum() >= item.getAmount();
    }

    private boolean hasChestSpace(Chest chest, ItemStack item) {
        return chest.getInventory().firstEmpty() != -1 ||
               Arrays.stream(chest.getInventory().getContents())
                   .filter(s -> s != null && s.isSimilar(item))
                   .mapToInt(s -> s.getMaxStackSize() - s.getAmount())
                   .sum() >= item.getAmount();
    }

    private int countItems(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(template)) count += stack.getAmount();
        }
        return count;
    }

    private void removeItems(Player player, ItemStack template, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.isSimilar(template)) {
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
                if (stack.getAmount() == 0) contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
    }

    private String getItemName(ItemStack item) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        String raw = item.getType().name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    private String formatPrice(double price) {
        if (price == (long) price) return (long) price + " Coins";
        return String.format("%.2f Coins", price);
    }
}
