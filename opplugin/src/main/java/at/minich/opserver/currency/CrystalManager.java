package at.minich.opserver.currency;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.UUID;

public class CrystalManager {

    private final OpServerPlugin plugin;

    public CrystalManager(OpServerPlugin plugin) {
        this.plugin = plugin;
    }

    private String path(UUID uuid) {
        return "players/" + uuid + ".yml";
    }

    public long getCrystals(UUID uuid) {
        YamlConfiguration cfg = plugin.getDataManager().loadYaml(path(uuid));
        return Math.max(0, cfg.getLong("crystals", 0));
    }

    public void setCrystals(UUID uuid, long amount) {
        if (amount < 0) amount = 0;
        String p = path(uuid);
        YamlConfiguration cfg = plugin.getDataManager().loadYaml(p);
        cfg.set("crystals", amount);
        plugin.getDataManager().saveYaml(cfg, p);
    }

    public void addCrystals(UUID uuid, long amount) {
        if (amount <= 0) return;
        setCrystals(uuid, getCrystals(uuid) + amount);
    }

    /** Returns false if the player does not have enough crystals. */
    public boolean removeCrystals(UUID uuid, long amount) {
        if (amount <= 0) return true;
        long current = getCrystals(uuid);
        if (current < amount) return false;
        setCrystals(uuid, current - amount);
        return true;
    }
}
