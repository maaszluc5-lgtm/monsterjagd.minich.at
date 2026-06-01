package at.minich.opserver.enchants;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Handles all active effects of custom enchantments.
 */
public class EnchantListener implements Listener {

    private final OpServerPlugin plugin;
    private final EnchantManager em;
    private final Random random = new Random();

    // Track which players currently have passive effects active
    private final Set<UUID> activePassives = new HashSet<>();

    public EnchantListener(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.em = plugin.getEnchantManager();
    }

    // =========================================================================
    // Combat enchants (EntityDamageByEntityEvent)
    // =========================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        double damage = event.getFinalDamage();

        // --- LIFESTEAL ---
        int lifestealLevel = em.getLevel(weapon, CustomEnchant.LIFESTEAL);
        if (lifestealLevel > 0) {
            double healAmount = damage * (lifestealLevel / 100.0);
            double newHealth = Math.min(attacker.getHealth() + healAmount, attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            attacker.setHealth(newHealth);
        }

        // --- THUNDER ---
        int thunderLevel = em.getLevel(weapon, CustomEnchant.THUNDER);
        if (thunderLevel > 0) {
            double chance = thunderLevel * 0.005; // 0.5% per level
            if (random.nextDouble() < chance) {
                victim.getWorld().strikeLightning(victim.getLocation());
            }
        }

        // --- BURN ---
        int burnLevel = em.getLevel(weapon, CustomEnchant.BURN);
        if (burnLevel > 0) {
            victim.setFireTicks(burnLevel * 20); // level = seconds
        }

        // --- POISON ---
        int poisonLevel = em.getLevel(weapon, CustomEnchant.POISON);
        if (poisonLevel > 0) {
            int durationTicks = poisonLevel * 20;
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, durationTicks, 0, false, true));
        }

        // --- EXPLOSIVE ---
        int explosiveLevel = em.getLevel(weapon, CustomEnchant.EXPLOSIVE);
        if (explosiveLevel > 0) {
            float radius = 1.0f + (explosiveLevel * 0.15f);
            victim.getWorld().createExplosion(victim.getLocation(), radius, false, false);
        }

        // --- BERSERKER ---
        int berserkerLevel = em.getLevel(weapon, CustomEnchant.BERSERKER);
        if (berserkerLevel > 0) {
            double maxHp = attacker.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            double currentHp = attacker.getHealth();
            double missingHpRatio = (maxHp - currentHp) / maxHp; // 0 = full, 1 = dead
            // At level 100: up to 2x damage. Scale linearly with level.
            double multiplier = 1.0 + missingHpRatio * (berserkerLevel / 100.0);
            event.setDamage(event.getDamage() * multiplier);
        }

        // --- SHOCKWAVE ---
        int shockwaveLevel = em.getLevel(weapon, CustomEnchant.SHOCKWAVE);
        if (shockwaveLevel > 0) {
            double aoeRadius = 1.5 + (shockwaveLevel * 0.1);
            double aoeDamage = shockwaveLevel * 0.5;
            for (Entity nearby : victim.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
                if (nearby instanceof LivingEntity le && le != attacker && le != victim) {
                    le.damage(aoeDamage, attacker);
                }
            }
        }
    }

    // =========================================================================
    // TITAN — apply/remove max health modifier
    // =========================================================================

    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        // Recalculate passive effects on slot change
        Bukkit.getScheduler().runTask(plugin, () -> applyPassiveEffects(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> applyPassiveEffects(event.getPlayer()));
    }

    /**
     * Apply passive effects (speed, jump, titan health) based on held item.
     */
    public void applyPassiveEffects(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();

        // --- SPEED_BOOST ---
        int speedLevel = em.getLevel(held, CustomEnchant.SPEED_BOOST);
        if (speedLevel > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedLevel - 1, false, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.SPEED);
        }

        // --- JUMP_BOOST ---
        int jumpLevel = em.getLevel(held, CustomEnchant.JUMP_BOOST);
        if (jumpLevel > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, jumpLevel - 1, false, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        }

        // --- TITAN — scan all armor + held item ---
        applyTitanHealth(player);
    }

    private void applyTitanHealth(Player player) {
        int totalTitan = 0;
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece != null) totalTitan += em.getLevel(piece, CustomEnchant.TITAN);
        }
        totalTitan += em.getLevel(player.getInventory().getItemInMainHand(), CustomEnchant.TITAN);

        org.bukkit.attribute.AttributeInstance attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attr == null) return;

        // Remove existing titan modifier
        attr.getModifiers().stream()
            .filter(m -> m.key().toString().contains("titan"))
            .forEach(attr::removeModifier);

        if (totalTitan > 0) {
            double bonus = totalTitan * 0.5;
            org.bukkit.attribute.AttributeModifier mod = new org.bukkit.attribute.AttributeModifier(
                new NamespacedKey(plugin, "titan"),
                bonus,
                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
            );
            attr.addModifier(mod);
        }
    }

    // =========================================================================
    // Block enchants
    // =========================================================================

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // --- AUTOSMELT ---
        if (em.hasEnchant(tool, CustomEnchant.AUTOSMELT)) {
            handleAutosmelt(event);
        }

        // --- VEINMINER ---
        int veinLevel = em.getLevel(tool, CustomEnchant.VEINMINER);
        if (veinLevel > 0) {
            handleVeinminer(event, veinLevel);
        }

        // --- WEALTH ---
        int wealthLevel = em.getLevel(tool, CustomEnchant.WEALTH);
        if (wealthLevel > 0) {
            handleWealth(event, wealthLevel);
        }

        // --- EXPERIENCE ---
        int expLevel = em.getLevel(tool, CustomEnchant.EXPERIENCE);
        if (expLevel > 0) {
            int bonusXp = (int) (event.getExpToDrop() * (1.0 + expLevel * 0.05));
            event.setExpToDrop(bonusXp);
        }
    }

    private void handleAutosmelt(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material smeltedResult = getSmeltedResult(block.getType());
        if (smeltedResult == null) return;

        event.setDropItems(false);
        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(smeltedResult));
    }

    private void handleVeinminer(BlockBreakEvent event, int radius) {
        Block origin = event.getBlock();
        Material targetType = origin.getType();
        if (!isOre(targetType)) return;

        Player player = event.getPlayer();
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(origin);
        visited.add(origin);

        int limit = (int) Math.pow(radius * 2 + 1, 3); // rough cap
        int broken = 0;

        while (!queue.isEmpty() && broken < limit) {
            Block current = queue.poll();
            if (current != origin) {
                current.breakNaturally(player.getInventory().getItemInMainHand());
                broken++;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block neighbor = current.getRelative(dx, dy, dz);
                        if (!visited.contains(neighbor) && neighbor.getType() == targetType
                                && neighbor.getLocation().distance(origin.getLocation()) <= radius + 1) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
    }

    private void handleWealth(BlockBreakEvent event, int wealthLevel) {
        Block block = event.getBlock();
        if (!isOre(block.getType())) return;

        // Extra drops are spawned after the block drops naturally
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Collection<ItemStack> naturalDrops = block.getDrops(tool);
        int extraDropCount = wealthLevel; // each level = 1 extra drop set

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int i = 0; i < extraDropCount; i++) {
                for (ItemStack drop : naturalDrops) {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop.clone());
                }
            }
        });
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private boolean isOre(Material mat) {
        String name = mat.name();
        return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS");
    }

    private Material getSmeltedResult(Material mat) {
        return switch (mat) {
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> Material.GOLD_INGOT;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
            case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
            case SAND -> Material.GLASS;
            case COBBLESTONE -> Material.STONE;
            case NETHERRACK -> Material.NETHER_BRICK;
            case RAW_IRON -> Material.IRON_INGOT;
            case RAW_GOLD -> Material.GOLD_INGOT;
            case RAW_COPPER -> Material.COPPER_INGOT;
            default -> null;
        };
    }
}
