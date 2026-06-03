package at.minich.opserver.crates;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.items.CustomItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrateGUI {

    private final OpServerPlugin plugin;
    private final Player player;
    private final CrateType crateType;
    private final CrateReward reward;

    private Inventory inventory;
    private boolean finished = false;

    // Slots 9-17 are the spin strip (middle row of a 27-slot 3x9 inventory)
    private static final int[] STRIP_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int CENTER_SLOT = 13;

    public CrateGUI(OpServerPlugin plugin, Player player, CrateType crateType) {
        this.plugin = plugin;
        this.player = player;
        this.crateType = crateType;
        this.reward = plugin.getCrateManager().pickReward(crateType);
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, crateType.getDisplayName());

        // Fill top and bottom rows with glass panes
        ItemStack filler = buildPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 18; i < 27; i++) inventory.setItem(i, filler);

        // Highlight center slot
        inventory.setItem(CENTER_SLOT, buildPane(Material.YELLOW_STAINED_GLASS_PANE, "§e▼ Gewinn ▼"));

        player.openInventory(inventory);

        startAnimation();
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean isFinished() {
        return finished;
    }

    private void startAnimation() {
        List<CrateReward> allRewards = plugin.getCrateManager().getRewards(crateType);
        if (allRewards.isEmpty()) {
            finalize(reward);
            return;
        }

        // Phase 1: fast spin — 20 ticks, every 2 ticks = 10 frames
        // Phase 2: slow spin — 10 ticks, every 5 ticks = 2 frames
        // Then stop

        new BukkitRunnable() {
            int tick = 0;
            final int FAST_TICKS = 40;  // 20 * 2
            final int SLOW_TICKS = 20;  // 4 * 5

            @Override
            public void run() {
                if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                    cancel();
                    return;
                }

                boolean isFast = tick < FAST_TICKS;
                boolean isSlow = tick >= FAST_TICKS && tick < FAST_TICKS + SLOW_TICKS;

                if (isFast && tick % 2 == 0) {
                    spinStrip(allRewards);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                } else if (isSlow && (tick - FAST_TICKS) % 5 == 0) {
                    spinStrip(allRewards);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
                }

                tick++;

                if (tick >= FAST_TICKS + SLOW_TICKS) {
                    cancel();
                    // Place the winning reward at center and finalize
                    inventory.setItem(CENTER_SLOT, buildRewardDisplay(reward));
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                player.closeInventory();
                                finalize(reward);
                            }
                        }
                    }.runTaskLater(plugin, 30L);
                }
            }
        }.runTaskTimer(plugin, 5L, 1L);
    }

    private void spinStrip(List<CrateReward> allRewards) {
        Random rng = new Random();
        for (int slot : STRIP_SLOTS) {
            if (slot == CENTER_SLOT) continue;
            CrateReward r = allRewards.get(rng.nextInt(allRewards.size()));
            inventory.setItem(slot, buildRewardDisplay(r));
        }
        // Center shows a random one during spin too
        CrateReward centerRandom = allRewards.get(rng.nextInt(allRewards.size()));
        inventory.setItem(CENTER_SLOT, buildRewardDisplay(centerRandom));
    }

    private void finalize(CrateReward won) {
        finished = true;
        if (!player.isOnline()) return;

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        switch (won.getType()) {
            case COINS -> {
                plugin.getEconomyManager().deposit(player.getUniqueId(), won.getAmount());
                player.sendMessage(prefix + "§6Du hast §e" + won.getAmount() + " Coins §6gewonnen!");
            }
            case CRYSTALS -> {
                plugin.getCrystalManager().addCrystals(player.getUniqueId(), won.getAmount());
                player.sendMessage(prefix + "§bDu hast §3" + won.getAmount() + " Kristalle §bgewonnen!");
            }
            case ITEM -> {
                ItemStack item = new ItemStack(won.getMaterial(), (int) Math.max(1, won.getAmount()));
                player.getInventory().addItem(item);
                player.sendMessage(prefix + "§aDu hast §f" + won.getDisplayName() + " §agewonnen!");
            }
            case CUSTOM_ITEM -> {
                CustomItems ci = CustomItems.fromString(won.getCustomItemId());
                if (ci != null) {
                    ItemStack item = ci.build(plugin.getEnchantManager());
                    player.getInventory().addItem(item);
                    player.sendMessage(prefix + "§aDu hast §6" + ci.getDisplayName() + " §agewonnen!");
                } else {
                    player.sendMessage(prefix + "§cFehler: Unbekanntes Custom Item §f" + won.getCustomItemId());
                }
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    private ItemStack buildRewardDisplay(CrateReward r) {
        ItemStack item = new ItemStack(r.getDisplayMaterial() != null ? r.getDisplayMaterial() : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(r.getDisplayName());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
