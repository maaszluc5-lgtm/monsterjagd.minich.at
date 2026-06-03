package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.ah.AuctionEntry;
import at.minich.opserver.ah.AuctionFilter;
import at.minich.opserver.ah.AuctionGUI;
import at.minich.opserver.ah.AuctionGUIListener;
import at.minich.opserver.ah.AuctionSort;
import at.minich.opserver.markt.MarktManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * /ah [sell <startPreis> [stunden] | cancel <id> | list | collect]
 */
public class AhCommand implements CommandExecutor {

    private final OpServerPlugin plugin;
    private final AuctionGUIListener guiListener;

    public AhCommand(OpServerPlugin plugin, AuctionGUIListener guiListener) {
        this.plugin = plugin;
        this.guiListener = guiListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            // Open GUI
            guiListener.openAuctionHouse(player, 0, AuctionFilter.ALLE, AuctionSort.ENDET_BALD);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "sell": {
                if (args.length < 2) {
                    player.sendMessage("§cVerwendung: §e/ah sell <startPreis> [stunden]");
                    return true;
                }
                double startPrice;
                try {
                    startPrice = Double.parseDouble(args[1].replace(",", "."));
                } catch (NumberFormatException e) {
                    player.sendMessage("§cUngültiger Preis: §e" + args[1]);
                    return true;
                }
                int defaultHours = plugin.getConfig().getInt("ah.default-duration-hours", 24);
                int maxHours = plugin.getConfig().getInt("ah.max-duration-hours", 72);
                int durationHours = defaultHours;
                if (args.length >= 3) {
                    try {
                        durationHours = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cUngültige Stunden: §e" + args[2]);
                        return true;
                    }
                }
                if (durationHours > maxHours) {
                    player.sendMessage("§cMaximale Dauer: §e" + maxHours + "h§c.");
                    return true;
                }
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage("§cDu hältst kein Item in der Hand.");
                    return true;
                }
                plugin.getAuctionManager().createAuction(player, held.clone(), startPrice, durationHours);
                break;
            }

            case "cancel": {
                if (args.length < 2) {
                    player.sendMessage("§cVerwendung: §e/ah cancel <id>");
                    return true;
                }
                plugin.getAuctionManager().cancelAuction(args[1], player);
                break;
            }

            case "list": {
                List<AuctionEntry> list = plugin.getAuctionManager().getAuctionsByPlayer(player.getUniqueId());
                if (list.isEmpty()) {
                    player.sendMessage("§7Du hast keine aktiven Auktionen.");
                    return true;
                }
                player.sendMessage("§6§lDeine Auktionen:");
                for (AuctionEntry entry : list) {
                    player.sendMessage("§7- §eID: §f" + entry.getId()
                            + " §7| §f" + MarktManager.itemDisplayName(entry.getItem())
                            + " §7| Gebot: §6" + MarktManager.formatCoins(entry.getCurrentBid()) + " Coins"
                            + " §7| Endet in: §f" + AuctionGUI.formatTimeRemaining(entry.getEndsAt()));
                }
                break;
            }

            case "collect": {
                plugin.getAuctionManager().collectPending(player);
                break;
            }

            default: {
                player.sendMessage("§cUnbekannter Unterbefehl. Verwende: §e/ah [sell|cancel|list|collect]");
            }
        }

        return true;
    }
}
