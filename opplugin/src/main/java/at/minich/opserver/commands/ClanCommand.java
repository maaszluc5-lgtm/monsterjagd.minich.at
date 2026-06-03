package at.minich.opserver.commands;

import at.minich.opserver.OpServerPlugin;
import at.minich.opserver.clan.Clan;
import at.minich.opserver.clan.ClanManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class ClanCommand implements CommandExecutor {

    private static final double CREATE_COST = 5000.0;

    private final OpServerPlugin plugin;
    private final ClanManager clanManager;

    public ClanCommand(OpServerPlugin plugin, ClanManager clanManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "§8[§6OpServer§8] §r");

        if (!(sender instanceof Player player)) {
            // Allow /cc from console? No — clan chat requires player
            sender.sendMessage(prefix + "§cOnly players can use this command.");
            return true;
        }

        // /cc alias routing
        if (label.equalsIgnoreCase("cc") || label.equalsIgnoreCase("clanchat")) {
            return handleChat(player, args, prefix, "");
        }

        if (args.length < 1) {
            sendHelp(player, prefix);
            return true;
        }

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "create"  -> handleCreate(player, args, prefix);
            case "invite"  -> handleInvite(player, args, prefix);
            case "join"    -> handleJoin(player, args, prefix);
            case "leave"   -> handleLeave(player, prefix);
            case "kick"    -> handleKick(player, args, prefix);
            case "disband" -> handleDisband(player, prefix);
            case "info"    -> handleInfo(player, args, prefix);
            case "list"    -> handleList(player, prefix);
            case "chat"    -> handleChat(player, java.util.Arrays.copyOfRange(args, 1, args.length), prefix, "");
            default -> { sendHelp(player, prefix); yield true; }
        };
    }

    private boolean handleCreate(Player player, String[] args, String prefix) {
        if (args.length < 2) {
            player.sendMessage(prefix + "§cUsage: /clan create <name>");
            return true;
        }
        String name = args[1];
        if (name.length() > Clan.MAX_NAME_LENGTH) {
            player.sendMessage(prefix + "§cClan-Name darf maximal " + Clan.MAX_NAME_LENGTH + " Zeichen lang sein.");
            return true;
        }
        if (clanManager.getClanOfPlayer(player.getUniqueId()) != null) {
            player.sendMessage(prefix + "§cDu bist bereits in einem Clan.");
            return true;
        }
        if (clanManager.clanExists(name)) {
            player.sendMessage(prefix + "§cEin Clan mit diesem Namen existiert bereits.");
            return true;
        }
        if (!plugin.getEconomyManager().has(player.getUniqueId(), CREATE_COST)) {
            player.sendMessage(prefix + "§cDu benötigst §e" + String.format("%.0f", CREATE_COST) + " Coins §cum einen Clan zu gründen.");
            return true;
        }
        plugin.getEconomyManager().withdraw(player.getUniqueId(), CREATE_COST);
        clanManager.createClan(name, player.getUniqueId());
        player.sendMessage(prefix + "§aClan §6" + name + " §agegründet! Kosten: §e" + String.format("%.0f", CREATE_COST) + " Coins");
        return true;
    }

    private boolean handleInvite(Player player, String[] args, String prefix) {
        if (args.length < 2) {
            player.sendMessage(prefix + "§cUsage: /clan invite <player>");
            return true;
        }
        Clan clan = clanManager.getClanOfPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(prefix + "§cDu bist in keinem Clan.");
            return true;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage(prefix + "§cNur der Clan-Leader kann Spieler einladen.");
            return true;
        }
        if (clan.isFull()) {
            player.sendMessage(prefix + "§cDein Clan ist voll (max " + Clan.MAX_MEMBERS + " Mitglieder).");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(prefix + "§cSpieler nicht gefunden.");
            return true;
        }
        if (clanManager.getClanOfPlayer(target.getUniqueId()) != null) {
            player.sendMessage(prefix + "§c" + target.getName() + " ist bereits in einem Clan.");
            return true;
        }
        clanManager.addInvite(target.getUniqueId(), clan.getName());
        player.sendMessage(prefix + "§aEinladung an §e" + target.getName() + " §agesendet.");
        target.sendMessage(prefix + "§eDu wurdest in den Clan §6" + clan.getName() + " §eeingeladen! Tippe §a/clan join " + clan.getName() + "§e.");
        return true;
    }

    private boolean handleJoin(Player player, String[] args, String prefix) {
        if (args.length < 2) {
            player.sendMessage(prefix + "§cUsage: /clan join <name>");
            return true;
        }
        String clanName = args[1];
        String invite = clanManager.getInvite(player.getUniqueId());
        if (invite == null || !invite.equalsIgnoreCase(clanName)) {
            player.sendMessage(prefix + "§cDu wurdest nicht in diesen Clan eingeladen.");
            return true;
        }
        Clan clan = clanManager.getClan(clanName);
        if (clan == null) {
            player.sendMessage(prefix + "§cClan nicht gefunden.");
            clanManager.removeInvite(player.getUniqueId());
            return true;
        }
        if (clanManager.getClanOfPlayer(player.getUniqueId()) != null) {
            player.sendMessage(prefix + "§cDu bist bereits in einem Clan.");
            return true;
        }
        if (clan.isFull()) {
            player.sendMessage(prefix + "§cDer Clan ist voll.");
            return true;
        }
        clan.addMember(player.getUniqueId());
        clanManager.removeInvite(player.getUniqueId());
        clanManager.save();
        // Notify clan members
        broadcastToClan(clan, prefix + "§e" + player.getName() + " §aist dem Clan beigetreten!");
        return true;
    }

    private boolean handleLeave(Player player, String prefix) {
        Clan clan = clanManager.getClanOfPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(prefix + "§cDu bist in keinem Clan.");
            return true;
        }
        if (clan.isLeader(player.getUniqueId())) {
            player.sendMessage(prefix + "§cAls Leader kannst du den Clan nicht verlassen. Nutze §e/clan disband§c.");
            return true;
        }
        clan.removeMember(player.getUniqueId());
        clanManager.save();
        player.sendMessage(prefix + "§aDu hast den Clan §6" + clan.getName() + " §averlassen.");
        broadcastToClan(clan, prefix + "§e" + player.getName() + " §chat den Clan verlassen.");
        return true;
    }

    private boolean handleKick(Player player, String[] args, String prefix) {
        if (args.length < 2) {
            player.sendMessage(prefix + "§cUsage: /clan kick <player>");
            return true;
        }
        Clan clan = clanManager.getClanOfPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(prefix + "§cDu bist in keinem Clan.");
            return true;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage(prefix + "§cNur der Leader kann Mitglieder rauswerfen.");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        UUID targetId;
        String targetName;
        if (target != null) {
            targetId = target.getUniqueId();
            targetName = target.getName();
        } else {
            player.sendMessage(prefix + "§cSpieler nicht gefunden oder nicht online.");
            return true;
        }
        if (!clan.isMember(targetId)) {
            player.sendMessage(prefix + "§c" + targetName + " ist nicht in deinem Clan.");
            return true;
        }
        if (clan.isLeader(targetId)) {
            player.sendMessage(prefix + "§cDu kannst dich nicht selbst rauswerfen.");
            return true;
        }
        clan.removeMember(targetId);
        clanManager.save();
        player.sendMessage(prefix + "§e" + targetName + " §cwurde aus dem Clan geworfen.");
        target.sendMessage(prefix + "§cDu wurdest aus dem Clan §6" + clan.getName() + " §cgeworfen.");
        broadcastToClan(clan, prefix + "§e" + targetName + " §cwurde aus dem Clan geworfen.");
        return true;
    }

    private boolean handleDisband(Player player, String prefix) {
        Clan clan = clanManager.getClanOfPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(prefix + "§cDu bist in keinem Clan.");
            return true;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage(prefix + "§cNur der Leader kann den Clan auflösen.");
            return true;
        }
        broadcastToClan(clan, prefix + "§cDer Clan §6" + clan.getName() + " §cwurde aufgelöst.");
        clanManager.disbandClan(clan.getName());
        return true;
    }

    private boolean handleInfo(Player player, String[] args, String prefix) {
        Clan clan;
        if (args.length >= 2) {
            clan = clanManager.getClan(args[1]);
            if (clan == null) {
                player.sendMessage(prefix + "§cClan nicht gefunden.");
                return true;
            }
        } else {
            clan = clanManager.getClanOfPlayer(player.getUniqueId());
            if (clan == null) {
                player.sendMessage(prefix + "§cDu bist in keinem Clan. Nutze /clan info <name>.");
                return true;
            }
        }

        Player leaderPlayer = Bukkit.getPlayer(clan.getLeader());
        String leaderName = leaderPlayer != null ? leaderPlayer.getName() : clan.getLeader().toString();

        player.sendMessage("§6§lClan: §e" + clan.getName());
        player.sendMessage("§7Leader: §e" + leaderName);
        player.sendMessage("§7Mitglieder: §e" + clan.getMembers().size() + "§7/§e" + Clan.MAX_MEMBERS);
        player.sendMessage("§7Gegründet: §e" + clan.getCreatedAt().substring(0, 10));

        StringBuilder members = new StringBuilder("§7Mitgliederliste: ");
        for (UUID m : clan.getMembers()) {
            Player mp = Bukkit.getPlayer(m);
            String mName = mp != null ? "§a" + mp.getName() : "§8" + m.toString().substring(0, 8);
            members.append(mName).append("§7, ");
        }
        String membersStr = members.toString();
        if (membersStr.endsWith("§7, ")) membersStr = membersStr.substring(0, membersStr.length() - 4);
        player.sendMessage(membersStr);
        return true;
    }

    private boolean handleList(Player player, String prefix) {
        Map<String, Clan> all = clanManager.getClansMap();
        if (all.isEmpty()) {
            player.sendMessage(prefix + "§7Es gibt noch keine Clans.");
            return true;
        }
        player.sendMessage("§6§lClanliste:");
        for (Map.Entry<String, Clan> entry : all.entrySet()) {
            Clan c = entry.getValue();
            player.sendMessage("  §6" + c.getName() + " §7(" + c.getMembers().size() + "/" + Clan.MAX_MEMBERS + ")");
        }
        return true;
    }

    private boolean handleChat(Player player, String[] args, String prefix, String ignored) {
        if (args.length < 1) {
            player.sendMessage(prefix + "§cUsage: /clan chat <message> oder /cc <message>");
            return true;
        }
        Clan clan = clanManager.getClanOfPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(prefix + "§cDu bist in keinem Clan.");
            return true;
        }
        StringBuilder msgBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) msgBuilder.append(" ");
            msgBuilder.append(args[i]);
        }
        String chatMsg = "§7[§6" + clan.getName() + "§7] §f" + player.getName() + "§7: §f" + msgBuilder;
        broadcastToClan(clan, chatMsg);
        return true;
    }

    private void broadcastToClan(Clan clan, String message) {
        for (UUID m : clan.getMembers()) {
            Player mp = Bukkit.getPlayer(m);
            if (mp != null && mp.isOnline()) mp.sendMessage(message);
        }
    }

    private void sendHelp(Player player, String prefix) {
        player.sendMessage(prefix + "§6/clan create <name> §7- Clan gründen (" + String.format("%.0f", CREATE_COST) + " Coins)");
        player.sendMessage(prefix + "§6/clan invite <player> §7- Spieler einladen");
        player.sendMessage(prefix + "§6/clan join <name> §7- Clan beitreten");
        player.sendMessage(prefix + "§6/clan leave §7- Clan verlassen");
        player.sendMessage(prefix + "§6/clan kick <player> §7- Mitglied rauswerfen");
        player.sendMessage(prefix + "§6/clan disband §7- Clan auflösen");
        player.sendMessage(prefix + "§6/clan info [name] §7- Clan-Info");
        player.sendMessage(prefix + "§6/clan list §7- Alle Clans");
        player.sendMessage(prefix + "§6/clan chat <msg> §7- Clan-Chat (Alias: /cc)");
    }
}
