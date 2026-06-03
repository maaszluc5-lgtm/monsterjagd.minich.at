package at.minich.opserver.clan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Clan {

    public static final int MAX_MEMBERS = 20;
    public static final int MAX_NAME_LENGTH = 15;

    private final String name;
    private UUID leader;
    private final List<UUID> members;
    private final String createdAt;

    public Clan(String name, UUID leader, List<UUID> members, String createdAt) {
        this.name = name;
        this.leader = leader;
        this.members = new ArrayList<>(members);
        this.createdAt = createdAt;
    }

    public String getName() { return name; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public List<UUID> getMembers() { return members; }
    public String getCreatedAt() { return createdAt; }

    public boolean isMember(UUID uuid) { return members.contains(uuid); }
    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }
    public boolean isFull() { return members.size() >= MAX_MEMBERS; }

    public boolean addMember(UUID uuid) {
        if (isFull() || isMember(uuid)) return false;
        members.add(uuid);
        return true;
    }

    public boolean removeMember(UUID uuid) {
        return members.remove(uuid);
    }
}
