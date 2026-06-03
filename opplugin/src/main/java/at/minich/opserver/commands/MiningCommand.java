package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.mining.AreaMineManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /mining [1|3|5|7] — View or set your area mining size.
 * Aliases: /mine, /abbau
 */
public class MiningCommand implements CommandExecutor, TabCompleter {

    private final OpServerPlugin plugin;

    public MiningCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");
        AreaMineManager mgr = plugin.getAreaMineManager();

        if (args.length == 0) {
            int current = mgr.getSize(player.getUniqueId());
            player.sendMessage(prefix + "§7Deine aktuelle Mining-Größe: §e" + current + "x" + current);
            player.sendMessage(prefix + "§7Verfügbare Größen: §e1§7, §e3§7, §e5§7, §e7");
            return true;
        }

        int size;
        try {
            size = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + "§cUngültige Eingabe. Erlaubt: §e1, 3, 5, 7");
            return true;
        }

        if (!AreaMineManager.isValidSize(size)) {
            player.sendMessage(prefix + "§cUngültige Größe. Erlaubt: §e1, 3, 5, 7");
            return true;
        }

        mgr.setSize(player.getUniqueId(), size);

        if (size == 1) {
            player.sendMessage(prefix + "§aArea Mining §cdeaktiviert §a(1x1 — normales Mining).");
        } else {
            player.sendMessage(prefix + "§aArea Mining auf §e" + size + "x" + size + " §agestellt.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (int s : AreaMineManager.VALID_SIZES) {
                String sv = String.valueOf(s);
                if (sv.startsWith(args[0])) completions.add(sv);
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
