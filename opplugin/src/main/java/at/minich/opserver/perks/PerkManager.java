package at.minich.opserver.perks;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PerkManager {

    private final OpServerPlugin plugin;
    private final File perksDir;
    /** In-memory cache: uuid -> set of perks */
    private final Map<UUID, Set<Perk>> cache = new HashMap<>();

    public PerkManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        this.perksDir = new File(plugin.getDataFolder(), "perks");
        if (!perksDir.exists()) perksDir.mkdirs();
    }

    private File fileFor(UUID uuid) {
        return new File(perksDir, uuid.toString() + ".yml");
    }

    private Set<Perk> loadFromDisk(UUID uuid) {
        File f = fileFor(uuid);
        if (!f.exists()) return EnumSet.noneOf(Perk.class);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        List<String> list = cfg.getStringList("perks");
        Set<Perk> result = EnumSet.noneOf(Perk.class);
        for (String s : list) {
            try {
                result.add(Perk.valueOf(s));
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    private void saveToDisk(UUID uuid, Set<Perk> perks) {
        File f = fileFor(uuid);
        YamlConfiguration cfg = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (Perk p : perks) list.add(p.name());
        cfg.set("perks", list);
        try {
            cfg.save(f);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save perks for " + uuid + ": " + e.getMessage());
        }
    }

    private Set<Perk> getOrLoad(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromDisk);
    }

    public boolean hasPerk(UUID uuid, Perk perk) {
        return getOrLoad(uuid).contains(perk);
    }

    public void givePerk(UUID uuid, Perk perk) {
        Set<Perk> perks = getOrLoad(uuid);
        perks.add(perk);
        saveToDisk(uuid, perks);
    }

    public void removePerk(UUID uuid, Perk perk) {
        Set<Perk> perks = getOrLoad(uuid);
        perks.remove(perk);
        saveToDisk(uuid, perks);
    }

    public Set<Perk> getPerks(UUID uuid) {
        return Collections.unmodifiableSet(getOrLoad(uuid));
    }

    public void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
    }
}
