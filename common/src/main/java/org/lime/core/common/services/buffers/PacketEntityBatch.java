package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/** Collects one packet entity buffer's outbound packets by viewer. */
public class PacketEntityBatch<Viewer, Packet> {
    private enum Phase {
        IDLE,
        BUFFERING,
        COLLECTING
    }

    private final int packetLimit;
    private final BiConsumer<Viewer, List<Packet>> bundleSender;
    private final Function<Packet, @Nullable Iterable<? extends Packet>> subPackets;
    private final Map<Viewer, ArrayList<Packet>> packets = new IdentityHashMap<>();
    private Phase phase = Phase.IDLE;

    public PacketEntityBatch(int packetLimit, @NotNull BiConsumer<Viewer, List<Packet>> bundleSender, @NotNull Function<Packet, @Nullable Iterable<? extends Packet>> subPackets) {
        if (packetLimit <= 0)
            throw new IllegalArgumentException("Packet limit must be positive");
        this.packetLimit = packetLimit;
        this.bundleSender = bundleSender;
        this.subPackets = subPackets;
    }

    public void begin(@NotNull Runnable callback) {
        if (phase != Phase.IDLE)
            throw new IllegalStateException("Packet entity buffer already active");
        phase = Phase.BUFFERING;
        boolean completed = false;
        try {
            callback.run();
            completed = true;
        } finally {
            if (!completed)
                reset();
        }
    }

    public void end(@NotNull Runnable callback) {
        if (phase != Phase.BUFFERING)
            throw new IllegalStateException("Packet entity buffer is not buffering");
        collect(callback);
    }

    public void close(@NotNull Runnable callback) {
        if (phase == Phase.COLLECTING)
            throw new IllegalStateException("Packet entity batch already collecting");
        collect(callback);
    }

    public void send(@NotNull Viewer viewer, @NotNull Packet packet) {
        addFlattened(queue(viewer), packet);
    }

    public void sendLeaves(@NotNull Viewer viewer, @NotNull Iterable<? extends Packet> values) {
        ArrayList<Packet> queue = queue(viewer);
        for (Packet packet : values)
            queue.add(packet);
    }

    public @NotNull ArrayList<Packet> flatten(@NotNull Iterable<? extends Packet> values) {
        ArrayList<Packet> flattened = new ArrayList<>();
        for (Packet packet : values)
            addFlattened(flattened, packet);
        return flattened;
    }

    public void forEachLeaf(@NotNull Packet packet, @NotNull Consumer<? super Packet> action) {
        Iterable<? extends Packet> values = subPackets.apply(packet);
        if (values == null) {
            action.accept(packet);
            return;
        }
        for (Packet value : values)
            forEachLeaf(value, action);
    }

    private @NotNull ArrayList<Packet> queue(@NotNull Viewer viewer) {
        if (phase == Phase.IDLE)
            throw new IllegalStateException("Packet entity batch is not active");
        return packets.computeIfAbsent(viewer, ignored -> new ArrayList<>());
    }

    private void addFlattened(@NotNull List<Packet> result, @NotNull Packet packet) {
        Iterable<? extends Packet> values = subPackets.apply(packet);
        if (values == null) {
            result.add(packet);
            return;
        }
        for (Packet value : values)
            addFlattened(result, value);
    }

    private void collect(@NotNull Runnable callback) {
        phase = Phase.COLLECTING;
        try {
            callback.run();
            finish();
        } finally {
            reset();
        }
    }

    private void finish() {
        while (!packets.isEmpty()) {
            Iterator<Map.Entry<Viewer, ArrayList<Packet>>> iterator = packets.entrySet().iterator();
            Map.Entry<Viewer, ArrayList<Packet>> entry = iterator.next();
            Viewer viewer = entry.getKey();
            ArrayList<Packet> values = entry.getValue();
            iterator.remove();
            sendBundles(viewer, values);
        }
    }

    private void sendBundles(@NotNull Viewer viewer, @NotNull List<Packet> values) {
        for (int from = 0; from < values.size(); from += packetLimit) {
            int to = Math.min(from + packetLimit, values.size());
            bundleSender.accept(viewer, from == 0 && to == values.size() ? values : values.subList(from, to));
        }
    }

    private void reset() {
        packets.clear();
        phase = Phase.IDLE;
    }
}
