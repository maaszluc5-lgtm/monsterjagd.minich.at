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

/**
 * 27-slot crate GUI.
 *
 * Layout:
 *   Row 0 (0-8):   border, slot 4 = arrow ▼
 *   Row 1 (9-17):  spinning strip (center = slot 13)
 *   Row 2 (18-26): collected rewards display
 *                  slot 20 = reward 1, slot 22 = reward 2, slot 24 = reward 3
 *
 * Three sequential spins — each one picks a separate reward.
 * After all 3 spins, player receives all 3 rewards and inventory closes.
 */
public class CrateGUI {

    private static final int[] STRIP     = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int   CENTER    = 13;
    private static final int   ARROW_TOP = 4;
    private static final int[] REWARD_SLOTS = {20, 22, 24};

    private final OpServerPlugin plugin;
    private final Player player;
    private final CrateType crateType;
    private final List<CrateReward> rewards = new ArrayList<>(); // 3 pre-picked rewards

    private Inventory inventory;
    private boolean finished = false;
    private int currentSpin = 0; // 0, 1, 2

    public CrateGUI(OpServerPlugin plugin, Player player, CrateType crateType) {
        this.plugin = plugin;
        this.player = player;
        this.crateType = crateType;
        // Pre-pick all 3 rewards now
        for (int i = 0; i < 3; i++) {
            rewards.add(plugin.getCrateManager().pickReward(crateType));
        }
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, crateType.getDisplayName());
        redrawBorders();
        fillStrip(plugin.getCrateManager().getRewards(crateType));
        player.openInventory(inventory);
        startSpin(0);
    }

    public Inventory getInventory() { return inventory; }
    public boolean isFinished()     { return finished; }

    private void redrawBorders() {
        ItemStack gray   = pane(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack arrow  = pane(Material.YELLOW_STAINED_GLASS_PANE, "§e§l▼ HIER GEWINNST DU ▼");
        ItemStack empty  = pane(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 9; i++)   inventory.setItem(i, gray);
        inventory.setItem(ARROW_TOP, arrow);

        // Bottom row: black except reward slots (18-26)
        for (int i = 18; i < 27; i++) inventory.setItem(i, empty);

        // Reward slot placeholders
        for (int i = 0; i < REWARD_SLOTS.length; i++) {
            inventory.setItem(REWARD_SLOTS[i], pane(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    "§7Belohnung " + (i + 1)));
        }

        // Spin counter label at slot 18
        inventory.setItem(18, pane(Material.LIME_STAINED_GLASS_PANE,
                "§aSpin " + (currentSpin + 1) + " von 3"));
    }

    private void startSpin(int spinIndex) {
        currentSpin = spinIndex;
        // Update spin counter
        inventory.setItem(18, pane(Material.LIME_STAINED_GLASS_PANE,
                "§aSpin " + (spinIndex + 1) + " von 3"));

        List<CrateReward> all = plugin.getCrateManager().getRewards(crateType);
        CrateReward thisReward = rewards.get(spinIndex);

        new BukkitRunnable() {
            int tick = 0;
            final int FAST_END = 30; // fast spin: ticks 0-29, every 2
            final int SLOW_END = 56; // slow spin: ticks 30-55, every 6

            @Override
            public void run() {
                if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                    cancel(); return;
                }

                if (tick < FAST_END && tick % 2 == 0) {
                    fillStrip(all);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.35f, 1.3f);
                } else if (tick >= FAST_END && tick < SLOW_END && (tick - FAST_END) % 6 == 0) {
                    fillStrip(all);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.85f);
                }

                tick++;

                if (tick >= SLOW_END) {
                    cancel();
                    // Show winner at center
                    inventory.setItem(CENTER, buildWinner(thisReward, spinIndex + 1));
                    // Place in collected row
                    inventory.setItem(REWARD_SLOTS[spinIndex], buildCollected(thisReward, spinIndex + 1));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!player.isOnline()) { cancel(); return; }
                            if (spinIndex < 2) {
                                // Start next spin
                                fillStrip(all);
                                startSpin(spinIndex + 1);
                            } else {
                                // All 3 done — give rewards and close
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (player.isOnline()) {
                                            player.closeInventory();
                                            giveAllRewards();
                                        }
                                    }
                                }.runTaskLater(plugin, 40L);
                            }
                        }
                    }.runTaskLater(plugin, 25L);
                }
            }
        }.runTaskTimer(plugin, 5L, 1L);
    }

    private void fillStrip(List<CrateReward> all) {
        Random rng = new Random();
        for (int slot : STRIP) {
            CrateReward r = all.get(rng.nextInt(all.size()));
            inventory.setItem(slot, buildItem(r));
        }
    }

    private void giveAllRewards() {
        finished = true;
        if (!player.isOnline()) return;
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < rewards.size(); i++) {
            CrateReward r = rewards.get(i);
            giveReward(r, prefix);
            if (i > 0) summary.append("§7, ");
            summary.append(r.getDisplayName());
        }

        player.sendMessage(prefix + "§6✦ Kiste geöffnet! Belohnungen: " + summary);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        if (crateType == CrateType.LEGENDARY) {
            Bukkit.broadcastMessage("§6§l✦ " + player.getName()
                    + " §ehat eine §6Legendäre Kiste §egeöffnet! Belohnungen: " + summary + " §6§l✦");
        }
    }

    private void giveReward(CrateReward r, String prefix) {
        switch (r.getType()) {
            case COINS  -> plugin.getEconomyManager().deposit(player.getUniqueId(), r.getAmount());
            case CRYSTALS -> plugin.getCrystalManager().addCrystals(player.getUniqueId(), (long) r.getAmount());
            case ITEM   -> player.getInventory().addItem(new ItemStack(r.getMaterial(), (int) Math.max(1, r.getAmount())));
            case CUSTOM_ITEM -> {
                CustomItems ci = CustomItems.fromString(r.getCustomItemId());
                if (ci != null) player.getInventory().addItem(ci.build(plugin.getEnchantManager()));
            }
        }
    }

    private ItemStack buildItem(CrateReward r) {
        Material mat = r.getDisplayMaterial() != null ? r.getDisplayMaterial() : Material.PAPER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(r.getDisplayName());
        meta.setLore(List.of("§8" + crateType.getDisplayName()));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildWinner(CrateReward r, int num) {
        Material mat = r.getDisplayMaterial() != null ? r.getDisplayMaterial() : Material.PAPER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§a§l✦ " + r.getDisplayName() + " §a§l✦");
        meta.setLore(Arrays.asList("§7Belohnung #" + num, "§8" + crateType.getDisplayName()));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCollected(CrateReward r, int num) {
        Material mat = r.getDisplayMaterial() != null ? r.getDisplayMaterial() : Material.PAPER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§e#" + num + " §f" + r.getDisplayName());
        meta.setLore(List.of("§7Gewonnen!"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }
}
