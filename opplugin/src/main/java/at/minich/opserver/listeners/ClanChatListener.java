package at.minich.opserver.listeners;

import at.minich.opserver.clan.Clan;
import at.minich.opserver.clan.ClanManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ClanChatListener implements Listener {

    private final ClanManager clanManager;

    public ClanChatListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Clan clan = clanManager.getClanOfPlayer(player.getUniqueId());
        if (clan == null) return;

        // Prepend clan tag to the format
        // Default format is: <%s> %s  — we just prepend our tag
        String originalFormat = event.getFormat();
        String clanTag = "§7[§6" + clan.getName() + "§7] ";
        event.setFormat(clanTag + originalFormat);
    }
}
