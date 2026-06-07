package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SignierenCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final NamespacedKey keySignedBy;
    private final NamespacedKey keySignedText;

    public SignierenCommand(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.keySignedBy = new NamespacedKey(plugin, "signed_by");
        this.keySignedText = new NamespacedKey(plugin, "signed_text");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "§cNur Spieler können diesen Befehl verwenden.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(prefix + "§cVerwendung: /signieren <text>");
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(prefix + "§cDu hältst kein Item in der Hand.");
            return true;
        }

        String text = String.join(" ", args);
        String playerName = player.getName();

        ItemMeta meta = held.getItemMeta();
        if (meta == null) {
            player.sendMessage(prefix + "§cDieses Item kann nicht signiert werden.");
            return true;
        }

        // Add signature lore
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        lore.add("§8§m--------------------");
        lore.add("§7Signiert von: §e" + playerName);
        lore.add("§7\"" + text + "\"");
        lore.add("§8§m--------------------");
        meta.setLore(lore);

        // Store in PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keySignedBy, PersistentDataType.STRING, playerName);
        pdc.set(keySignedText, PersistentDataType.STRING, text);

        held.setItemMeta(meta);
        player.sendMessage(prefix + "§aItem erfolgreich signiert!");
        return true;
    }
}
