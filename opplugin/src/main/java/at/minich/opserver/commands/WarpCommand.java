package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WarpCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public WarpCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "setwarp" -> {
                if (!sender.hasPermission("opserver.admin")) {
                    sender.sendMessage(prefix + plugin.getConfig().getString("messages.no-permission", "§cNo permission."));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(prefix + "§cOnly players can set warps.");
                    return true;
                }
                if (args.length < 1) {
                    sender.sendMessage(prefix + "§cUsage: /setwarp <name>");
                    return true;
                }
                String name = args[0].toLowerCase();
                YamlConfiguration cfg = loadWarps();
                Location loc = player.getLocation();
                cfg.set(name + ".world", loc.getWorld().getName());
                cfg.set(name + ".x", loc.getX());
                cfg.set(name + ".y", loc.getY());
                cfg.set(name + ".z", loc.getZ());
                cfg.set(name + ".yaw", (double) loc.getYaw());
                cfg.set(name + ".pitch", (double) loc.getPitch());
                saveWarps(cfg);
                sender.sendMessage(prefix + "§aWarp §e" + name + " §agesetzt.");
            }
            case "delwarp" -> {
                if (!sender.hasPermission("opserver.admin")) {
                    sender.sendMessage(prefix + plugin.getConfig().getString("messages.no-permission", "§cNo permission."));
                    return true;
                }
                if (args.length < 1) {
                    sender.sendMessage(prefix + "§cUsage: /delwarp <name>");
                    return true;
                }
                String name = args[0].toLowerCase();
                YamlConfiguration cfg = loadWarps();
                if (!cfg.contains(name)) {
                    sender.sendMessage(prefix + "§cWarp §e" + name + " §cexistiert nicht.");
                    return true;
                }
                cfg.set(name, null);
                saveWarps(cfg);
                sender.sendMessage(prefix + "§aWarp §e" + name + " §agelöscht.");
            }
            default -> {
                // /warp
                if (args.length < 1 || args[0].equalsIgnoreCase("list")) {
                    YamlConfiguration cfg = loadWarps();
                    List<String> names = cfg.getKeys(false).stream().sorted().toList();
                    if (names.isEmpty()) {
                        sender.sendMessage(prefix + "§eKeine Warps vorhanden.");
                    } else {
                        sender.sendMessage(prefix + "§6Warps: §e" + String.join(", ", names));
                    }
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(prefix + "§cOnly players can teleport to warps.");
                    return true;
                }
                String name = args[0].toLowerCase();
                YamlConfiguration cfg = loadWarps();
                if (!cfg.contains(name)) {
                    sender.sendMessage(prefix + "§cWarp §e" + name + " §cexistiert nicht.");
                    return true;
                }
                Location loc = deserializeLocation(cfg, name);
                if (loc == null) {
                    sender.sendMessage(prefix + "§cFehler: Welt für Warp §e" + name + " §cnicht gefunden.");
                    return true;
                }
                player.teleport(loc);
                sender.sendMessage(prefix + "§aTeleportiert zu Warp §e" + name + "§a.");
            }
        }
        return true;
    }

    private YamlConfiguration loadWarps() {
        return plugin.getDataManager().loadYaml("warps.yml");
    }

    private void saveWarps(YamlConfiguration cfg) {
        plugin.getDataManager().saveYaml(cfg, "warps.yml");
    }

    private Location deserializeLocation(YamlConfiguration cfg, String key) {
        String worldName = cfg.getString(key + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x = cfg.getDouble(key + ".x");
        double y = cfg.getDouble(key + ".y");
        double z = cfg.getDouble(key + ".z");
        float yaw = (float) cfg.getDouble(key + ".yaw");
        float pitch = (float) cfg.getDouble(key + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }
}
