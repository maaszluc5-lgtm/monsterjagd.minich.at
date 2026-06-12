package at.minich.opserver.perks;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PerkListener implements Listener {

    private final OpServerPlugin plugin;
    private final PerkManager perkManager;
    private final Random random = new Random();

    public PerkListener(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.perkManager = plugin.getPerkManager();
        startEffectTask();
    }

    private void startEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    applyPotionPerks(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 60L); // every 3 seconds
    }

    private void applyPotionPerks(Player player) {
        UUID uuid = player.getUniqueId();

        if (has(uuid, Perk.NIGHT_VISION)) apply(player, PotionEffectType.NIGHT_VISION, 100, 0);
        if (has(uuid, Perk.SPEED_BOOST)) apply(player, PotionEffectType.SPEED, 100, 0);
        if (has(uuid, Perk.SPEED_BOOST_2)) apply(player, PotionEffectType.SPEED, 100, 1);
        if (has(uuid, Perk.JUMP_BOOST)) apply(player, PotionEffectType.JUMP_BOOST, 100, 0);
        if (has(uuid, Perk.SUPER_JUMP)) apply(player, PotionEffectType.JUMP_BOOST, 100, 2);
        if (has(uuid, Perk.WATER_BREATHING)) apply(player, PotionEffectType.WATER_BREATHING, 100, 0);
        if (has(uuid, Perk.FIRE_RESISTANCE)) apply(player, PotionEffectType.FIRE_RESISTANCE, 100, 0);
        if (has(uuid, Perk.REGENERATION)) apply(player, PotionEffectType.REGENERATION, 100, 0);
        if (has(uuid, Perk.STRENGTH)) apply(player, PotionEffectType.STRENGTH, 100, 0);
        if (has(uuid, Perk.RESISTANCE)) apply(player, PotionEffectType.RESISTANCE, 100, 0);
        if (has(uuid, Perk.GLIDE)) apply(player, PotionEffectType.SLOW_FALLING, 100, 0);
        if (has(uuid, Perk.FAST_SWIMMER)) apply(player, PotionEffectType.DOLPHINS_GRACE, 100, 0);
        if (has(uuid, Perk.GOD_APPLE_EFFECT)) apply(player, PotionEffectType.ABSORPTION, 100, 3);
        if (has(uuid, Perk.FAST_MINER)) apply(player, PotionEffectType.HASTE, 100, 1);

        // Night/Day miner
        boolean isDay = player.getWorld().getTime() < 13000;
        if (has(uuid, Perk.SOLAR_MINER) && isDay) apply(player, PotionEffectType.HASTE, 100, 2);
        if (has(uuid, Perk.NIGHT_MINER) && !isDay) apply(player, PotionEffectType.HASTE, 100, 2);

        // Fly perk
        if (has(uuid, Perk.FLY) && !player.isOp()) {
            player.setAllowFlight(true);
        }

        // Always full health
        if (has(uuid, Perk.ALWAYS_FULL_HEALTH) && player.getHealth() < 4.0) {
            apply(player, PotionEffectType.INSTANT_HEALTH, 1, 1);
        }

        // Invisibility on sneak
        if (has(uuid, Perk.INVISIBILITY_ON_SNEAK) && player.isSneaking()) {
            apply(player, PotionEffectType.INVISIBILITY, 100, 0);
        }

        // Magnet perks
        int magnetRadius = 0;
        if (has(uuid, Perk.MAGNET_LARGE)) magnetRadius = 8;
        else if (has(uuid, Perk.MAGNET_SMALL)) magnetRadius = 4;
        if (magnetRadius > 0) {
            final int radius = magnetRadius;
            player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius).forEach(e -> {
                if (e instanceof org.bukkit.entity.Item item) {
                    if (player.getInventory().firstEmpty() != -1) {
                        item.teleport(player.getLocation());
                    }
                }
            });
        }
    }

    private void apply(Player player, PotionEffectType type, int duration, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
    }

    private boolean has(UUID uuid, Perk perk) {
        return perkManager.hasPerk(uuid, perk);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        if (has(uuid, Perk.KEEP_INVENTORY)) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
            player.sendMessage("§a✔ §7Dein §eInventar behalten §7Perk hat aktiviert!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Auto smelt
        if (has(uuid, Perk.AUTO_SMELT)) {
            Material smelt = getSmeltResult(event.getBlock().getType());
            if (smelt != null) {
                event.setDropItems(false);
                player.getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(smelt));
            }
        }

        // Double drops
        if (has(uuid, Perk.DOUBLE_DROPS) && random.nextInt(100) < 30) {
            List<ItemStack> drops = new ArrayList<>(event.getBlock().getDrops(player.getInventory().getItemInMainHand()));
            for (ItemStack drop : drops) {
                player.getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
            }
        }

        // Telekinesis
        if (has(uuid, Perk.TELEKINESIS)) {
            event.setDropItems(false);
            List<ItemStack> drops = new ArrayList<>(event.getBlock().getDrops(player.getInventory().getItemInMainHand()));
            for (ItemStack drop : drops) {
                player.getInventory().addItem(drop).values().forEach(
                    leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover)
                );
            }
        }

        // Tree feller
        if (has(uuid, Perk.TREE_FELLER)) {
            Material mat = event.getBlock().getType();
            if (mat.name().endsWith("_LOG")) {
                fellTree(player, event.getBlock(), 0);
            }
        }
    }

    private void fellTree(Player player, org.bukkit.block.Block block, int depth) {
        if (depth > 50) return;
        Material mat = block.getType();
        if (!mat.name().endsWith("_LOG") && !mat.name().endsWith("_LEAVES")) return;
        List<ItemStack> drops = new ArrayList<>(block.getDrops(player.getInventory().getItemInMainHand()));
        block.setType(Material.AIR);
        for (ItemStack drop : drops) {
            player.getInventory().addItem(drop).values().forEach(
                l -> player.getWorld().dropItemNaturally(player.getLocation(), l)
            );
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    org.bukkit.block.Block neighbor = block.getRelative(dx, dy, dz);
                    if (neighbor.getType().name().endsWith("_LOG")) {
                        fellTree(player, neighbor, depth + 1);
                    }
                }
            }
        }
    }

    private Material getSmeltResult(Material mat) {
        return switch (mat) {
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> Material.GOLD_INGOT;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
            case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
            case COAL_ORE, DEEPSLATE_COAL_ORE -> Material.COAL;
            case SAND -> Material.GLASS;
            case COBBLESTONE -> Material.STONE;
            default -> null;
        };
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevel(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (has(player.getUniqueId(), Perk.NO_HUNGER) && event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        if (!has(player.getUniqueId(), Perk.DOUBLE_FISHING)) return;
        if (event.getCaught() instanceof org.bukkit.entity.Item item) {
            ItemStack extra = item.getItemStack().clone();
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        UUID uuid = killer.getUniqueId();
        if (has(uuid, Perk.COIN_MAGNET)) {
            plugin.getEconomyManager().deposit(uuid, 5.0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (has(uuid, Perk.NO_FALL_DAMAGE)) {
            player.setFallDistance(0f);
        }
    }
}
