package at.minich.opserver.items;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.ranks.RankManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityItemListener implements Listener {

    private final OpServerPlugin plugin;
    private final NamespacedKey abilityKey;
    private final NamespacedKey expiryKey;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public AbilityItemListener(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.abilityKey = new NamespacedKey(plugin, "ability_item");
        this.expiryKey  = new NamespacedKey(plugin, "ability_expiry");
    }

    public NamespacedKey getAbilityKey() { return abilityKey; }
    public NamespacedKey getExpiryKey()  { return expiryKey; }

    // On login: remove any expired ability items and disable fly if needed
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean hadFlyItem = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isExpired(item)) {
                String id = getAbilityId(item);
                if ("fly".equals(id)) hadFlyItem = true;
                item.setAmount(0); // remove
            }
        }
        if (hadFlyItem && !player.isOp()) {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage("§c✦ Dein Flug-Kristall ist abgelaufen und wurde entfernt.");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        String abilityId = getAbilityId(item);
        if (abilityId == null) return;

        event.setCancelled(true);

        // Expiry check
        if (isExpired(item)) {
            String id = abilityId;
            item.setAmount(0);
            if ("fly".equals(id) && !player.isOp()) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
            player.sendMessage("§c✦ Dieses Ability Item ist abgelaufen und wurde entfernt!");
            return;
        }

        AbilityItem ability = AbilityItem.fromId(abilityId);
        if (ability == null) return;

        // Rank check
        RankManager rm = plugin.getRankManager();
        RankManager.RankInfo rank = rm.getRank(player.getUniqueId());
        int rankIndex = rm.getRankIndex(rank);
        if (rankIndex < ability.requiredRankIndex()) {
            String required = ability.requiredRankIndex() == 3 ? "§6Elite" : "§cLegende";
            player.sendMessage("§cDu benötigst mindestens Rang " + required + " für dieses Item!");
            return;
        }

        // Cooldown check
        long now = System.currentTimeMillis();
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        Long lastUse = cooldowns.get(player.getUniqueId()).get(abilityId);
        long cooldownMs = getCooldownMs(abilityId);
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage("§cNoch " + remaining + "s Cooldown!");
            return;
        }

        switch (abilityId) {
            case "fly" -> {
                boolean flying = !player.getAllowFlight();
                player.setAllowFlight(flying);
                if (!flying) player.setFlying(false);
                player.sendMessage(flying ? "§b✦ Fliegen §aaktiviert!" : "§b✦ Fliegen §cdeaktiviert!");
            }
            case "heal" -> {
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setSaturation(20f);
                player.sendMessage("§a✦ Leben aufgefüllt!");
                cooldowns.get(player.getUniqueId()).put(abilityId, now);
            }
            case "speed" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1, false, true, true));
                player.sendMessage("§e✦ Geschwindigkeit für 60s aktiv!");
                cooldowns.get(player.getUniqueId()).put(abilityId, now);
            }
            case "god" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 30, 4, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 30, 3, false, true, true));
                player.sendMessage("§4✦ Gottes Schutz für 30s aktiv!");
                cooldowns.get(player.getUniqueId()).put(abilityId, now);
            }
        }
    }

    private String getAbilityId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(abilityKey, PersistentDataType.STRING);
    }

    private boolean isExpired(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(expiryKey, PersistentDataType.LONG)) return false;
        long expiresAt = pdc.get(expiryKey, PersistentDataType.LONG);
        return System.currentTimeMillis() > expiresAt;
    }

    private long getCooldownMs(String abilityId) {
        return switch (abilityId) {
            case "heal"  -> 30_000L;
            case "speed" -> 45_000L;
            case "god"   -> 120_000L;
            default      -> 0L;
        };
    }
}
