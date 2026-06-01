package at.minich.opserver.salaryfarm;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages the Lohnfarm (salary farm) world.
 * Players earn 3x normal salary while in this world.
 * On first creation a 50x50 farm area is generated around spawn.
 */
public class SalaryFarmManager {

    private static final String WORLD_NAME = "lohnfarm";

    private final OpServerPlugin plugin;
    private World lohnfarmWorld;

    public SalaryFarmManager(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    public void initialize() {
        lohnfarmWorld = Bukkit.getWorld(WORLD_NAME);
        boolean justCreated = lohnfarmWorld == null;
        if (justCreated) {
            lohnfarmWorld = createWorld();
            if (lohnfarmWorld != null) {
                // Build farm one tick later so chunks are loaded
                final World world = lohnfarmWorld;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        buildFarm(world);
                        spawnVillagers(world);
                    }
                }.runTaskLater(plugin, 40L);
            }
        }

        // Start action-bar task for players in lohnfarm
        startActionBarTask();
    }

    private World createWorld() {
        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        return Bukkit.createWorld(creator);
    }

    // -------------------------------------------------------------------------
    // Farm construction (50x50 centred at spawn, at y=4 for flat world)
    // -------------------------------------------------------------------------

    private void buildFarm(World world) {
        int centerX = 0;
        int centerZ = 0;
        int radius  = 25; // half of 50
        int baseY   = 4;  // flat world ground

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                boolean onBorder = (x == centerX - radius || x == centerX + radius
                        || z == centerZ - radius || z == centerZ + radius);

                Block base = world.getBlockAt(x, baseY, z);
                Block top  = world.getBlockAt(x, baseY + 1, z);

                if (onBorder) {
                    base.setType(Material.STONE_BRICKS);
                    top.setType(Material.AIR);
                } else {
                    // 4 water sources in the "corners" of the inner field
                    boolean isWaterCorner =
                            (Math.abs(x - (centerX - radius + 5)) <= 1 && Math.abs(z - (centerZ - radius + 5)) <= 1) ||
                            (Math.abs(x - (centerX + radius - 5)) <= 1 && Math.abs(z - (centerZ - radius + 5)) <= 1) ||
                            (Math.abs(x - (centerX - radius + 5)) <= 1 && Math.abs(z - (centerZ + radius - 5)) <= 1) ||
                            (Math.abs(x - (centerX + radius - 5)) <= 1 && Math.abs(z - (centerZ + radius - 5)) <= 1);

                    if (isWaterCorner) {
                        base.setType(Material.DIRT);
                        top.setType(Material.WATER);
                    } else {
                        base.setType(Material.FARMLAND);
                        // Place a random crop on farmland
                        Material crop = randomCrop(x, z);
                        top.setType(crop);
                    }
                }
            }
        }
    }

    private Material randomCrop(int x, int z) {
        int hash = Math.abs(x * 31 + z * 17) % 4;
        return switch (hash) {
            case 0 -> Material.WHEAT;
            case 1 -> Material.CARROTS;
            case 2 -> Material.POTATOES;
            default -> Material.BEETROOTS;
        };
    }

    private void spawnVillagers(World world) {
        Location spawn = world.getSpawnLocation();
        double x = spawn.getX();
        double y = spawn.getY();
        double z = spawn.getZ();

        String[] names = {"§eFarmer Hans", "§eFarmer Maria", "§eFarmer Klaus", "§eFarmer Lisa"};
        double[] offsets = {2, -2, 0, 0};
        double[] zOff    = {0,  0, 2, -2};

        for (int i = 0; i < 4; i++) {
            Location loc = new Location(world, x + offsets[i], y, z + zOff[i]);
            Villager villager = (Villager) world.spawnEntity(loc, EntityType.VILLAGER);
            villager.setCustomName(names[i]);
            villager.setCustomNameVisible(true);
            villager.setAI(false);
            villager.setInvulnerable(true);
        }
    }

    // -------------------------------------------------------------------------
    // Action-bar task
    // -------------------------------------------------------------------------

    private void startActionBarTask() {
        double base = plugin.getConfig().getDouble("salary.coins-per-minute", 10.0);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().getName().equals(WORLD_NAME)) {
                        double multiplier = plugin.getRankManager().getSalaryMultiplier(p.getUniqueId());
                        double amount = base * multiplier * 3.0;
                        p.spigot().sendMessage(
                                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(
                                        "§6+" + String.format("%.0f", amount)
                                                + " Coins §7pro Minute (§a3x Bonus§7)"));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 5, 20L * 5); // every 5 seconds
    }

    // -------------------------------------------------------------------------
    // Teleport
    // -------------------------------------------------------------------------

    public void teleportPlayer(Player player) {
        if (lohnfarmWorld == null) {
            initialize();
        }
        if (lohnfarmWorld == null) {
            player.sendMessage("§cDie Lohnfarm-Welt konnte nicht geladen werden.");
            return;
        }
        Location spawn = lohnfarmWorld.getSpawnLocation();
        spawn.setY(lohnfarmWorld.getHighestBlockYAt(spawn) + 1);
        player.teleport(spawn);
        player.sendMessage("§6§lLohnfarm §r§7— Du wurdest zur Lohnfarm teleportiert! §a(3x Lohn)");
    }

    // -------------------------------------------------------------------------

    public World getWorld() {
        return lohnfarmWorld;
    }

    public String getWorldName() {
        return WORLD_NAME;
    }
}
