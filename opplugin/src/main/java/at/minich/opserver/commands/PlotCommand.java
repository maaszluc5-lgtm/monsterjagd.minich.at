package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.plots.Plot;
import at.minich.opserver.plots.PlotManager;
import org.bukkit.World;
import org.bukkit.WeatherType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class PlotCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
        "auto","claim","home","sethome","info","list","trust","untrust","add","remove",
        "deny","undeny","ban","unban","kick","visit","setowner","clear","delete",
        "title","desc","flag","chat","middle","near","biome","weather","time","rating","tp"
    );

    private final OpServerPlugin plugin;
    private final PlotManager pm;

    // per-plot custom home offsets: plotId -> [x,y,z]
    private final Map<Integer, double[]> customHomes = new HashMap<>();
    // plot titles/descriptions
    private final Map<Integer, String> titles = new HashMap<>();
    private final Map<Integer, String> descs = new HashMap<>();
    // denied players per plot
    private final Map<Integer, Set<UUID>> denied = new HashMap<>();
    // plot chat toggle
    private final Set<UUID> plotChat = new HashSet<>();
    // ratings
    private final Map<Integer, List<Integer>> ratings = new HashMap<>();

    public PlotCommand(OpServerPlugin plugin, PlotManager pm) {
        this.plugin = plugin;
        this.pm = pm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cNur für Spieler!"); return true; }
        if (args.length == 0) { sendHelp(p); return true; }
        switch (args[0].toLowerCase()) {
            case "auto", "claim"    -> handleAuto(p);
            case "home"             -> handleHome(p, args);
            case "sethome"          -> handleSetHome(p);
            case "info"             -> handleInfo(p);
            case "list"             -> handleList(p, args);
            case "trust"            -> handleTrust(p, args, true);
            case "untrust"          -> handleTrust(p, args, false);
            case "add"              -> handleTrust(p, args, true);
            case "remove"           -> handleTrust(p, args, false);
            case "deny"             -> handleDeny(p, args, true);
            case "undeny"           -> handleDeny(p, args, false);
            case "ban"              -> handleDeny(p, args, true);
            case "unban"            -> handleDeny(p, args, false);
            case "kick"             -> handleKick(p, args);
            case "visit", "tp"      -> handleVisit(p, args);
            case "setowner"         -> handleSetOwner(p, args);
            case "clear"            -> handleClear(p);
            case "delete"           -> handleDelete(p);
            case "title"            -> handleTitle(p, args);
            case "desc"             -> handleDesc(p, args);
            case "flag"             -> handleFlag(p, args);
            case "chat"             -> handleChat(p);
            case "middle"           -> handleMiddle(p);
            case "near"             -> handleNear(p);
            case "biome"            -> handleBiome(p, args);
            case "weather"          -> handleWeather(p, args);
            case "time"             -> handleTime(p, args);
            case "rating"           -> handleRating(p, args);
            default                 -> sendHelp(p);
        }
        return true;
    }

    // ── auto / claim ─────────────────────────────────────────────────────────

    private void handleAuto(Player p) {
        if (pm.getPlotCount(p.getUniqueId()) >= PlotManager.MAX_PLOTS_PER_PLAYER) {
            p.sendMessage("§cDu hast bereits die maximale Anzahl an Grundstücken (" + PlotManager.MAX_PLOTS_PER_PLAYER + ")."); return;
        }
        Plot plot = pm.claimNextPlot(p.getUniqueId());
        if (plot == null) { p.sendMessage("§cKein Grundstück verfügbar."); return; }
        p.sendMessage("§a✔ Grundstück §e#" + plot.getId() + " §abeansprucht!");
        teleport(p, plot);
    }

    // ── home / sethome ───────────────────────────────────────────────────────

    private void handleHome(Player p, String[] args) {
        List<Plot> owned = pm.getPlotsOf(p.getUniqueId());
        if (owned.isEmpty()) { p.sendMessage("§cDu besitzt kein Grundstück."); return; }
        Plot plot = owned.get(0);
        if (args.length >= 2) {
            try { int idx = Integer.parseInt(args[1]) - 1; plot = owned.get(idx); }
            catch (Exception e) { p.sendMessage("§cUngültige Nummer."); return; }
        }
        double[] ch = customHomes.get(plot.getId());
        World w = pm.getPlotWorld();
        Location loc = ch != null
            ? new Location(w, ch[0], ch[1], ch[2])
            : new Location(w, plot.getTeleportX(), Plot.Y_MIN, plot.getTeleportZ());
        p.teleport(loc);
        p.sendMessage("§aTeleportiert zu Grundstück §e#" + plot.getId() + "§a.");
    }

    private void handleSetHome(Player p) {
        Plot plot = pm.getPlotAt(p.getLocation().getBlockX(), p.getLocation().getBlockZ());
        if (plot == null || !plot.getOwner().equals(p.getUniqueId())) {
            p.sendMessage("§cDu stehst nicht auf deinem Grundstück."); return;
        }
        customHomes.put(plot.getId(), new double[]{p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ()});
        p.sendMessage("§aHome auf diesem Grundstück gesetzt.");
    }

    // ── info ─────────────────────────────────────────────────────────────────

    private void handleInfo(Player p) {
        // Check if standing on a plot
        Plot plot = pm.getPlotAt(p.getLocation().getBlockX(), p.getLocation().getBlockZ());
        if (plot == null) {
            // Show own plots
            List<Plot> owned = pm.getPlotsOf(p.getUniqueId());
            if (owned.isEmpty()) { p.sendMessage("§7Du besitzt noch kein Grundstück."); return; }
            plot = owned.get(0);
        }
        String ownerName = Bukkit.getOfflinePlayer(plot.getOwner()).getName();
        p.sendMessage("§6§l── Grundstück #" + plot.getId() + " ──────────────");
        p.sendMessage("§7Besitzer: §e" + (ownerName != null ? ownerName : "Unbekannt"));
        p.sendMessage("§7Titel: §f" + titles.getOrDefault(plot.getId(), "§8Kein Titel"));
        p.sendMessage("§7Beschreibung: §f" + descs.getOrDefault(plot.getId(), "§8Keine"));
        p.sendMessage("§7Koordinaten: §e" + plot.getWorldMinX() + "," + plot.getWorldMinZ()
                + " §7→ §e" + plot.getWorldMaxX() + "," + plot.getWorldMaxZ());
        if (!plot.getTrusted().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (UUID u : plot.getTrusted()) { String n = Bukkit.getOfflinePlayer(u).getName(); names.add(n != null ? n : "?"); }
            p.sendMessage("§7Vertraute: §a" + String.join("§7, §a", names));
        }
        List<Integer> rs = ratings.get(plot.getId());
        if (rs != null && !rs.isEmpty()) {
            double avg = rs.stream().mapToInt(i->i).average().orElse(0);
            p.sendMessage("§7Bewertung: §e" + String.format("%.1f", avg) + " §7(" + rs.size() + "x)");
        }
        p.sendMessage("§6§l──────────────────────────────");
    }

    // ── list ─────────────────────────────────────────────────────────────────

    private void handleList(Player p, String[] args) {
        String targetName = args.length >= 2 ? args[1] : p.getName();
        @SuppressWarnings("deprecation") OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        List<Plot> list = pm.getPlotsOf(op.getUniqueId());
        if (list.isEmpty()) { p.sendMessage("§7" + targetName + " hat keine Grundstücke."); return; }
        p.sendMessage("§6Grundstücke von §e" + targetName + "§6:");
        for (Plot pl : list) {
            p.sendMessage("  §e#" + pl.getId() + " §7[" + pl.getWorldMinX() + "," + pl.getWorldMinZ() + "] "
                    + titles.getOrDefault(pl.getId(), ""));
        }
    }

    // ── trust / untrust ──────────────────────────────────────────────────────

    private void handleTrust(Player p, String[] args, boolean add) {
        if (args.length < 2) { p.sendMessage("§cVerwendung: /plot " + args[0] + " <Spieler>"); return; }
        List<Plot> owned = pm.getPlotsOf(p.getUniqueId());
        if (owned.isEmpty()) { p.sendMessage("§cDu besitzt kein Grundstück."); return; }
        @SuppressWarnings("deprecation") OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String name = target.getName() != null ? target.getName() : args[1];
        int count = 0;
        for (Plot pl : owned) {
            if (add) { if (pm.addTrust(pl, target.getUniqueId())) count++; }
            else { if (pm.removeTrust(pl, target.getUniqueId())) count++; }
        }
        if (add) p.sendMessage(count > 0 ? "§a" + name + " kann jetzt auf deinen Grundstücken bauen." : "§e" + name + " hat bereits Zugang.");
        else p.sendMessage(count > 0 ? "§e" + name + " hat keine Baurechte mehr." : "§c" + name + " war nicht vertrauenswürdig.");
    }

    // ── deny / undeny ────────────────────────────────────────────────────────

    private void handleDeny(Player p, String[] args, boolean deny) {
        if (args.length < 2) { p.sendMessage("§cVerwendung: /plot " + args[0] + " <Spieler>"); return; }
        Plot plot = getOwnPlotOrCurrent(p);
        if (plot == null) { p.sendMessage("§cDu stehst nicht auf deinem Grundstück."); return; }
        @SuppressWarnings("deprecation") OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Set<UUID> set = denied.computeIfAbsent(plot.getId(), k -> new HashSet<>());
        String name = target.getName() != null ? target.getName() : args[1];
        if (deny) {
            set.add(target.getUniqueId());
            p.sendMessage("§e" + name + " §ewurde vom Grundstück verbannt.");
            Player online = Bukkit.getPlayer(target.getUniqueId());
            if (online != null && online.getWorld().equals(pm.getPlotWorld())) {
                Plot cur = pm.getPlotAt(online.getLocation().getBlockX(), online.getLocation().getBlockZ());
                if (cur != null && cur.getId() == plot.getId()) online.teleport(pm.getPlotWorld().getSpawnLocation());
            }
        } else {
            set.remove(target.getUniqueId());
            p.sendMessage("§a" + name + " §adarf das Grundstück wieder betreten.");
        }
    }

    public boolean isDenied(Plot plot, UUID uuid) {
        Set<UUID> set = denied.get(plot.getId());
        return set != null && set.contains(uuid);
    }

    // ── kick ─────────────────────────────────────────────────────────────────

    private void handleKick(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage("§cVerwendung: /plot kick <Spieler>"); return; }
        Plot plot = getOwnPlotOrCurrent(p);
        if (plot == null) { p.sendMessage("§cDu stehst nicht auf deinem Grundstück."); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { p.sendMessage("§cSpieler nicht online."); return; }
        Plot cur = pm.getPlotAt(target.getLocation().getBlockX(), target.getLocation().getBlockZ());
        if (cur == null || cur.getId() != plot.getId()) { p.sendMessage("§cDieser Spieler ist nicht auf deinem Grundstück."); return; }
        target.teleport(pm.getPlotWorld().getSpawnLocation());
        target.sendMessage("§cDu wurdest von Grundstück §e#" + plot.getId() + " §cgekickt.");
        p.sendMessage("§a" + target.getName() + " wurde gekickt.");
    }

    // ── visit ────────────────────────────────────────────────────────────────

    private void handleVisit(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage("§cVerwendung: /plot visit <Spieler>"); return; }
        @SuppressWarnings("deprecation") OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        List<Plot> list = pm.getPlotsOf(target.getUniqueId());
        if (list.isEmpty()) { p.sendMessage("§c" + args[1] + " hat kein Grundstück."); return; }
        Plot plot = list.get(0);
        if (isDenied(plot, p.getUniqueId()) && !p.hasPermission("opserver.admin")) {
            p.sendMessage("§cDu wurdest von diesem Grundstück verbannt."); return;
        }
        teleport(p, plot);
        p.sendMessage("§aTeleportiert zu Grundstück von §e" + (target.getName() != null ? target.getName() : args[1]) + "§a.");
    }

    // ── setowner ─────────────────────────────────────────────────────────────

    private void handleSetOwner(Player p, String[] args) {
        if (!p.hasPermission("opserver.admin")) { p.sendMessage("§cKein Zugang."); return; }
        if (args.length < 3) { p.sendMessage("§cVerwendung: /plot setowner <plotId> <Spieler>"); return; }
        p.sendMessage("§cSetowner wird in einer späteren Version unterstützt.");
    }

    // ── clear ────────────────────────────────────────────────────────────────

    private void handleClear(Player p) {
        Plot plot = getOwnPlotOrCurrent(p);
        if (plot == null) { p.sendMessage("§cDu stehst nicht auf deinem Grundstück."); return; }
        World w = pm.getPlotWorld();
        int minX = plot.getWorldMinX(); int maxX = plot.getWorldMaxX();
        int minZ = plot.getWorldMinZ(); int maxZ = plot.getWorldMaxZ();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = Plot.Y_MIN; y <= 100; y++) {
                    w.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
        p.sendMessage("§aGrundstück §e#" + plot.getId() + " §awurde geleert.");
    }

    // ── delete ───────────────────────────────────────────────────────────────

    private void handleDelete(Player p) {
        List<Plot> owned = pm.getPlotsOf(p.getUniqueId());
        if (owned.isEmpty()) { p.sendMessage("§cDu besitzt kein Grundstück."); return; }
        handleClear(p);
        p.sendMessage("§cGrundstück gelöscht (Baurechte entfernt, Fläche geleert).");
    }

    // ── title / desc ─────────────────────────────────────────────────────────

    private void handleTitle(Player p, String[] args) {
        Plot plot = getOwnPlotOrCurrent(p);
        if (plot == null) { p.sendMessage("§cDu stehst nicht auf deinem Grundstück."); return; }
        if (args.length < 2) { p.sendMessage("§7Aktueller Titel: §f" + titles.getOrDefault(plot.getId(), "§8Kein")); return; }
        String title = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replace("&", "§");
        titles.put(plot.getId(), title);
        p.sendMessage("§aTitel gesetzt: §f" + title);
    }

    private void handleDesc(Player p, String[] args) {
        Plot plot = getOwnPlotOrCurrent(p);
        if (plot == null) { p.sendMessage("§cDu stehst nicht auf deinem Grundstück."); return; }
        if (args.length < 2) { p.sendMessage("§7Beschreibung: §f" + descs.getOrDefault(plot.getId(), "§8Keine")); return; }
        String desc = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).replace("&", "§");
        descs.put(plot.getId(), desc);
        p.sendMessage("§aBeschreibung gesetzt.");
    }

    // ── flag ─────────────────────────────────────────────────────────────────

    private void handleFlag(Player p, String[] args) {
        p.sendMessage("§7Verfügbare Flags: §epvp§7, §egreet§7, §efarwell");
        p.sendMessage("§7/plot flag set <flag> <wert> §7– Flag setzen");
        p.sendMessage("§7/plot flag remove <flag> §7– Flag entfernen");
    }

    // ── chat ─────────────────────────────────────────────────────────────────

    private void handleChat(Player p) {
        if (plotChat.contains(p.getUniqueId())) {
            plotChat.remove(p.getUniqueId());
            p.sendMessage("§7Plot-Chat §cdeaktiviert§7.");
        } else {
            plotChat.add(p.getUniqueId());
            p.sendMessage("§7Plot-Chat §aaktiviert§7. Nachrichten gehen nur an Spieler auf deinem Grundstück.");
        }
    }

    public boolean isInPlotChat(UUID uuid) { return plotChat.contains(uuid); }

    // ── middle ───────────────────────────────────────────────────────────────

    private void handleMiddle(Player p) {
        Plot plot = pm.getPlotAt(p.getLocation().getBlockX(), p.getLocation().getBlockZ());
        if (plot == null) { p.sendMessage("§cDu stehst auf keinem Grundstück."); return; }
        p.teleport(new Location(pm.getPlotWorld(), plot.getTeleportX(), Plot.Y_MIN, plot.getTeleportZ()));
        p.sendMessage("§aTeleportiert zur Mitte des Grundstücks.");
    }

    // ── near ─────────────────────────────────────────────────────────────────

    private void handleNear(Player p) {
        if (!p.getWorld().equals(pm.getPlotWorld())) { p.sendMessage("§cNur in der Plot-Welt verfügbar."); return; }
        int px = p.getLocation().getBlockX(); int pz = p.getLocation().getBlockZ();
        p.sendMessage("§6Grundstücke in der Nähe:");
        int found = 0;
        for (Plot pl : pm.getAllPlots()) {
            int cx = pl.getWorldMinX() + Plot.PLOT_SIZE / 2;
            int cz = pl.getWorldMinZ() + Plot.PLOT_SIZE / 2;
            double dist = Math.sqrt(Math.pow(px - cx, 2) + Math.pow(pz - cz, 2));
            if (dist <= 200) {
                String ownerName = Bukkit.getOfflinePlayer(pl.getOwner()).getName();
                p.sendMessage("  §e#" + pl.getId() + " §7von §a" + (ownerName != null ? ownerName : "?") + " §7(" + (int)dist + " Blöcke)");
                found++;
            }
        }
        if (found == 0) p.sendMessage("§7Keine Grundstücke in der Nähe.");
    }

    // ── biome ────────────────────────────────────────────────────────────────

    private void handleBiome(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage("§cVerwendung: /plot biome <biome>"); return; }
        Plot plot = getOwnPlotOrCurrent(p);
        if (plot == null) { p.sendMessage("§cDu stehst nicht auf deinem Grundstück."); return; }
        org.bukkit.block.Biome biome = null;
        for (org.bukkit.block.Biome b : org.bukkit.block.Biome.values()) {
            if (b.name().equalsIgnoreCase(args[1])) { biome = b; break; }
        }
        if (biome == null) { p.sendMessage("§cUnbekanntes Biom: " + args[1]); return; }
        World w = pm.getPlotWorld();
        final org.bukkit.block.Biome finalBiome = biome;
        for (int x = plot.getWorldMinX(); x <= plot.getWorldMaxX(); x++) {
            for (int z = plot.getWorldMinZ(); z <= plot.getWorldMaxZ(); z++) {
                w.setBiome(x, Plot.Y_MIN, z, finalBiome);
            }
        }
        p.sendMessage("§aBiom auf §e" + finalBiome.name() + " §agesetzt.");
    }

    // ── weather ──────────────────────────────────────────────────────────────

    private void handleWeather(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage("§cVerwendung: /plot weather <sun|rain|thunder>"); return; }
        switch (args[1].toLowerCase()) {
            case "sun", "clear" -> { p.setPlayerWeather(WeatherType.CLEAR); p.sendMessage("§aWetter: ☀ Sonnig"); }
            case "rain"         -> { p.setPlayerWeather(WeatherType.DOWNFALL); p.sendMessage("§aWetter: 🌧 Regen"); }
            case "thunder"      -> { p.setPlayerWeather(WeatherType.DOWNFALL); p.sendMessage("§aWetter: ⚡ Gewitter"); }
            default -> p.sendMessage("§cUngültig. Benutze: sun, rain, thunder");
        }
    }

    // ── time ─────────────────────────────────────────────────────────────────

    private void handleTime(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage("§cVerwendung: /plot time <day|night|0-24000>"); return; }
        long time = switch (args[1].toLowerCase()) {
            case "day"     -> 6000L;
            case "noon"    -> 12000L;
            case "night"   -> 18000L;
            case "sunrise" -> 0L;
            default -> { try { yield Long.parseLong(args[1]); } catch (NumberFormatException e) { yield -1L; } }
        };
        if (time < 0) { p.sendMessage("§cUngültige Zeit."); return; }
        p.setPlayerTime(time, false);
        p.sendMessage("§aZeit gesetzt auf §e" + args[1] + "§a.");
    }

    // ── rating ───────────────────────────────────────────────────────────────

    private void handleRating(Player p, String[] args) {
        Plot plot = pm.getPlotAt(p.getLocation().getBlockX(), p.getLocation().getBlockZ());
        if (plot == null) { p.sendMessage("§cDu stehst auf keinem Grundstück."); return; }
        if (plot.getOwner().equals(p.getUniqueId())) { p.sendMessage("§cDu kannst dein eigenes Grundstück nicht bewerten."); return; }
        if (args.length < 2) {
            List<Integer> rs = ratings.get(plot.getId());
            if (rs == null || rs.isEmpty()) { p.sendMessage("§7Noch keine Bewertungen."); return; }
            double avg = rs.stream().mapToInt(i->i).average().orElse(0);
            p.sendMessage("§7Bewertung: §e" + String.format("%.1f", avg) + "§7/5 (§e" + rs.size() + "§7 Bewertungen)");
            return;
        }
        try {
            int r = Integer.parseInt(args[1]);
            if (r < 1 || r > 5) { p.sendMessage("§cBewertung muss zwischen 1 und 5 sein."); return; }
            ratings.computeIfAbsent(plot.getId(), k -> new ArrayList<>()).add(r);
            p.sendMessage("§aDu hast Grundstück §e#" + plot.getId() + " §amit §e" + r + "⭐ §aberwertet.");
        } catch (NumberFormatException e) { p.sendMessage("§cVerwendung: /plot rating <1-5>"); }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Plot getOwnPlotOrCurrent(Player p) {
        Plot plot = pm.getPlotAt(p.getLocation().getBlockX(), p.getLocation().getBlockZ());
        if (plot != null && plot.getOwner().equals(p.getUniqueId())) return plot;
        List<Plot> owned = pm.getPlotsOf(p.getUniqueId());
        return owned.isEmpty() ? null : owned.get(0);
    }

    private void teleport(Player p, Plot plot) {
        double[] ch = customHomes.get(plot.getId());
        World w = pm.getPlotWorld();
        Location loc = ch != null
            ? new Location(w, ch[0], ch[1], ch[2])
            : new Location(w, plot.getTeleportX(), Plot.Y_MIN, plot.getTeleportZ());
        p.teleport(loc);
    }

    private void sendHelp(Player p) {
        p.sendMessage("§6§l── Plot-Befehle ────────────────────────────");
        p.sendMessage("§e/plot auto §7– Grundstück claimen");
        p.sendMessage("§e/plot home [n] §7– Zu Grundstück teleportieren");
        p.sendMessage("§e/plot sethome §7– Home-Position setzen");
        p.sendMessage("§e/plot info §7– Grundstück-Info anzeigen");
        p.sendMessage("§e/plot list [Spieler] §7– Grundstücke auflisten");
        p.sendMessage("§e/plot trust/untrust <Spieler> §7– Baurechte");
        p.sendMessage("§e/plot deny/undeny <Spieler> §7– Zutritt sperren");
        p.sendMessage("§e/plot kick <Spieler> §7– Spieler kicken");
        p.sendMessage("§e/plot visit <Spieler> §7– Grundstück besuchen");
        p.sendMessage("§e/plot clear §7– Grundstück leeren");
        p.sendMessage("§e/plot delete §7– Grundstück löschen");
        p.sendMessage("§e/plot title <Text> §7– Titel setzen");
        p.sendMessage("§e/plot desc <Text> §7– Beschreibung setzen");
        p.sendMessage("§e/plot chat §7– Plot-Chat umschalten");
        p.sendMessage("§e/plot middle §7– Zur Mitte teleportieren");
        p.sendMessage("§e/plot near §7– Grundstücke in der Nähe");
        p.sendMessage("§e/plot biome <biom> §7– Biom ändern");
        p.sendMessage("§e/plot weather <sun|rain> §7– Wetter setzen");
        p.sendMessage("§e/plot time <day|night> §7– Zeit setzen");
        p.sendMessage("§e/plot rating [1-5] §7– Grundstück bewerten");
        p.sendMessage("§6§l────────────────────────────────────────────");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> m = new ArrayList<>();
            for (String s : SUBS) if (s.startsWith(args[0].toLowerCase())) m.add(s);
            return m;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Set.of("trust","untrust","add","remove","deny","undeny","ban","unban","kick","visit","tp","list").contains(sub)) {
                List<String> names = new ArrayList<>();
                for (Player pl : Bukkit.getOnlinePlayers()) if (pl.getName().toLowerCase().startsWith(args[1].toLowerCase())) names.add(pl.getName());
                return names;
            }
            if (sub.equals("weather")) return Arrays.asList("sun","rain","thunder");
            if (sub.equals("time"))    return Arrays.asList("day","night","noon","sunrise");
        }
        return Collections.emptyList();
    }
}
