package at.minich.opserver.quests;

import at.minich.opserver.OpServerPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class QuestManager {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OpServerPlugin plugin;
    private final Map<UUID, List<Quest>> cache = new HashMap<>();

    public QuestManager(OpServerPlugin plugin) {
        this.plugin = plugin;
        scheduleReset();
    }

    private void scheduleReset() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new HashSet<>(cache.keySet())) {
                resetIfNewDay(uuid);
            }
        }, 20L * 60, 20L * 60);
    }

    public List<Quest> getPlayerQuests(UUID uuid) {
        resetIfNewDay(uuid);
        if (cache.containsKey(uuid)) return cache.get(uuid);

        YamlConfiguration cfg = plugin.getDataManager().loadYaml("quests/" + uuid + ".yml");
        String today = LocalDate.now().format(FMT);
        String date = cfg.getString("assignedDate", "");

        List<Quest> quests;
        if (today.equals(date) && cfg.contains("quests")) {
            quests = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String base = "quests." + i + ".";
                String typeName = cfg.getString(base + "type");
                if (typeName == null) continue;
                try {
                    QuestType type = QuestType.valueOf(typeName);
                    int progress = cfg.getInt(base + "progress", 0);
                    boolean completed = cfg.getBoolean(base + "completed", false);
                    quests.add(new Quest(type, progress, completed));
                } catch (IllegalArgumentException ignored) {}
            }
        } else {
            quests = assignRandom(uuid, today);
        }
        cache.put(uuid, quests);
        return quests;
    }

    private List<Quest> assignRandom(UUID uuid, String date) {
        List<QuestType> types = new ArrayList<>(Arrays.asList(QuestType.values()));
        Collections.shuffle(types);
        List<Quest> quests = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            quests.add(new Quest(types.get(i), 0, false));
        }
        save(uuid, quests, date);
        return quests;
    }

    public void resetIfNewDay(UUID uuid) {
        String today = LocalDate.now().format(FMT);
        YamlConfiguration cfg = plugin.getDataManager().loadYaml("quests/" + uuid + ".yml");
        String date = cfg.getString("assignedDate", "");
        if (!today.equals(date)) {
            cache.remove(uuid);
        }
    }

    public void addProgress(UUID uuid, QuestType type, int amount) {
        List<Quest> quests = getPlayerQuests(uuid);
        boolean changed = false;
        for (Quest q : quests) {
            if (q.getType() == type && !q.isCompleted()) {
                boolean wasDone = q.isCompleted();
                q.addProgress(amount);
                changed = true;
                if (!wasDone && q.isCompleted()) {
                    plugin.getEconomyManager().deposit(uuid, q.getReward());
                    org.bukkit.entity.Player p = plugin.getServer().getPlayer(uuid);
                    if (p != null) {
                        p.sendMessage("§a✔ Quest abgeschlossen: §e" + type.displayName + " §7(+" + (int) q.getReward() + " Coins)");
                    }
                }
                break;
            }
        }
        if (changed) save(uuid, quests, LocalDate.now().format(FMT));
    }

    private void save(UUID uuid, List<Quest> quests, String date) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("assignedDate", date);
        for (int i = 0; i < quests.size(); i++) {
            Quest q = quests.get(i);
            String base = "quests." + i + ".";
            cfg.set(base + "type", q.getType().name());
            cfg.set(base + "progress", q.getProgress());
            cfg.set(base + "completed", q.isCompleted());
        }
        plugin.getDataManager().saveYaml(cfg, "quests/" + uuid + ".yml");
    }
}
