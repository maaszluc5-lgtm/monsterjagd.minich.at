package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.plots.Plot;
import at.minich.opserver.plots.PlotManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /plot <auto|info|home|trust <player>|untrust <player>>
 */
public class PlotCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList("auto", "info", "home", "trust", "untrust");

    private final OpServerPlugin plugin;
    private final PlotManager plotManager;

    public PlotCommand(OpServerPlugin plugin, PlotManager plotManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "auto" -> handleAuto(player);
            case "info" -> handleInfo(player);
            case "home" -> handleHome(player);
            case "trust" -> {
                if (args.length < 2) {
                    player.sendMessage("§cVerwendung: /plot trust <Spieler>");
                    return true;
                }
                handleTrust(player, args[1]);
            }
            case "untrust" -> {
                if (args.length < 2) {
                    player.sendMessage("§cVerwendung: /plot untrust <Spieler>");
                    return true;
                }
                handleUntrust(player, args[1]);
            }
            default -> sendHelp(player);
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Subcommand handlers
    // -------------------------------------------------------------------------

    private void handleAuto(Player player) {
        UUID uuid = player.getUniqueId();

        if (plotManager.getPlotCount(uuid) >= PlotManager.MAX_PLOTS_PER_PLAYER) {
            player.sendMessage("§cDu hast bereits die maximale Anzahl an Plots (" + PlotManager.MAX_PLOTS_PER_PLAYER + ").");
            return;
        }

        Plot plot = plotManager.claimNextPlot(uuid);
        if (plot == null) {
            player.sendMessage("§cEs konnte kein Plot beansprucht werden.");
            return;
        }

        player.sendMessage("§a§lPlot beansprucht! §rPlot-ID: §e#" + plot.getId());
        player.sendMessage("§7Koordinaten: §e(" + plot.getWorldMinX() + ", " + Plot.Y_MIN + ", " + plot.getWorldMinZ() + ")"
                + " §7bis §e(" + plot.getWorldMaxX() + ", " + Plot.Y_MAX + ", " + plot.getWorldMaxZ() + ")");
        player.sendMessage("§7Benutze §e/plot home §7um dorthin zu teleportieren.");

        // Teleport player to the new plot immediately
        teleportToPlot(player, plot);
    }

    private void handleInfo(Player player) {
        UUID uuid = player.getUniqueId();
        List<Plot> owned = plotManager.getPlotsOf(uuid);

        if (owned.isEmpty()) {
            player.sendMessage("§7Du besitzt noch kein Plot. Benutze §e/plot auto §7um eines zu beanspruchen.");
            return;
        }

        player.sendMessage("§6§l── Deine Plots ─────────────────");
        for (Plot p : owned) {
            player.sendMessage("§e  Plot #" + p.getId()
                    + " §7[" + p.getWorldMinX() + ", " + p.getWorldMinZ() + "]"
                    + " §7→ §e[" + p.getWorldMaxX() + ", " + p.getWorldMaxZ() + "]");
            if (!p.getTrusted().isEmpty()) {
                StringBuilder sb = new StringBuilder("§7    Vertraute: ");
                for (UUID t : p.getTrusted()) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(t);
                    sb.append("§a").append(op.getName() != null ? op.getName() : t.toString()).append("§7, ");
                }
                player.sendMessage(sb.substring(0, sb.length() - 4)); // trim trailing ", "
            }
        }
        player.sendMessage("§6§l──────────────────────────────");
        player.sendMessage("§7Plots: §e" + owned.size() + "/" + PlotManager.MAX_PLOTS_PER_PLAYER);
    }

    private void handleHome(Player player) {
        Plot plot = plotManager.getFirstPlot(player.getUniqueId());
        if (plot == null) {
            player.sendMessage("§cDu besitzt noch kein Plot. Benutze §e/plot auto§c.");
            return;
        }
        teleportToPlot(player, plot);
        player.sendMessage("§aTeleportiert zu deinem Plot #" + plot.getId() + ".");
    }

    private void handleTrust(Player player, String targetName) {
        List<Plot> owned = plotManager.getPlotsOf(player.getUniqueId());
        if (owned.isEmpty()) {
            player.sendMessage("§cDu besitzt kein Plot.");
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cUngültiger Spieler.");
            return;
        }

        // Trust on all owned plots
        int count = 0;
        for (Plot p : owned) {
            if (plotManager.addTrust(p, target.getUniqueId())) count++;
        }

        String displayName = target.getName() != null ? target.getName() : targetName;
        if (count > 0) {
            player.sendMessage("§a" + displayName + " §adarf jetzt auf deinen Plots bauen.");
            Player online = Bukkit.getPlayer(target.getUniqueId());
            if (online != null) {
                online.sendMessage("§a" + player.getName() + " §ahat dir Baurechte auf seinen Plots gegeben.");
            }
        } else {
            player.sendMessage("§e" + displayName + " §ehat bereits Zugang zu deinen Plots.");
        }
    }

    private void handleUntrust(Player player, String targetName) {
        List<Plot> owned = plotManager.getPlotsOf(player.getUniqueId());
        if (owned.isEmpty()) {
            player.sendMessage("§cDu besitzt kein Plot.");
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null) {
            player.sendMessage("§cUngültiger Spieler.");
            return;
        }

        int count = 0;
        for (Plot p : owned) {
            if (plotManager.removeTrust(p, target.getUniqueId())) count++;
        }

        String displayName = target.getName() != null ? target.getName() : targetName;
        if (count > 0) {
            player.sendMessage("§e" + displayName + " §ehat keine Baurechte mehr auf deinen Plots.");
        } else {
            player.sendMessage("§c" + displayName + " §chatte keine Baurechte auf deinen Plots.");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void teleportToPlot(Player player, Plot plot) {
        World world = plotManager.getPlotWorld();
        Location loc = new Location(world, plot.getTeleportX(), Plot.Y_MAX + 1, plot.getTeleportZ(), 0f, 0f);
        player.teleport(loc);
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l── Plot-Befehle ─────────────────");
        player.sendMessage("§e/plot auto §7– Nächstes freies Plot beanspruchen");
        player.sendMessage("§e/plot info §7– Deine Plots anzeigen");
        player.sendMessage("§e/plot home §7– Zu deinem Plot teleportieren");
        player.sendMessage("§e/plot trust <Spieler> §7– Spieler als vertrauenswürdig einstufen");
        player.sendMessage("§e/plot untrust <Spieler> §7– Vertrauen entziehen");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(args[0].toLowerCase())) matches.add(sub);
            }
            return matches;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) names.add(p.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
