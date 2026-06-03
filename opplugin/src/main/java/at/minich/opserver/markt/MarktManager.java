package at.minich.opserver.markt;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MarktManager {

    private static final String FILE = "markt.yml";

    private final OpServerPlugin plugin;
    private final Map<String, MarktListing> listings = new ConcurrentHashMap<>();

    public MarktManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Creates a listing. Takes {@code amount} of {@code item} from the player's inventory.
     * Charges a listing fee. Returns the listing ID on success, or null on failure (message sent).
     */
    public String createListing(Player seller, ItemStack item, int amount, double price) {
        UUID uuid = seller.getUniqueId();

        // Validate price bounds
        double minPrice = plugin.getConfig().getDouble("markt.min-price", 1.0);
        double maxPrice = plugin.getConfig().getDouble("markt.max-price", 1_000_000_000.0);
        if (price < minPrice) {
            seller.sendMessage("§cDer Mindestpreis beträgt §6" + formatCoins(minPrice) + " Coins§c.");
            return null;
        }
        if (price > maxPrice) {
            seller.sendMessage("§cDer Höchstpreis beträgt §6" + formatCoins(maxPrice) + " Coins§c.");
            return null;
        }

        // Listing cap
        int maxListings = plugin.getConfig().getInt("markt.max-listings-per-player", 10);
        long currentCount = listings.values().stream()
                .filter(l -> l.getSellerUuid().equals(uuid))
                .count();
        if (currentCount >= maxListings) {
            seller.sendMessage("§cDu hast bereits §6" + maxListings + " §cAngebote eingestellt (Maximum).");
            return null;
        }

        // Check that the player actually has the item in sufficient quantity
        int available = countItem(seller, item);
        if (available < amount) {
            seller.sendMessage("§cDu hast nicht genug von diesem Item. (Vorhanden: " + available + ", Benötigt: " + amount + ")");
            return null;
        }

        // Listing fee
        double fee = plugin.getConfig().getDouble("markt.listing-fee", 50.0);
        if (fee > 0) {
            boolean paid = plugin.getEconomyManager().withdraw(uuid, fee);
            if (!paid) {
                seller.sendMessage("§cDu hast nicht genug Coins für die Einstellungsgebühr (§6" + formatCoins(fee) + " Coins§c).");
                return null;
            }
        }

        // Remove item from inventory
        removeItem(seller, item, amount);

        // Build listing
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MarktListing listing = new MarktListing(id, uuid, seller.getName(), item.clone(), amount, price, System.currentTimeMillis());
        listings.put(id, listing);
        save();

        seller.sendMessage("§aItem erfolgreich eingestellt! §7ID: §e" + id
                + (fee > 0 ? " §7(Gebühr: §c" + formatCoins(fee) + " Coins§7)" : ""));
        return id;
    }

    /**
     * Removes a listing. Only the seller or an OP may remove.
     */
    public boolean removeListing(String id, Player requester) {
        MarktListing listing = listings.get(id);
        if (listing == null) {
            requester.sendMessage("§cKein Angebot mit der ID §e" + id + " §cgefunden.");
            return false;
        }
        if (!listing.getSellerUuid().equals(requester.getUniqueId()) && !requester.isOp()) {
            requester.sendMessage("§cDu kannst nur deine eigenen Angebote entfernen.");
            return false;
        }

        listings.remove(id);
        save();

        // Return the item to the owner (if they are online and it's the owner requesting)
        if (listing.getSellerUuid().equals(requester.getUniqueId())) {
            ItemStack returnItem = listing.getItem().clone();
            returnItem.setAmount(listing.getAmount());
            giveOrDrop(requester, returnItem);
            requester.sendMessage("§aAngebot §e" + id + " §aentfernt. Item zurückgegeben.");
        } else {
            // OP removed someone else's listing — return to seller if online
            Player seller = plugin.getServer().getPlayer(listing.getSellerUuid());
            if (seller != null && seller.isOnline()) {
                ItemStack returnItem = listing.getItem().clone();
                returnItem.setAmount(listing.getAmount());
                giveOrDrop(seller, returnItem);
                seller.sendMessage("§cDein Angebot §e" + id + " §cwurde von einem Admin entfernt. Item zurückgegeben.");
            }
            requester.sendMessage("§aAngebot §e" + id + " §avon §e" + listing.getSellerName() + " §aentfernt.");
        }
        return true;
    }

    /**
     * Processes a purchase. Deducts coins from buyer, pays seller, gives item.
     */
    public boolean buyListing(String id, Player buyer) {
        MarktListing listing = listings.get(id);
        if (listing == null) {
            buyer.sendMessage("§cDieses Angebot existiert nicht mehr.");
            return false;
        }
        if (listing.getSellerUuid().equals(buyer.getUniqueId())) {
            buyer.sendMessage("§cDu kannst dein eigenes Angebot nicht kaufen.");
            return false;
        }

        double price = listing.getPrice();
        if (!plugin.getEconomyManager().has(buyer.getUniqueId(), price)) {
            buyer.sendMessage("§cDu hast nicht genug Coins. (Benötigt: §6" + formatCoins(price) + " Coins§c)");
            return false;
        }

        // Atomic: withdraw from buyer, credit seller's Markt-Bank
        plugin.getEconomyManager().withdraw(buyer.getUniqueId(), price);
        plugin.getBankManager().addMarktBalance(listing.getSellerUuid(), price);

        // Give item to buyer
        ItemStack give = listing.getItem().clone();
        give.setAmount(listing.getAmount());
        giveOrDrop(buyer, give);

        listings.remove(id);
        save();

        buyer.sendMessage("§aGekauft! §e" + listing.getAmount() + "x §f"
                + itemDisplayName(listing.getItem()) + " §afür §6" + formatCoins(price) + " Coins§a.");

        // Notify seller if online
        Player seller = plugin.getServer().getPlayer(listing.getSellerUuid());
        if (seller != null && seller.isOnline()) {
            seller.sendMessage("§a" + buyer.getName() + " §ahat dein Angebot gekauft: §e"
                    + listing.getAmount() + "x §f" + itemDisplayName(listing.getItem())
                    + " §afür §6" + formatCoins(price) + " Coins§a. §7(→ Markt-Bank in §6/bank§7)");
        }
        return true;
    }

    public Collection<MarktListing> getListings() {
        return Collections.unmodifiableCollection(listings.values());
    }

    public List<MarktListing> getListingsByPlayer(UUID uuid) {
        return listings.values().stream()
                .filter(l -> l.getSellerUuid().equals(uuid))
                .collect(Collectors.toList());
    }

    public MarktListing getListing(String id) {
        return listings.get(id);
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (MarktListing listing : listings.values()) {
            String base = "listings." + listing.getId();
            cfg.set(base + ".seller-uuid", listing.getSellerUuid().toString());
            cfg.set(base + ".seller-name", listing.getSellerName());
            cfg.set(base + ".price", listing.getPrice());
            cfg.set(base + ".amount", listing.getAmount());
            cfg.set(base + ".listed-at", listing.getListedAt());
            try {
                cfg.set(base + ".item", itemToBase64(listing.getItem()));
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not serialize item for listing " + listing.getId(), e);
            }
        }
        File file = new File(plugin.getDataFolder(), FILE);
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save markt.yml", e);
        }
    }

    private void load() {
        File file = new File(plugin.getDataFolder(), FILE);
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("listings");
        if (section == null) return;
        for (String id : section.getKeys(false)) {
            try {
                String base = "listings." + id;
                UUID sellerUuid = UUID.fromString(cfg.getString(base + ".seller-uuid"));
                String sellerName = cfg.getString(base + ".seller-name", "Unbekannt");
                double price = cfg.getDouble(base + ".price");
                int amount = cfg.getInt(base + ".amount");
                long listedAt = cfg.getLong(base + ".listed-at");
                String itemData = cfg.getString(base + ".item");
                if (itemData == null) continue;
                ItemStack item = itemFromBase64(itemData);
                if (item == null) continue;
                listings.put(id, new MarktListing(id, sellerUuid, sellerName, item, amount, price, listedAt));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not load listing " + id, e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Serialisation helpers
    // -------------------------------------------------------------------------

    public static String itemToBase64(ItemStack item) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    public static ItemStack itemFromBase64(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                return (ItemStack) dataInput.readObject();
            }
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    private int countItem(Player player, ItemStack target) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(target)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    private void removeItem(Player player, ItemStack target, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.isSimilar(target)) {
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
                if (stack.getAmount() <= 0) {
                    contents[i] = null;
                }
            }
        }
        player.getInventory().setContents(contents);
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    public static String formatCoins(double amount) {
        if (amount == Math.floor(amount)) {
            return String.format("%,.0f", amount);
        }
        return String.format("%,.2f", amount);
    }

    public static String itemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String name = item.getType().name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static String timeAgo(long epochMillis) {
        long diff = System.currentTimeMillis() - epochMillis;
        long seconds = diff / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "min";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        long days = hours / 24;
        return days + "d";
    }
}
