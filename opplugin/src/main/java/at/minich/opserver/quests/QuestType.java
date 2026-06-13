package at.minich.opserver.quests;

public enum QuestType {
    MINE_BLOCKS(100, 500, "Blöcke abbauen"),
    KILL_MOBS(20, 300, "Mobs töten"),
    WALK_DISTANCE(1000, 200, "Blöcke laufen"),
    FISH_ITEMS(10, 400, "Fische fangen"),
    CRAFT_ITEMS(50, 350, "Items craften"),
    PLACE_BLOCKS(100, 250, "Blöcke platzieren");

    public final int goal;
    public final double reward;
    public final String displayName;

    QuestType(int goal, double reward, String displayName) {
        this.goal = goal;
        this.reward = reward;
        this.displayName = displayName;
    }
}
