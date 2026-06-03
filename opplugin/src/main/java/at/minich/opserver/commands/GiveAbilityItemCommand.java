package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.items.AbilityItem;
import at.minich.opserver.items.AbilityItemListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveAbilityItemCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final AbilityItemListener listener;

    public GiveAbilityItemCommand(OpServerPlugin plugin, AbilityItemListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /giveabilityitem <player> <fly|heal|speed|god>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cSpieler nicht gefunden.");
            return true;
        }
        AbilityItem ability = AbilityItem.fromId(args[1].toLowerCase());
        if (ability == null) {
            sender.sendMessage("§cUnbekanntes Ability Item. Optionen: fly, heal, speed, god");
            return true;
        }
        ItemStack item = ability.build(listener.getAbilityKey());
        target.getInventory().addItem(item);
        sender.sendMessage("§a✦ " + ability.displayName + " §aan §f" + target.getName() + " §agegeben.");
        target.sendMessage("§a✦ Du hast §f" + ability.displayName + " §aerhalten!");
        return true;
    }
}
