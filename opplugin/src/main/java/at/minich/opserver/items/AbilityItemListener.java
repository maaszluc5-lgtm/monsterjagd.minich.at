package at.minich.opserver.items;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.ranks.RankManager;
import at.minich.opserver.util.DataManager;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityItemListener implements Listener {

    private static final long THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000;

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

    // On login: check if fly time has expired
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;
        long flyExpiry = getFlyExpiry(player.getUniqueId());
        if (flyExpiry > 0 && System.currentTimeMillis() > flyExpiry) {
            player.setAllowFlight(false);
            player.setFlying(false);
            setFlyExpiry(player.getUniqueId(), 0);
            player.sendMessage("§c✦ Dein Flug-Kristall ist abgelaufen. Fliegen wurde deaktiviert.");
        } else if (flyExpiry > 0) {
            // Restore fly for players who still have time left
            player.setAllowFlight(true);
            long remaining = (flyExpiry - System.currentTimeMillis()) / 1000 / 3600;
            player.sendMessage("§b✦ Fliegen aktiv, noch §f" + remaining + "h §bübrig.");
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

        String abilityId = item.getItemMeta().getPersistentDataContainer()
                .get(abilityKey, PersistentDataType.STRING);
        if (abilityId == null) return;

        event.setCancelled(true);

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

        long now = System.currentTimeMillis();

        if ("fly".equals(abilityId)) {
            // Consume item and grant 3-day fly
            item.setAmount(item.getAmount() - 1);
            long expiresAt = now + THREE_DAYS_MS;
            setFlyExpiry(player.getUniqueId(), expiresAt);
            player.setAllowFlight(true);
            player.sendMessage("§b✦ Flug-Kristall eingelöst! Du kannst §f3 Tage §bfliegen.");
            return;
        }

        // Cooldown check for other abilities
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        Long lastUse = cooldowns.get(player.getUniqueId()).get(abilityId);
        long cooldownMs = getCooldownMs(abilityId);
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage("§cNoch " + remaining + "s Cooldown!");
            return;
        }

        // Consume item and apply effect
        item.setAmount(item.getAmount() - 1);

        switch (abilityId) {
            case "heal" -> {
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setSaturation(20f);
                player.sendMessage("§a✦ Heilungs-Kristall eingelöst! Leben aufgefüllt.");
                cooldowns.get(player.getUniqueId()).put(abilityId, now);
            }
            case "speed" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1, false, true, true));
                player.sendMessage("§e✦ Geschwindigkeits-Kristall eingelöst! Speed II für 60s.");
                cooldowns.get(player.getUniqueId()).put(abilityId, now);
            }
            case "god" -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 30, 4, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 30, 3, false, true, true));
                player.sendMessage("§4✦ Gottes-Kristall eingelöst! Unverwundbar für 30s.");
                cooldowns.get(player.getUniqueId()).put(abilityId, now);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Fly expiry persistence (stored in player YAML)
    // -------------------------------------------------------------------------

    private long getFlyExpiry(UUID uuid) {
        DataManager dm = plugin.getDataManager();
        YamlConfiguration cfg = dm.loadYaml("players/" + uuid + ".yml");
        return cfg.getLong("fly-expiry", 0L);
    }

    private void setFlyExpiry(UUID uuid, long timestamp) {
        DataManager dm = plugin.getDataManager();
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dm.loadYaml(path);
        cfg.set("fly-expiry", timestamp);
        dm.saveYaml(cfg, path);
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
