package at.minich.opserver.quests;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuestListener implements Listener {

    private final OpServerPlugin plugin;
    private final Map<UUID, org.bukkit.Location> lastLoc = new HashMap<>();

    public QuestListener(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        plugin.getQuestManager().addProgress(e.getPlayer().getUniqueId(), QuestType.MINE_BLOCKS, 1);
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() instanceof Player p) {
            plugin.getQuestManager().addProgress(p.getUniqueId(), QuestType.KILL_MOBS, 1);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        org.bukkit.Location from = e.getFrom();
        org.bukkit.Location to = e.getTo();
        if (to == null) return;
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0.1) {
            org.bukkit.Location prev = lastLoc.get(uuid);
            double walked = (prev != null) ? prev.distance(to) : dist;
            lastLoc.put(uuid, to.clone());
            if (walked > 0.5) {
                plugin.getQuestManager().addProgress(uuid, QuestType.WALK_DISTANCE, (int) walked);
            }
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            plugin.getQuestManager().addProgress(e.getPlayer().getUniqueId(), QuestType.FISH_ITEMS, 1);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            int amount = e.getRecipe().getResult().getAmount();
            plugin.getQuestManager().addProgress(p.getUniqueId(), QuestType.CRAFT_ITEMS, amount);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        plugin.getQuestManager().addProgress(e.getPlayer().getUniqueId(), QuestType.PLACE_BLOCKS, 1);
    }
}
