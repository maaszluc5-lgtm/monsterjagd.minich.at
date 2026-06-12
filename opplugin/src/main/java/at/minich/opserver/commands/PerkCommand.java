package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.perks.Perk;
import at.minich.opserver.perks.PerkManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PerkCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final PerkManager perkManager;

    public PerkCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.perkManager = plugin.getPerkManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }

        // Admin: /perks give <player> <perk>
        if (args.length >= 3 && args[0].equalsIgnoreCase("give") && player.isOp()) {
            @SuppressWarnings("deprecation")
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { player.sendMessage("§cSpieler nicht gefunden!"); return true; }
            try {
                Perk perk = Perk.valueOf(args[2].toUpperCase());
                perkManager.givePerk(target.getUniqueId(), perk);
                player.sendMessage("§a✔ §e" + perk.getDisplayName() + " §aan §e" + target.getName() + " §agegeben!");
                target.sendMessage("§a✔ Du hast den Perk §e" + perk.getDisplayName() + " §aerhalten!");
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cUnbekannter Perk: §e" + args[2]);
            }
            return true;
        }

        openGUI(player);
        return true;
    }

    private void openGUI(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Perk> playerPerks = perkManager.getPerks(uuid);
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lDeine Perks");

        // Fill with glass
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) { fillerMeta.setDisplayName(" "); filler.setItemMeta(fillerMeta); }
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        int slot = 0;
        for (Perk perk : Perk.values()) {
            if (slot >= 54) break;
            boolean has = playerPerks.contains(perk);

            ItemStack item = new ItemStack(has ? perk.getIcon() : Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) { slot++; continue; }

            String rarityColor = switch (perk.getRarity()) {
                case COMMON -> "§f";
                case RARE -> "§9";
                case EPIC -> "§5";
                case LEGENDARY -> "§6";
            };

            meta.setDisplayName(rarityColor + perk.getDisplayName() + (has ? " §a✔" : " §c✗"));
            List<String> lore = new ArrayList<>();
            lore.add("§7" + perk.getDescription());
            lore.add("");
            lore.add("§7Seltenheit: " + perk.getRarity().getDisplay());
            lore.add(has ? "§a§lAKTIV" : "§c§lNICHT AKTIV");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
        }

        // Stats item
        ItemStack stats = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = stats.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName("§6§lPerk Statistiken");
            List<String> lore = new ArrayList<>();
            lore.add("§7Aktive Perks: §e" + playerPerks.size() + " §8/ §e" + Perk.values().length);
            long legendary = playerPerks.stream().filter(p -> p.getRarity() == Perk.Rarity.LEGENDARY).count();
            long epic = playerPerks.stream().filter(p -> p.getRarity() == Perk.Rarity.EPIC).count();
            long rare = playerPerks.stream().filter(p -> p.getRarity() == Perk.Rarity.RARE).count();
            long common = playerPerks.stream().filter(p -> p.getRarity() == Perk.Rarity.COMMON).count();
            lore.add("§6Legendär: §e" + legendary);
            lore.add("§5Episch: §e" + epic);
            lore.add("§9Selten: §e" + rare);
            lore.add("§fGewöhnlich: §e" + common);
            statsMeta.setLore(lore);
            stats.setItemMeta(statsMeta);
        }
        inv.setItem(49, stats);

        player.openInventory(inv);
    }
}
