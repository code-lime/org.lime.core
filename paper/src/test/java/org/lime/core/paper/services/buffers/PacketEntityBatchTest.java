package org.lime.core.paper.services.buffers;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.*;
import org.junit.jupiter.api.Test;
import org.lime.core.common.services.buffers.PacketEntityBatch;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PacketEntityBatchTest {
    @Test
    void flattensNestedBundlesAndPreservesOrderAtTheProtocolLimit() {
        for (int count : new int[]{4096, 4097}) {
            var sent = new ArrayList<ClientboundBundlePacket>();
            var viewer = new Object();
            var batch = new PacketEntityBatch<Object, Packet<? super ClientGamePacketListener>>(BundlerInfo.BUNDLE_SIZE_LIMIT,
                    (player, packets) -> sent.add(new ClientboundBundlePacket(packets)),
                    packet -> packet instanceof ClientboundBundlePacket bundle ? bundle.subPackets() : null);
            batch.begin(() -> {});
            batch.end(() -> {
                batch.send(viewer, new ClientboundBundlePacket(List.of(new ClientboundRemoveEntitiesPacket(0),
                        new ClientboundBundlePacket(List.of(new ClientboundRemoveEntitiesPacket(1))))));
                for (int id = 2; id < count; id++)
                    batch.send(viewer, new ClientboundRemoveEntitiesPacket(id));
                assertTrue(sent.isEmpty());
            });

            assertEquals(count == 4096 ? 1 : 2, sent.size());
            int id = 0;
            for (int index = 0; index < sent.size(); index++) {
                int size = 0;
                for (var packet : sent.get(index).subPackets()) {
                    var removal = assertInstanceOf(ClientboundRemoveEntitiesPacket.class, packet);
                    var bytes = new FriendlyByteBuf(Unpooled.buffer());
                    try {
                        ClientboundRemoveEntitiesPacket.STREAM_CODEC.encode(bytes, removal);
                        assertEquals(1, bytes.readVarInt());
                        assertEquals(id++, bytes.readVarInt());
                        assertFalse(bytes.isReadable());
                    } finally {
                        bytes.release();
                    }
                    size++;
                }
                assertEquals(index == 0 ? 4096 : 1, size);
            }
            assertEquals(count, id);
        }
    }

    @Test
    void discardsCollectionFailureAndStopsFlushAtTheFirstSendFailure() {
        var sends = new AtomicInteger();
        var failure = new IllegalStateException("send failed");
        var viewer = new Object();
        var batch = new PacketEntityBatch<Object, Packet<? super ClientGamePacketListener>>(BundlerInfo.BUNDLE_SIZE_LIMIT,
                (player, packets) -> {
                    sends.incrementAndGet();
                    throw failure;
                }, packet -> packet instanceof ClientboundBundlePacket bundle ? bundle.subPackets() : null);
        batch.begin(() -> {});
        assertSame(failure, assertThrows(IllegalStateException.class, () -> batch.end(() -> {
            batch.send(viewer, new ClientboundRemoveEntitiesPacket(1));
            throw failure;
        })));
        assertEquals(0, sends.get());

        batch.begin(() -> {});
        assertSame(failure, assertThrows(IllegalStateException.class, () -> batch.end(() -> {
            for (int id = 0; id < 4097; id++)
                batch.send(viewer, new ClientboundRemoveEntitiesPacket(id));
        })));
        assertEquals(1, sends.get());
        batch.begin(() -> {});
        batch.end(() -> {});
        assertEquals(1, sends.get());
    }
}
