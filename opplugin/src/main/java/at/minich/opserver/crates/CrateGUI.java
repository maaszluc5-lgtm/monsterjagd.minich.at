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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CrateGUI {

    private final OpServerPlugin plugin;
    private final Player player;
    private final CrateType crateType;
    private final CrateReward reward;

    private Inventory inventory;
    private boolean finished = false;

    // Layout (27 slots, 3 rows):
    //   Row 0 (0-8):  border, slot 4 = fixed arrow pointing DOWN ▼
    //   Row 1 (9-17): spinning items — all 9 slots
    //   Row 2 (18-26): border, slot 22 = fixed arrow pointing UP ▲
    // Winner = whatever lands at slot 13 (center of row 1)

    private static final int[] STRIP_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int CENTER_SLOT   = 13;
    private static final int ARROW_TOP     = 4;
    private static final int ARROW_BOTTOM  = 22;

    public CrateGUI(OpServerPlugin plugin, Player player, CrateType crateType) {
        this.plugin = plugin;
        this.player = player;
        this.crateType = crateType;
        this.reward = plugin.getCrateManager().pickReward(crateType);
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, crateType.getDisplayName());

        // Static borders
        ItemStack gray = pane(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack arrow = pane(Material.YELLOW_STAINED_GLASS_PANE, "§e§l▼ HIER GEWINNT DU ▼");
        ItemStack arrowB = pane(Material.YELLOW_STAINED_GLASS_PANE, "§e§l▲ HIER GEWINNT DU ▲");

        for (int i = 0; i < 9; i++)    inventory.setItem(i, gray);
        for (int i = 18; i < 27; i++)  inventory.setItem(i, gray);
        inventory.setItem(ARROW_TOP, arrow);
        inventory.setItem(ARROW_BOTTOM, arrowB);

        // Fill spin strip with random items before opening
        List<CrateReward> all = plugin.getCrateManager().getRewards(crateType);
        fillStrip(all);

        player.openInventory(inventory);
        startAnimation();
    }

    public Inventory getInventory() { return inventory; }
    public boolean isFinished()     { return finished; }

    private void startAnimation() {
        List<CrateReward> allRewards = plugin.getCrateManager().getRewards(crateType);
        if (allRewards.isEmpty()) { doFinalize(); return; }

        new BukkitRunnable() {
            int tick = 0;
            // Fast phase: 36 ticks total, spin every 2 ticks = 18 frames
            // Slow phase: 30 ticks total, spin every 6 ticks = 5 frames
            final int FAST_END = 36;
            final int SLOW_END = 66;

            @Override
            public void run() {
                if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                    cancel(); return;
                }

                if (tick < FAST_END && tick % 2 == 0) {
                    fillStrip(allRewards);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
                } else if (tick >= FAST_END && tick < SLOW_END && (tick - FAST_END) % 6 == 0) {
                    fillStrip(allRewards);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.9f);
                }

                tick++;

                if (tick >= SLOW_END) {
                    cancel();
                    // Place winner at center with glowing lore
                    inventory.setItem(CENTER_SLOT, buildWinner(reward));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    // Close and give reward after short pause
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                player.closeInventory();
                                doFinalize();
                            }
                        }
                    }.runTaskLater(plugin, 40L);
                }
            }
        }.runTaskTimer(plugin, 5L, 1L);
    }

    /** Fills all 9 strip slots with random reward items (center gets random too during spin). */
    private void fillStrip(List<CrateReward> all) {
        Random rng = new Random();
        for (int slot : STRIP_SLOTS) {
            CrateReward r = all.get(rng.nextInt(all.size()));
            inventory.setItem(slot, buildItem(r));
        }
    }

    /** Normal item display during spin. */
    private ItemStack buildItem(CrateReward r) {
        Material mat = r.getDisplayMaterial() != null ? r.getDisplayMaterial() : Material.PAPER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(r.getDisplayName());
        meta.setLore(Arrays.asList("§8" + crateType.getDisplayName()));
        item.setItemMeta(meta);
        return item;
    }

    /** Winner display — extra lore, highlighted. */
    private ItemStack buildWinner(CrateReward r) {
        Material mat = r.getDisplayMaterial() != null ? r.getDisplayMaterial() : Material.PAPER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§a§l✦ " + r.getDisplayName() + " §a§l✦");
        meta.setLore(Arrays.asList("§7Du hast gewonnen!", "§8" + crateType.getDisplayName()));
        item.setItemMeta(meta);
        return item;
    }

    private void doFinalize() {
        finished = true;
        if (!player.isOnline()) return;

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        switch (reward.getType()) {
            case COINS -> {
                plugin.getEconomyManager().deposit(player.getUniqueId(), reward.getAmount());
                player.sendMessage(prefix + "§6✦ Du hast §e" + (long) reward.getAmount() + " Coins §6aus der Kiste gewonnen!");
            }
            case CRYSTALS -> {
                plugin.getCrystalManager().addCrystals(player.getUniqueId(), (long) reward.getAmount());
                player.sendMessage(prefix + "§b✦ Du hast §3" + (long) reward.getAmount() + " Kristalle §baus der Kiste gewonnen!");
            }
            case ITEM -> {
                ItemStack item = new ItemStack(reward.getMaterial(), (int) Math.max(1, reward.getAmount()));
                player.getInventory().addItem(item);
                player.sendMessage(prefix + "§a✦ Du hast §f" + reward.getDisplayName() + " §aaus der Kiste gewonnen!");
            }
            case CUSTOM_ITEM -> {
                CustomItems ci = CustomItems.fromString(reward.getCustomItemId());
                if (ci != null) {
                    ItemStack item = ci.build(plugin.getEnchantManager());
                    player.getInventory().addItem(item);
                    player.sendMessage(prefix + "§a✦ Du hast §6" + ci.getDisplayName() + " §aaus der Kiste gewonnen!");
                }
            }
        }

        // Server-wide broadcast for legendary wins
        if (crateType == CrateType.LEGENDARY) {
            Bukkit.broadcastMessage("§6§l✦ " + player.getName() + " §ehat aus einer §6Legendären Kiste §egewonnen: §f" + reward.getDisplayName() + " §6§l✦");
        }
    }

    private ItemStack pane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }
}
