package org.lime.core.common.services.buffers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/** Per-viewer metadata overlays shared by the Paper and Fabric drivers. */
public final class PacketEntityMetadataState<Viewer, Data, Entry, Editor extends BasePacketEntityDataEditor<Data, Entry>, Packet> {
    private final PacketEntityBufferState.EntitySource<Viewer, Entry, Editor, Packet> source;
    private final Codec<Data, Entry, Editor, Packet> codec;
    private final Map<Viewer, Editor> states = new IdentityHashMap<>();

    public PacketEntityMetadataState(@NotNull PacketEntityBufferState.EntitySource<Viewer, Entry, Editor, Packet> source, @NotNull Codec<Data, Entry, Editor, Packet> codec) {
        this.source = source;
        this.codec = codec;
    }

    /** Replaces metadata packets in pairing data with one viewer-specific packet. */
    public void pairing(@NotNull Viewer viewer, @NotNull List<Packet> packets) {
        if (!source.hasViewListeners()) {
            states.remove(viewer);
            return;
        }

        Editor overlay = recompute(viewer);
        int metadataIndex = -1;
        ArrayList<Entry> canonical = new ArrayList<>();
        for (int index = 0; index < packets.size(); index++) {
            List<? extends Entry> entries = codec.decode().apply(packets.get(index));
            if (entries == null)
                continue;
            if (metadataIndex < 0)
                metadataIndex = index;
            canonical.addAll(entries);
            packets.remove(index--);
        }

        LinkedHashMap<Integer, Entry> merged = index(canonical);
        overlay.overrides.forEach((access, entry) -> merged.put(access.id(), entry));
        if (!merged.isEmpty()) {
            if (metadataIndex < 0)
                metadataIndex = Math.min(1, packets.size());
            packets.add(Math.min(metadataIndex, packets.size()), codec.encode().apply(new ArrayList<>(merged.values())));
        }
        store(viewer, overlay);
    }

    public @NotNull Packet personalize(@NotNull Viewer viewer, @NotNull Packet packet) {
        List<? extends Entry> incoming = codec.decode().apply(packet);
        if (incoming == null)
            return packet;

        Editor previous = states.get(viewer);
        boolean recompute = source.hasViewListeners() && incoming.stream().anyMatch(source::isTriggeredUpdate);
        Editor current;
        if (recompute) {
            current = recompute(viewer);
        } else {
            if (previous == null)
                return packet;
            current = previous;
        }

        LinkedHashMap<Integer, Entry> result = index(incoming);
        boolean personalized = false;
        for (var override : current.overrides.entrySet()) {
            int id = override.getKey().id();
            if (result.containsKey(id)) {
                result.put(id, override.getValue());
                personalized = true;
            }
        }
        if (recompute) {
            if (previous != null) {
                for (var override : previous.overrides.entrySet()) {
                    var access = override.getKey();
                    if (!current.overrides.containsKey(access)) {
                        result.put(access.id(), access.reset(previous.data));
                        personalized = true;
                    }
                }
            }
            for (var override : current.overrides.entrySet()) {
                Entry old = previous == null ? null : previous.overrides.get(override.getKey());
                if (old == null || !old.equals(override.getValue())) {
                    result.put(override.getKey().id(), override.getValue());
                    personalized = true;
                }
            }
        }

        Packet outgoing = personalized ? codec.encode().apply(new ArrayList<>(result.values())) : packet;
        if (recompute)
            store(viewer, current);
        return outgoing;
    }

    public void forget(@NotNull Viewer viewer) {
        states.remove(viewer);
    }

    private @NotNull Editor recompute(@NotNull Viewer viewer) {
        Editor editor = codec.editorFactory().get();
        source.edit(viewer, editor);
        return editor;
    }

    private @NotNull LinkedHashMap<Integer, Entry> index(@NotNull Iterable<? extends Entry> entries) {
        LinkedHashMap<Integer, Entry> result = new LinkedHashMap<>();
        entries.forEach(entry -> result.put(codec.id().applyAsInt(entry), entry));
        return result;
    }

    private void store(@NotNull Viewer viewer, @NotNull Editor editor) {
        if (editor.overrides.isEmpty())
            states.remove(viewer);
        else
            states.put(viewer, editor);
    }

    /** Platform hooks for metadata overlays. */
    public record Codec<Data, Entry, Editor extends BasePacketEntityDataEditor<Data, Entry>, Packet>(@NotNull Supplier<? extends Editor> editorFactory, @NotNull ToIntFunction<? super Entry> id, @NotNull Function<? super List<Entry>, ? extends Packet> encode, @NotNull Function<? super Packet, ? extends @Nullable List<? extends Entry>> decode) {}
}
