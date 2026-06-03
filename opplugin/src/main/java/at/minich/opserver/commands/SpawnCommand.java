package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public SpawnCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("setspawn")) {
            if (!sender.hasPermission("opserver.admin")) {
                sender.sendMessage(prefix + plugin.getConfig().getString("messages.no-permission", "§cNo permission."));
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix + "§cOnly players can set the spawn.");
                return true;
            }
            Location loc = player.getLocation();
            FileConfiguration cfg = plugin.getConfig();
            cfg.set("spawn.location.world", loc.getWorld().getName());
            cfg.set("spawn.location.x", loc.getX());
            cfg.set("spawn.location.y", loc.getY());
            cfg.set("spawn.location.z", loc.getZ());
            cfg.set("spawn.location.yaw", (double) loc.getYaw());
            cfg.set("spawn.location.pitch", (double) loc.getPitch());
            plugin.saveConfig();
            sender.sendMessage(prefix + "§aSpawn wurde gesetzt.");
            return true;
        }

        // /spawn
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cOnly players can teleport to spawn.");
            return true;
        }

        Location loc = getSpawnLocation();
        if (loc == null) {
            // Fall back to world spawn
            World world = Bukkit.getWorlds().get(0);
            loc = world.getSpawnLocation();
        }
        player.teleport(loc);
        sender.sendMessage(prefix + "§aTeleportiert zum Spawn.");
        return true;
    }

    private Location getSpawnLocation() {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains("spawn.location")) return null;
        String worldName = cfg.getString("spawn.location.world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x = cfg.getDouble("spawn.location.x");
        double y = cfg.getDouble("spawn.location.y");
        double z = cfg.getDouble("spawn.location.z");
        float yaw = (float) cfg.getDouble("spawn.location.yaw");
        float pitch = (float) cfg.getDouble("spawn.location.pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}
