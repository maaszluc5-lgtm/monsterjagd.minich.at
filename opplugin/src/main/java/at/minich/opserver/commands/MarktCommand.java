package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.markt.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class MarktCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final MarktGUIListener listener;

    public MarktCommand(OpServerPlugin plugin, MarktGUIListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            listener.openMarkt(player, 0, MarktFilter.ALLE, MarktSort.NEU, null);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "sell": {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) {
                    player.sendMessage("§cDu hältst kein Item in der Hand.");
                    return true;
                }
                if (args.length < 2) {
                    // Ask for price via chat, then open sell GUI
                    listener.startSellSession(player, hand.clone(), hand.getAmount());
                    return true;
                }
                double price;
                try {
                    price = Double.parseDouble(args[1].replace(",", "."));
                } catch (NumberFormatException e) {
                    player.sendMessage("§cUngültiger Preis: §e" + args[1]);
                    return true;
                }
                int amount = hand.getAmount();
                if (args.length >= 3) {
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cUngültige Menge: §e" + args[2]);
                        return true;
                    }
                }
                listener.openSell(player, hand.clone(), amount, price);
                return true;
            }

            case "remove": {
                if (args.length < 2) {
                    player.sendMessage("§cVerwendung: /markt remove <ID>");
                    return true;
                }
                plugin.getMarktManager().removeListing(args[1], player);
                return true;
            }

            case "list": {
                List<MarktListing> listings = plugin.getMarktManager().getListingsByPlayer(player.getUniqueId());
                if (listings.isEmpty()) {
                    player.sendMessage("§7Du hast keine aktiven Angebote.");
                    return true;
                }
                player.sendMessage("§6§lDeine Angebote:");
                for (MarktListing l : listings) {
                    player.sendMessage("§e" + l.getId() + " §f- §7"
                            + l.getAmount() + "x " + MarktManager.itemDisplayName(l.getItem())
                            + " §8| §6" + MarktManager.formatCoins(l.getPrice()) + " Coins"
                            + " §8| §7eingestellt vor " + MarktManager.timeAgo(l.getListedAt()));
                }
                return true;
            }

            case "search": {
                if (args.length < 2) {
                    player.sendMessage("§cVerwendung: /markt search <Suchbegriff>");
                    return true;
                }
                String term = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                listener.openMarkt(player, 0, MarktFilter.ALLE, MarktSort.NEU, term);
                return true;
            }

            default:
                sendHelp(player);
                return true;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l/markt §7- Spieler-Markt");
        player.sendMessage("§e/markt §7- Markt öffnen");
        player.sendMessage("§e/markt sell §7- Item in der Hand verkaufen (Preis per Chat)");
        player.sendMessage("§e/markt sell <preis> [menge] §7- Item direkt zum Preis einstellen");
        player.sendMessage("§e/markt remove <ID> §7- Angebot entfernen");
        player.sendMessage("§e/markt list §7- Deine Angebote anzeigen");
        player.sendMessage("§e/markt search <Begriff> §7- Markt durchsuchen");
    }
}
