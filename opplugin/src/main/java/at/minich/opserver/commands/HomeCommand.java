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

public class HomeCommand implements CommandExecutor {

    private final OpServerPlugin plugin;

    public HomeCommand(OpServerPlugin plugin) {
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

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "sethome" -> {
                String name = (args.length >= 1) ? args[0].toLowerCase() : "home";
                int maxHomes = plugin.getConfig().getInt("homes.max-per-player", 3);
                YamlConfiguration cfg = loadHomes(player);
                List<String> keys = cfg.getKeys(false).stream().toList();
                if (!cfg.contains(name) && keys.size() >= maxHomes) {
                    sender.sendMessage(prefix + "§cDu hast bereits die maximale Anzahl an Homes (" + maxHomes + ").");
                    return true;
                }
                Location loc = player.getLocation();
                cfg.set(name + ".world", loc.getWorld().getName());
                cfg.set(name + ".x", loc.getX());
                cfg.set(name + ".y", loc.getY());
                cfg.set(name + ".z", loc.getZ());
                cfg.set(name + ".yaw", (double) loc.getYaw());
                cfg.set(name + ".pitch", (double) loc.getPitch());
                saveHomes(player, cfg);
                sender.sendMessage(prefix + "§aHome §e" + name + " §agesetzt.");
            }
            case "delhome" -> {
                String name = (args.length >= 1) ? args[0].toLowerCase() : "home";
                YamlConfiguration cfg = loadHomes(player);
                if (!cfg.contains(name)) {
                    sender.sendMessage(prefix + "§cHome §e" + name + " §cexistiert nicht.");
                    return true;
                }
                cfg.set(name, null);
                saveHomes(player, cfg);
                sender.sendMessage(prefix + "§aHome §e" + name + " §agelöscht.");
            }
            default -> {
                // /home command
                if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
                    YamlConfiguration cfg = loadHomes(player);
                    List<String> names = cfg.getKeys(false).stream().sorted().toList();
                    if (names.isEmpty()) {
                        sender.sendMessage(prefix + "§eKeine Homes gesetzt.");
                    } else {
                        sender.sendMessage(prefix + "§6Deine Homes: §e" + String.join(", ", names));
                    }
                    return true;
                }
                String name = (args.length >= 1) ? args[0].toLowerCase() : "home";
                YamlConfiguration cfg = loadHomes(player);
                if (!cfg.contains(name)) {
                    sender.sendMessage(prefix + "§cHome §e" + name + " §cexistiert nicht.");
                    return true;
                }
                Location loc = deserializeLocation(cfg, name);
                if (loc == null) {
                    sender.sendMessage(prefix + "§cFehler: Welt für Home §e" + name + " §cnicht gefunden.");
                    return true;
                }
                player.teleport(loc);
                sender.sendMessage(prefix + "§aTeleportiert zu Home §e" + name + "§a.");
            }
        }
        return true;
    }

    private YamlConfiguration loadHomes(Player player) {
        return plugin.getDataManager().loadYaml("homes/" + player.getUniqueId() + ".yml");
    }

    private void saveHomes(Player player, YamlConfiguration cfg) {
        plugin.getDataManager().saveYaml(cfg, "homes/" + player.getUniqueId() + ".yml");
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
