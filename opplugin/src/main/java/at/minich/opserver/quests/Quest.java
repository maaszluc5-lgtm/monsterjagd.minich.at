package at.minich.opserver.quests;

public class Quest {
    private final QuestType type;
    private int progress;
    private boolean completed;

    public Quest(QuestType type, int progress, boolean completed) {
        this.type = type;
        this.progress = progress;
        this.completed = completed;
    }

    public QuestType getType() { return type; }
    public int getProgress() { return progress; }
    public int getGoal() { return type.goal; }
    public double getReward() { return type.reward; }
    public boolean isCompleted() { return completed; }

    public void addProgress(int amount) {
        if (completed) return;
        progress = Math.min(progress + amount, type.goal);
        if (progress >= type.goal) completed = true;
    }
}
