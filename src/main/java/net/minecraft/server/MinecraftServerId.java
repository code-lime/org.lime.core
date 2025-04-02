package net.minecraft.server;

import java.util.UUID;

public class MinecraftServerId {
    public static final UUID SERVER_ID = UUID.randomUUID();
    public static UUID serverId() {
        return SERVER_ID;
    }
}
