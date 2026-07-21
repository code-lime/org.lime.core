package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;
import org.lime.core.common.utils.Disposable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

/** Audience, packet routing and synchronization state of one packet entity. */
public final class PacketEntityTracker<Player, Packet> implements Disposable {
    private record Transition<Player>(@NotNull Player player, boolean added) {}

    private final Driver<Player, Packet> driver;
    private final PacketEntityBatch<Player, Packet> batch;
    private final PacketEntityBufferState.EntitySource<Player, ?, ?, Packet> source;
    private final Set<Player> viewers = identitySet();
    private final Set<Player> desired = identitySet();
    private final ArrayList<Transition<Player>> transitions = new ArrayList<>();
    private boolean closed;

    public PacketEntityTracker(@NotNull Driver<Player, Packet> driver, @NotNull PacketEntityBatch<Player, Packet> batch, @NotNull PacketEntityBufferState.EntitySource<Player, ?, ?, Packet> source) {
        this.driver = driver;
        this.batch = batch;
        this.source = source;
    }

    /** Reconciles removals, advances NMS state, pairs every new viewer, then dispatches transitions. */
    public void synchronize(@NotNull Iterable<? extends Player> trackedPlayers, int trackingRange) {
        if (closed)
            return;

        try {
            if (trackingRange > 0) {
                for (Player player : trackedPlayers) {
                    if (driver.canTrack(player, trackingRange))
                        desired.add(player);
                }
            }

            Iterator<Player> viewers = this.viewers.iterator();
            while (viewers.hasNext()) {
                Player player = viewers.next();
                if (desired.remove(player))
                    continue;
                driver.removePairing(player);
                viewers.remove();
                transitions.add(new Transition<>(player, false));
            }

            if (!this.viewers.isEmpty() || !desired.isEmpty()) {
                driver.collectStatePackets();
                for (Player player : desired) {
                    driver.addPairing(player);
                    this.viewers.add(player);
                    transitions.add(new Transition<>(player, true));
                }
            }
            dispatchTracking();
        } finally {
            desired.clear();
            transitions.clear();
        }
    }

    public void broadcast(@NotNull Packet packet) {
        broadcast(packet, player -> true);
    }

    public void broadcast(@NotNull Packet packet, @NotNull Predicate<? super Player> filter) {
        for (Player player : viewers) {
            if (!filter.test(player))
                continue;
            batch.forEachLeaf(packet, value -> batch.send(player, driver.personalize(player, value)));
        }
    }

    /** Removes all current viewers before a platform level unload or rebind. */
    public void clearAudience() {
        transitions.clear();
        try {
            Iterator<Player> viewers = this.viewers.iterator();
            while (viewers.hasNext()) {
                Player player = viewers.next();
                driver.removePairing(player);
                viewers.remove();
                transitions.add(new Transition<>(player, false));
            }
            dispatchTracking();
        } finally {
            transitions.clear();
        }
    }

    @Override
    public void close() {
        if (closed)
            return;
        closed = true;
        try {
            clearAudience();
        } finally {
            driver.onClose();
        }
    }

    private void dispatchTracking() {
        for (Transition<Player> transition : transitions)
            source.tracking(transition.player(), transition.added());
    }

    private static <Value> @NotNull Set<Value> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public interface Driver<Player, Packet> {
        boolean canTrack(@NotNull Player player, int trackingRange);

        void addPairing(@NotNull Player player);

        void removePairing(@NotNull Player player);

        @NotNull Packet personalize(@NotNull Player player, @NotNull Packet packet);

        void collectStatePackets();

        void onClose();
    }
}
