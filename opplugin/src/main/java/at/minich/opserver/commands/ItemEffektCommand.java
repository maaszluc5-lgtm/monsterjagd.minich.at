package at.minich.opserver.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /itemeffekt <effekt> <stufe> [hand|offhand]
 * Adds a passive potion effect to the held item.
 * Effect is active while the item is in main hand or offhand.
 *
 * /itemeffekt list    — shows all effects on held item
 * /itemeffekt remove <effekt> — removes an effect
 */
public class ItemEffektCommand implements CommandExecutor, TabCompleter {

    public static final String PDC_KEY_PREFIX = "item_effect_";
    private final JavaPlugin plugin;
    private final NamespacedKey slotKey;

    public ItemEffektCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.slotKey = new NamespacedKey(plugin, "item_effect_slot");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        if (!player.isOp()) {
            player.sendMessage("§cKein Zugriff!");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("§cHalte ein Item in der Hand!");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            showEffects(player, item);
            return true;
        }

        if (args[0].equalsIgnoreCase("remove") && args.length >= 2) {
            removeEffect(player, item, args[1]);
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /itemeffekt <effekt> <stufe> [hand|offhand|both]");
            player.sendMessage("§7/itemeffekt list  |  /itemeffekt remove <effekt>");
            return true;
        }

        PotionEffectType type = PotionEffectType.getByName(args[0].toUpperCase());
        if (type == null) {
            player.sendMessage("§cUnbekannter Effekt: §e" + args[0]);
            player.sendMessage("§7Beispiele: NIGHT_VISION, SPEED, STRENGTH, JUMP_BOOST, REGENERATION");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
            if (level < 1 || level > 255) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage("§cStufe muss zwischen 1 und 255 sein.");
            return true;
        }

        String slot = args.length >= 3 ? args[2].toLowerCase() : "both";
        if (!slot.equals("hand") && !slot.equals("offhand") && !slot.equals("both")) {
            player.sendMessage("§cSlot muss hand, offhand oder both sein.");
            return true;
        }

        // Store in PDC: key = "item_effect_<EFFECT_NAME>", value = "<level>:<slot>"
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;
        NamespacedKey key = new NamespacedKey(plugin, PDC_KEY_PREFIX + type.getName());
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, level + ":" + slot);

        // Update lore
        updateEffectLore(meta, plugin);
        item.setItemMeta(meta);

        player.sendMessage("§a✔ Effekt §e" + type.getName() + " §aStufe §e" + level
                + " §a(" + slot + ") §ahinzugefügt!");
        return true;
    }

    private void showEffects(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) { player.sendMessage("§7Keine Effekte."); return; }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean any = false;
        for (PotionEffectType type : PotionEffectType.values()) {
            NamespacedKey key = new NamespacedKey(plugin, PDC_KEY_PREFIX + type.getName());
            String val = pdc.get(key, PersistentDataType.STRING);
            if (val != null) {
                String[] parts = val.split(":");
                player.sendMessage("§7- §e" + type.getName() + " §7Stufe §e" + parts[0] + " §7(" + (parts.length > 1 ? parts[1] : "both") + ")");
                any = true;
            }
        }
        if (!any) player.sendMessage("§7Keine Effekte auf diesem Item.");
    }

    private void removeEffect(Player player, ItemStack item, String effectName) {
        PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
        if (type == null) { player.sendMessage("§cUnbekannter Effekt!"); return; }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        NamespacedKey key = new NamespacedKey(plugin, PDC_KEY_PREFIX + type.getName());
        meta.getPersistentDataContainer().remove(key);
        updateEffectLore(meta, plugin);
        item.setItemMeta(meta);
        player.sendMessage("§aEffekt §e" + type.getName() + " §aentfernt.");
    }

    public static void updateEffectLore(ItemMeta meta, JavaPlugin plugin) {
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        // Remove old effect lines
        lore.removeIf(l -> l.startsWith("§5⚗ ") || l.equals("§8§m------------") && lore.indexOf(l) > lore.size() - 5);
        // Add current effects
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean addedSep = false;
        for (PotionEffectType type : PotionEffectType.values()) {
            NamespacedKey key = new NamespacedKey(plugin, PDC_KEY_PREFIX + type.getName());
            String val = pdc.get(key, PersistentDataType.STRING);
            if (val != null) {
                if (!addedSep) { lore.add("§8§m------------"); addedSep = true; }
                String[] parts = val.split(":");
                String slot = parts.length > 1 ? parts[1] : "both";
                String slotDisplay = slot.equals("hand") ? "§7Hand" : slot.equals("offhand") ? "§7Offhand" : "§7Hand+Offhand";
                lore.add("§5⚗ §e" + type.getName() + " §7Stufe §e" + parts[0] + " §8| " + slotDisplay);
            }
        }
        meta.setLore(lore);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            opts.add("list"); opts.add("remove");
            for (PotionEffectType t : PotionEffectType.values()) opts.add(t.getName().toLowerCase());
            return opts.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            List<String> opts = new ArrayList<>();
            for (PotionEffectType t : PotionEffectType.values()) opts.add(t.getName().toLowerCase());
            return opts.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) return List.of("1", "2", "3", "5", "10");
        if (args.length == 3) return List.of("hand", "offhand", "both");
        return List.of();
    }
}
