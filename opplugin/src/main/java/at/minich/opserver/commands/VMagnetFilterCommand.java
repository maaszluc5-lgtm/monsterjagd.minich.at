package at.minich.opserver.commands;

import at.minich.opserver.items.SellMagnetListener;
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

public class VMagnetFilterCommand implements CommandExecutor, TabCompleter {

    private final SellMagnetListener listener;

    public VMagnetFilterCommand(SellMagnetListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (!listener.isSellMagnetItem(offhand)) {
            player.sendMessage("§cDu musst den §6Verkaufs-Magneten §cin der Offhand halten!");
            return true;
        }

        ItemMeta meta = offhand.getItemMeta();
        if (meta == null) return true;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String filterStr = pdc.getOrDefault(listener.getKeyFilter(), PersistentDataType.STRING, "");

        // No args → show current filter
        if (args.length == 0) {
            if (filterStr.isEmpty()) {
                player.sendMessage("§6💰 §7Behalten-Filter: §eKeine Items (alles wird verkauft)");
            } else {
                player.sendMessage("§6💰 §7Behalten-Filter: §e" + filterStr.replace(",", "§7, §e"));
            }
            player.sendMessage("§7Nutze §b/vmagnetfilter <item> §7zum Hinzufügen/Entfernen");
            player.sendMessage("§7Nutze §b/vmagnetfilter clear §7zum Leeren");
            return true;
        }

        String itemName = args[0].toLowerCase();

        if (itemName.equals("clear")) {
            pdc.set(listener.getKeyFilter(), PersistentDataType.STRING, "");
            boolean active = pdc.getOrDefault(listener.getKeyActive(), PersistentDataType.BYTE, (byte) 1) == 1;
            listener.updateLore(offhand, meta, active, "");
            offhand.setItemMeta(meta);
            player.sendMessage("§6💰 §aBehalten-Filter geleert! Alle Items werden verkauft.");
            return true;
        }

        Material mat = Material.matchMaterial(itemName);
        if (mat == null) {
            player.sendMessage("§cUnbekanntes Item: §e" + itemName);
            player.sendMessage("§7Tipp: Nutze den englischen Item-Namen (z.B. §bdiamond§7, §boak_log§7)");
            return true;
        }

        List<String> currentItems = filterStr.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(filterStr.split(",")));

        String matName = mat.name().toLowerCase();
        if (currentItems.contains(matName)) {
            currentItems.remove(matName);
            player.sendMessage("§6💰 §e" + matName + " §caus Behalten-Filter entfernt §7(wird jetzt verkauft)");
        } else {
            currentItems.add(matName);
            player.sendMessage("§6💰 §e" + matName + " §azum Behalten-Filter hinzugefügt §7(wird nicht mehr verkauft)");
        }

        String newFilter = String.join(",", currentItems);
        pdc.set(listener.getKeyFilter(), PersistentDataType.STRING, newFilter);
        boolean active = pdc.getOrDefault(listener.getKeyActive(), PersistentDataType.BYTE, (byte) 1) == 1;
        listener.updateLore(offhand, meta, active, newFilter);
        offhand.setItemMeta(meta);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            suggestions.add("clear");
            for (Material m : Material.values()) {
                if (m.isItem() && m.name().toLowerCase().startsWith(partial)) {
                    suggestions.add(m.name().toLowerCase());
                }
            }
            return suggestions.stream().limit(20).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
