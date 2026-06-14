package at.minich.opserver.commands;

import at.minich.opserver.items.SellLeggingListener;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class VLeggingFilterCommand implements CommandExecutor, TabCompleter {

    private final SellLeggingListener listener;

    public VLeggingFilterCommand(SellLeggingListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }

        ItemStack legs = player.getInventory().getLeggings();
        if (!listener.isSellLegging(legs)) {
            player.sendMessage("§cDu musst die §6Verkaufs-Leggings §ctragen!");
            return true;
        }

        ItemMeta meta = legs.getItemMeta();
        if (meta == null) return true;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String filterStr = pdc.getOrDefault(listener.getKeyFilter(), PersistentDataType.STRING, "");

        if (args.length == 0) {
            player.sendMessage("§6💰 §7Behalten-Filter: " + (filterStr.isEmpty() ? "§eKeine" : "§e" + filterStr.replace(",", "§7, §e")));
            player.sendMessage("§7Nutze §b/vleggingfilter <item> §7zum Hinzufügen/Entfernen");
            player.sendMessage("§7Nutze §b/vleggingfilter clear §7zum Leeren");
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            pdc.set(listener.getKeyFilter(), PersistentDataType.STRING, "");
            listener.updateLore(legs, meta, "");
            legs.setItemMeta(meta);
            player.sendMessage("§6💰 §aBehalten-Filter geleert!");
            return true;
        }

        Material mat = Material.matchMaterial(args[0]);
        if (mat == null) {
            player.sendMessage("§cUnbekanntes Item: §e" + args[0]);
            return true;
        }

        List<String> current = filterStr.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(filterStr.split(",")));
        String matName = mat.name().toLowerCase();
        if (current.contains(matName)) {
            current.remove(matName);
            player.sendMessage("§6💰 §e" + matName + " §centfernt §7(wird jetzt verkauft)");
        } else {
            current.add(matName);
            player.sendMessage("§6💰 §e" + matName + " §ahinzugefügt §7(wird nicht mehr verkauft)");
        }

        String newFilter = String.join(",", current);
        pdc.set(listener.getKeyFilter(), PersistentDataType.STRING, newFilter);
        listener.updateLore(legs, meta, newFilter);
        legs.setItemMeta(meta);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            suggestions.add("clear");
            for (Material m : Material.values()) {
                if (m.isItem() && m.name().toLowerCase().startsWith(partial)) suggestions.add(m.name().toLowerCase());
            }
            return suggestions.stream().limit(20).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
