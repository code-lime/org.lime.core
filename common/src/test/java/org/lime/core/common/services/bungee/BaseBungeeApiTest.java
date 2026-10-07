package org.lime.core.common.services.bungee;

import com.google.common.io.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BaseBungeeApiTest {
    @Test
    void writesProxyRequestsWithoutAdditionalFraming() throws IOException {
        TestApi api = new TestApi();
        api.connect("player", "lobby");
        assertEquals("bungeecord:main", BaseBungeeApi.CHANNEL_ID);
        assertEquals("player", api.player);
        assertArrayEquals(message(output -> {
            output.writeUTF("Connect");
            output.writeUTF("lobby");
        }), api.message);

        api.getPlayerCount("ALL");
        assertArrayEquals(message(output -> {
            output.writeUTF("PlayerCount");
            output.writeUTF("ALL");
        }), api.message);

        byte[] data = {0, 1, -1, 42};
        api.forward("ALL", "test", data);
        assertArrayEquals(message(output -> {
            output.writeUTF("Forward");
            output.writeUTF("ALL");
            output.writeUTF("test");
            output.writeShort(4);
            output.write(new byte[]{0, 1, -1, 42});
        }), api.message);
    }

    @Test
    void completesResponsesInOrderAndDispatchesForwardedBytes() throws IOException {
        TestApi api = new TestApi();
        var first = api.getPlayerCount("lobby");
        var second = api.getPlayerCount("lobby");
        api.receive(message(output -> {
            output.writeUTF("PlayerCount");
            output.writeUTF("lobby");
            output.writeInt(7);
        }));
        assertEquals(7, first.join());
        assertFalse(second.isDone());
        api.receive(message(output -> {
            output.writeUTF("PlayerCount");
            output.writeUTF("lobby");
            output.writeInt(12);
        }));
        assertEquals(12, second.join());

        var server = api.getServer();
        api.receive(message(output -> {
            output.writeUTF("GetServer");
            output.writeUTF("lobby");
        }));
        assertEquals("lobby", server.join());

        List<String> listeners = new ArrayList<>();
        api.registerForwardListener((channel, player, data) -> {
            listeners.add(channel + ":" + player);
            assertArrayEquals(new byte[]{0, -1, 42}, data);
        });
        api.registerForwardListener("test", (channel, player, data) -> listeners.add("local:" + channel));
        api.receive(message(output -> {
            output.writeUTF("test");
            output.writeShort(3);
            output.write(new byte[]{0, -1, 42});
        }));
        assertEquals(List.of("test:player", "local:test"), listeners);
    }

    private static byte[] message(MessageWriter writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            writer.write(output);
        }
        return bytes.toByteArray();
    }

    private interface MessageWriter {
        void write(DataOutputStream output) throws IOException;
    }

    private static final class TestApi extends BaseBungeeApi<Object> {
        private Object player;
        private byte[] message;

        @Override
        protected void sendBungeeMessage(Object player, ByteArrayDataOutput output) {
            this.player = player;
            message = output.toByteArray();
        }

        @Override
        protected Object getFirstPlayer() {
            return "player";
        }

        private void receive(byte[] message) {
            onPluginMessageReceived("player", ByteStreams.newDataInput(message));
        }
    }
}
