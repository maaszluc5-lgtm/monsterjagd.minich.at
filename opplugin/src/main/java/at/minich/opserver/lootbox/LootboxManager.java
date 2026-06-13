package at.minich.opserver.lootbox;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.UUID;

public class LootboxManager {

    private final OpServerPlugin plugin;
    private final Random rng = new Random();

    public LootboxManager(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    private String path(UUID uuid) { return "lootboxes/" + uuid + ".yml"; }

    public int getCount(UUID uuid, LootboxType type) {
        YamlConfiguration cfg = plugin.getDataManager().loadYaml(path(uuid));
        return cfg.getInt(type.name(), 0);
    }

    public void addBox(UUID uuid, LootboxType type, int amount) {
        YamlConfiguration cfg = plugin.getDataManager().loadYaml(path(uuid));
        cfg.set(type.name(), cfg.getInt(type.name(), 0) + amount);
        plugin.getDataManager().saveYaml(cfg, path(uuid));
    }

    private boolean removeBox(UUID uuid, LootboxType type) {
        YamlConfiguration cfg = plugin.getDataManager().loadYaml(path(uuid));
        int count = cfg.getInt(type.name(), 0);
        if (count <= 0) return false;
        cfg.set(type.name(), count - 1);
        plugin.getDataManager().saveYaml(cfg, path(uuid));
        return true;
    }

    public boolean buyBox(Player player, LootboxType type) {
        UUID uuid = player.getUniqueId();
        if (!plugin.getEconomyManager().has(uuid, type.cost)) {
            player.sendMessage("§cNicht genug Coins! Du brauchst §e" + (int) type.cost + " Coins§c.");
            return false;
        }
        plugin.getEconomyManager().withdraw(uuid, type.cost);
        addBox(uuid, type, 1);
        player.sendMessage("§aDu hast " + type.displayName + " §agekauft für §e" + (int) type.cost + " Coins§a.");
        return true;
    }

    public boolean openBox(Player player, LootboxType type) {
        UUID uuid = player.getUniqueId();
        if (!removeBox(uuid, type)) {
            player.sendMessage("§cDu hast keine " + type.displayName + "§c.");
            return false;
        }
        LootboxReward reward = roll(type);
        if (reward.type() == LootboxReward.RewardType.COINS) {
            plugin.getEconomyManager().deposit(uuid, reward.amount());
            player.sendMessage("§6🎁 " + type.displayName + " §6geöffnet! Du erhältst §e" + (int) reward.amount() + " Coins§6!");
        } else {
            ItemStack item = new ItemStack(reward.material(), (int) reward.amount());
            player.getInventory().addItem(item);
            player.sendMessage("§6🎁 " + type.displayName + " §6geöffnet! Du erhältst §e" + (int) reward.amount() + "x " + reward.material().name() + "§6!");
        }
        return true;
    }

    private LootboxReward roll(LootboxType type) {
        switch (type) {
            case COMMON -> {
                if (rng.nextBoolean()) {
                    double coins = 100 + rng.nextInt(901);
                    return new LootboxReward(LootboxReward.RewardType.COINS, coins, null);
                } else {
                    Material[] mats = {Material.IRON_INGOT, Material.GOLD_INGOT, Material.COAL};
                    Material mat = mats[rng.nextInt(mats.length)];
                    int amt = 1 + rng.nextInt(16);
                    return new LootboxReward(LootboxReward.RewardType.ITEM, amt, mat);
                }
            }
            case RARE -> {
                if (rng.nextBoolean()) {
                    double coins = 500 + rng.nextInt(4501);
                    return new LootboxReward(LootboxReward.RewardType.COINS, coins, null);
                } else {
                    Material[] mats = {Material.DIAMOND, Material.EMERALD, Material.LAPIS_LAZULI};
                    Material mat = mats[rng.nextInt(mats.length)];
                    int amt = 1 + rng.nextInt(8);
                    return new LootboxReward(LootboxReward.RewardType.ITEM, amt, mat);
                }
            }
            case LEGENDARY -> {
                if (rng.nextBoolean()) {
                    double coins = 2000 + rng.nextInt(18001);
                    return new LootboxReward(LootboxReward.RewardType.COINS, coins, null);
                } else {
                    Material[] mats = {Material.NETHERITE_INGOT, Material.NETHERITE_SCRAP, Material.ANCIENT_DEBRIS};
                    Material mat = mats[rng.nextInt(mats.length)];
                    int amt = 1 + rng.nextInt(4);
                    return new LootboxReward(LootboxReward.RewardType.ITEM, amt, mat);
                }
            }
            default -> { return new LootboxReward(LootboxReward.RewardType.COINS, 100, null); }
        }
    }
}
