package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.trophies.TrophyManager;
import at.minich.opserver.trophies.TrophyType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * /trophies [player] — Opens a 54-slot GUI displaying all trophies.
 */
public class TrophyCommand implements CommandExecutor {

    // Fixed slots for the 11 trophy types (in TrophyType.values() order)
    private static final int[] TROPHY_SLOTS = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32};

    private final OpServerPlugin plugin;
    private final TrophyManager trophyManager;

    public TrophyCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.trophyManager = plugin.getTrophyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }

        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                viewer.sendMessage(plugin.getConfig().getString("messages.player-not-found",
                        "§cPlayer not found."));
                return true;
            }
        } else {
            target = viewer;
        }

        openTrophyGUI(viewer, target);
        return true;
    }

    private void openTrophyGUI(Player viewer, Player target) {
        Inventory gui = Bukkit.createInventory(null, 54,
                "§6§lPokale von " + target.getName());

        UUID targetUUID = target.getUniqueId();
        List<TrophyType> earned = trophyManager.getTrophies(targetUUID);
        TrophyType[] allTypes = TrophyType.values();

        // Fill with black glass panes
        ItemStack black = makeMeta(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "§r", null);
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, black);
        }

        // Trophy items in fixed slots
        for (int i = 0; i < allTypes.length; i++) {
            if (i >= TROPHY_SLOTS.length) break;
            TrophyType type = allTypes[i];
            int slot = TROPHY_SLOTS[i];

            if (earned.contains(type)) {
                gui.setItem(slot, trophyManager.buildTrophyItem(type, target));
            } else {
                List<String> lore = Collections.singletonList("§7Noch nicht verdient");
                gui.setItem(slot, makeMeta(new ItemStack(Material.GRAY_STAINED_GLASS_PANE),
                        "§8???", lore));
            }
        }

        // Close button at slot 49
        gui.setItem(49, makeMeta(new ItemStack(Material.BARRIER), "§cSchließen", null));

        viewer.openInventory(gui);
    }

    private ItemStack makeMeta(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
