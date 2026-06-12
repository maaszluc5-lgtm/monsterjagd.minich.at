package at.minich.opserver.jobs;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class JobGUIListener implements Listener {

    private final OpServerPlugin plugin;
    private final JobGUI jobGUI;

    public JobGUIListener(OpServerPlugin plugin, JobGUI jobGUI) {
        this.plugin = plugin;
        this.jobGUI = jobGUI;
    }

    // Right-click with any job icon item opens the GUI
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().name().contains("RIGHT")) return;
        if (event.getItem() == null) return;

        for (Job job : Job.values()) {
            if (event.getItem().getType() == job.getIcon()) {
                jobGUI.open(event.getPlayer());
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(JobGUI.getGuiTitle())) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot == 49) { player.closeInventory(); }
    }
}
