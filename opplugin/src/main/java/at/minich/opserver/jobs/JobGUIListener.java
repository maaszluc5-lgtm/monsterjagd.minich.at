package at.minich.opserver.jobs;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles clicks in the Jobs GUI.
 * Switching jobs requires a confirmation click within 5 seconds if the player
 * already has progress (level > 1 or actions > 0).
 */
public class JobGUIListener implements Listener {

    private final OpServerPlugin plugin;
    private final JobManager jobManager;
    private final JobGUI jobGUI;

    /** Pending confirmations: uuid -> (Job to switch to, timestamp) */
    private final Map<UUID, PendingSwitch> pendingSwitches = new HashMap<>();

    private static class PendingSwitch {
        final Job job;
        final long timestamp;

        PendingSwitch(Job job) {
            this.job = job;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 5_000;
        }
    }

    public JobGUIListener(OpServerPlugin plugin, JobGUI jobGUI) {
        this.plugin = plugin;
        this.jobManager = plugin.getJobManager();
        this.jobGUI = jobGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals(JobGUI.getGuiTitle())) return;

        // Cancel all clicks inside the GUI
        event.setCancelled(true);

        int slot = event.getRawSlot();

        // Close button
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        // Job icon clicks
        int[] jobSlots = JobGUI.getJobSlots();
        Job[] jobOrder = JobGUI.getJobOrder();
        for (int i = 0; i < jobSlots.length; i++) {
            if (slot == jobSlots[i]) {
                handleJobClick(player, jobOrder[i]);
                return;
            }
        }
    }

    private void handleJobClick(Player player, Job selected) {
        UUID uuid = player.getUniqueId();
        Job currentJob = jobManager.getJob(uuid);
        int currentLevel = jobManager.getLevel(uuid);
        long currentActions = jobManager.getActions(uuid);

        // Already on this job
        if (selected == currentJob) {
            player.sendMessage("§eDu bist bereits " + selected.getDisplayName() + "§e!");
            return;
        }

        // Check if player needs to confirm (has progress on current job)
        boolean hasProgress = currentJob != null && (currentLevel > 1 || currentActions > 0);

        if (hasProgress) {
            PendingSwitch pending = pendingSwitches.get(uuid);
            if (pending != null && !pending.isExpired() && pending.job == selected) {
                // Confirmed switch
                pendingSwitches.remove(uuid);
                performSwitch(player, uuid, selected);
                return;
            }
            // First click – request confirmation
            pendingSwitches.put(uuid, new PendingSwitch(selected));
            player.sendMessage("§c§lACHTUNG! §rDein Fortschritt als "
                    + currentJob.getDisplayName() + " §r§cwird zurückgesetzt!");
            player.sendMessage("§7Klicke erneut auf " + selected.getDisplayName()
                    + " §7innerhalb von §e5 Sekunden §7um zu bestätigen.");
            return;
        }

        // No progress, switch immediately
        pendingSwitches.remove(uuid);
        performSwitch(player, uuid, selected);
    }

    private void performSwitch(Player player, UUID uuid, Job newJob) {
        jobManager.setJob(uuid, newJob);
        player.sendMessage("§a§lBeruf gewechselt! §rDu bist jetzt "
                + newJob.getDisplayName() + "§r§a.");
        player.sendMessage("§7" + newJob.getDescription());
        // Refresh GUI
        jobGUI.open(player);
    }
}
