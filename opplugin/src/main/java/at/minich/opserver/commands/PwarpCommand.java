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

public class PwarpCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public PwarpCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cOnly players can use this command.");
            return true;
        }

        if (args.length < 1 || args[0].equalsIgnoreCase("list")) {
            YamlConfiguration cfg = loadPwarps();
            List<String> names = cfg.getKeys(false).stream().sorted().toList();
            if (names.isEmpty()) {
                sender.sendMessage(prefix + "§eKeine Player-Warps vorhanden.");
            } else {
                sender.sendMessage(prefix + "§6Player-Warps:");
                for (String name : names) {
                    String owner = cfg.getString(name + ".owner", "?");
                    sender.sendMessage(prefix + " §e" + name + " §7(von §f" + owner + "§7)");
                }
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("set")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + "§cUsage: /pwarp set <name>");
                return true;
            }
            String name = args[1].toLowerCase();
            double cost = plugin.getConfig().getDouble("pwarp.cost", 1000.0);
            YamlConfiguration cfg = loadPwarps();

            // Check if player already owns a pwarp
            String uuid = player.getUniqueId().toString();
            for (String key : cfg.getKeys(false)) {
                if (uuid.equals(cfg.getString(key + ".uuid"))) {
                    sender.sendMessage(prefix + "§cDu hast bereits einen Player-Warp (§e" + key + "§c). Lösche ihn zuerst.");
                    return true;
                }
            }

            // Check cost
            double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
            if (balance < cost) {
                sender.sendMessage(prefix + "§cNicht genug Coins. Benötigt: §e" + (long) cost + " Coins§c.");
                return true;
            }

            if (cfg.contains(name)) {
                sender.sendMessage(prefix + "§cEin Player-Warp mit dem Namen §e" + name + " §cexistiert bereits.");
                return true;
            }

            plugin.getEconomyManager().withdraw(player.getUniqueId(), cost);
            Location loc = player.getLocation();
            cfg.set(name + ".uuid", uuid);
            cfg.set(name + ".owner", player.getName());
            cfg.set(name + ".world", loc.getWorld().getName());
            cfg.set(name + ".x", loc.getX());
            cfg.set(name + ".y", loc.getY());
            cfg.set(name + ".z", loc.getZ());
            cfg.set(name + ".yaw", (double) loc.getYaw());
            cfg.set(name + ".pitch", (double) loc.getPitch());
            savePwarps(cfg);
            sender.sendMessage(prefix + "§aPlayer-Warp §e" + name + " §afür §e" + (long) cost + " Coins §aerstellt.");
            return true;
        }

        if (sub.equals("del")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + "§cUsage: /pwarp del <name>");
                return true;
            }
            String name = args[1].toLowerCase();
            YamlConfiguration cfg = loadPwarps();
            if (!cfg.contains(name)) {
                sender.sendMessage(prefix + "§cPlayer-Warp §e" + name + " §cexistiert nicht.");
                return true;
            }
            String ownerUuid = cfg.getString(name + ".uuid", "");
            if (!ownerUuid.equals(player.getUniqueId().toString()) && !player.hasPermission("opserver.admin")) {
                sender.sendMessage(prefix + "§cDas ist nicht dein Player-Warp.");
                return true;
            }
            cfg.set(name, null);
            savePwarps(cfg);
            sender.sendMessage(prefix + "§aPlayer-Warp §e" + name + " §agelöscht.");
            return true;
        }

        // /pwarp <name> — teleport
        String name = sub;
        YamlConfiguration cfg = loadPwarps();
        if (!cfg.contains(name)) {
            sender.sendMessage(prefix + "§cPlayer-Warp §e" + name + " §cexistiert nicht.");
            return true;
        }
        Location loc = deserializeLocation(cfg, name);
        if (loc == null) {
            sender.sendMessage(prefix + "§cFehler: Welt für Player-Warp §e" + name + " §cnicht gefunden.");
            return true;
        }
        player.teleport(loc);
        sender.sendMessage(prefix + "§aTeleportiert zu Player-Warp §e" + name + "§a.");
        return true;
    }

    private YamlConfiguration loadPwarps() {
        return plugin.getDataManager().loadYaml("pwarps.yml");
    }

    private void savePwarps(YamlConfiguration cfg) {
        plugin.getDataManager().saveYaml(cfg, "pwarps.yml");
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
