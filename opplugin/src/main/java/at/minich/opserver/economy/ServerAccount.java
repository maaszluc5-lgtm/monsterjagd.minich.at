package at.minich.opserver.economy;

import java.util.UUID;

public final class ServerAccount {
    public static final UUID SERVER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID BANK   = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID MARKT  = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private ServerAccount() {}
}
