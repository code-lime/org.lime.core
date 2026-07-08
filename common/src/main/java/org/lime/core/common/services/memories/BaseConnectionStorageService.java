package org.lime.core.common.services.memories;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.Service;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Action3;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseConnectionStorageService
        implements Service {
    private record Memory(
            UUID uid,
            ConcurrentHashMap<MemoryKey<?, ?>, ConcurrentHashMap<Object, Object>> memory,
            ConcurrentHashMap<MemoryKey<?, ?>, ConcurrentHashMap<UUID, MemoryListenUpdating<?, ?>>> listenUpdating)
            implements Disposable {
        public <Index, T> Optional<T> get(MemoryKey<Index, T> key, Index index) {
            Objects.requireNonNull(index, "Memory index");
            var values = memory.get(key);
            if (values == null)
                return Optional.empty();
            return Optional.ofNullable(values.get(index))
                    .map(key::castOrNull);
        }
        public <Index, T> Stream<Map.Entry<Index, T>> streamIndexed(MemoryKey<Index, T> key) {
            var values = memory.get(key);
            if (values == null)
                return Stream.empty();
            return values.entrySet().stream()
                    .map(v -> {
                        //noinspection unchecked
                        Index index = (Index)v.getKey();
                        //noinspection unchecked
                        T value = (T)v.getValue();
                        return new AbstractMap.SimpleImmutableEntry<>(index, value);
                    });
        }
        public <Index, T> Optional<T> getOrCreate(MemoryKey<Index, T> key, Index index, Supplier<T> supplier) {
            Objects.requireNonNull(index, "Memory index");
            var values = memory.computeIfAbsent(key, v -> new ConcurrentHashMap<>());
            var value = values.computeIfAbsent(index, v -> {
                var newValue = supplier.get();
                handleListen(key, index, newValue);
                return newValue;
            });
            if (value == null && values.isEmpty())
                memory.remove(key, values);
            return key.cast(value);
        }
        public <Index, T> void set(MemoryKey<Index, T> key, Index index, @Nullable T value) {
            Objects.requireNonNull(index, "Memory index");
            if (value == null) {
                removeValue(key, index);
            } else {
                memory.computeIfAbsent(key, v -> new ConcurrentHashMap<>())
                        .put(index, value);
            }
            handleListen(key, index, value);
        }
        public <Index, T> boolean has(MemoryKey<Index, T> key, Index index) {
            Objects.requireNonNull(index, "Memory index");
            var values = memory.get(key);
            return values != null && values.containsKey(index);
        }
        public <Index, T> boolean remove(MemoryKey<Index, T> key, Index index) {
            Objects.requireNonNull(index, "Memory index");
            if (!removeValue(key, index))
                return false;
            handleListen(key, index, null);
            return true;
        }

        public <Index, T> void modify(MemoryKey<Index, T> key, Index index, Func1<@Nullable T, @Nullable T> modify) {
            Objects.requireNonNull(index, "Memory index");
            memory.compute(key, (k, values) -> {
                if (values == null) {
                    var newValue = modify.invoke(null);
                    if (newValue == null)
                        return null;
                    values = new ConcurrentHashMap<>();
                    values.put(index, newValue);
                    handleListen(key, index, newValue);
                    return values;
                }

                values.compute(index, (i, v) -> {
                    var newValue = modify.invoke(key.castOrNull(v));
                    if (v != newValue)
                        handleListen(key, index, newValue);
                    return newValue;
                });
                return values.isEmpty() ? null : values;
            });
        }

        private <Index, T> boolean removeValue(MemoryKey<Index, T> key, Index index) {
            var values = memory.get(key);
            if (values == null || values.remove(index) == null)
                return false;
            if (values.isEmpty())
                memory.remove(key, values);
            return true;
        }
        private void handleListen(MemoryKey<?, ?> key, Object index, @Nullable Object value) {
            var listeners = listenUpdating.get(key);
            if (listeners == null || listeners.isEmpty())
                return;
            for (var listener : listeners.values())
                listener.tryHandle(uid, index, value);
        }

        @Override
        public void close() {
            memory.forEach((key, values) -> values.keySet().forEach(index -> handleListen(key, index, null)));
            memory.clear();
        }
    }

    @Inject ScheduleTaskService taskService;

    private final ConcurrentHashMap<UUID, Memory> memories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MemoryKey<?, ?>, ConcurrentHashMap<UUID, MemoryListenUpdating<?, ?>>> listenUpdating = new ConcurrentHashMap<>();

    protected abstract Stream<UUID> onlinePlayerIds();

    @Override
    public Disposable register() {
        clearAll();
        onlinePlayerIds().forEach(this::handleLogin);
        return Disposable.combine(
                taskService.builder()
                        .withCallback(this::syncUpdate)
                        .withLoop(Duration.ofSeconds(2))
                        .execute(),
                this::clearAll
        );
    }
    protected void syncUpdate() {
        var playerIds = onlinePlayerIds().collect(Collectors.toSet());
        memories.entrySet().removeIf(v -> {
            if (playerIds.remove(v.getKey()))
                return false;
            v.getValue().close();
            return true;
        });
        playerIds.forEach(this::handleLogin);
    }
    private void clearAll() {
        memories.values().removeIf(v -> {
            v.close();
            return true;
        });
    }

    protected void handleLogin(UUID playerId) {
        var memory = memories.put(playerId, new Memory(playerId, new ConcurrentHashMap<>(), listenUpdating));
        if (memory != null)
            memory.close();
    }
    protected void handleLogout(UUID playerId) {
        var memory = memories.remove(playerId);
        if (memory != null)
            memory.close();
    }

    public <T> MemoryStorage<T> createStorage(TypeLiteral<T> type, InjectMemoryConnection key) {
        return createStorage(type, key.key());
    }
    public <T> MemoryStorage<T> createStorage(TypeLiteral<T> type, String key) {
        if (InjectMemoryConnection.UNIQUE_KEY.equals(key))
            key = "unique#" + UUID.randomUUID();
        return new MemoryStorage<>(this, key, type);
    }
    public <Index, T> IndexedMemoryStorage<Index, T> createIndexedStorage(
            TypeLiteral<Index> indexType,
            TypeLiteral<T> type,
            InjectMemoryConnection key) {
        return createIndexedStorage(indexType, type, key.key());
    }
    public <Index, T> IndexedMemoryStorage<Index, T> createIndexedStorage(
            TypeLiteral<Index> indexType,
            TypeLiteral<T> type,
            String key) {
        if (InjectMemoryConnection.UNIQUE_KEY.equals(key))
            key = "unique#" + UUID.randomUUID();
        return new IndexedMemoryStorage<>(this, new MemoryKey<>(key, indexType, type));
    }

    public <Index, T> Optional<T> get(MemoryKey<Index, T> key, Index index, UUID playerId) {
        return Optional.ofNullable(memories.get(playerId)).flatMap(v -> v.get(key, index));
    }
    public <Index, T> Stream<Map.Entry<Index, T>> streamIndexed(MemoryKey<Index, T> key, UUID playerId) {
        return Optional.ofNullable(memories.get(playerId))
                .map(v -> v.streamIndexed(key))
                .orElseGet(Stream::empty);
    }
    public <Index, T> Optional<T> getOrCreate(MemoryKey<Index, T> key, Index index, UUID playerId, Supplier<T> supplier) {
        return Optional.ofNullable(memories.get(playerId)).flatMap(v -> v.getOrCreate(key, index, supplier));
    }
    public <Index, T> void set(MemoryKey<Index, T> key, Index index, UUID playerId, @Nullable T value) {
        Optional.ofNullable(memories.get(playerId)).ifPresent(v -> v.set(key, index, value));
    }
    public <Index, T> boolean has(MemoryKey<Index, T> key, Index index, UUID playerId) {
        return Optional.ofNullable(memories.get(playerId)).map(v -> v.has(key, index)).orElse(false);
    }
    public <Index, T> boolean remove(MemoryKey<Index, T> key, Index index, UUID playerId) {
        return Optional.ofNullable(memories.get(playerId)).map(v -> v.remove(key, index)).orElse(false);
    }
    public <Index, T> void modify(MemoryKey<Index, T> key, Index index, UUID playerId, Func1<@Nullable T, @Nullable T> modify) {
        Optional.ofNullable(memories.get(playerId)).ifPresent(v -> v.modify(key, index, modify));
    }
    public <Index, T> void modifyEvery(MemoryKey<Index, T> key, Index index, Func2<UUID, @Nullable T, @Nullable T> modify) {
        memories.forEach((playerId, memory) -> memory.modify(key, index, v -> modify.invoke(playerId, v)));
    }
    public <Index, T> void every(MemoryKey<Index, T> key, Index index, Action2<UUID, T> action) {
        memories.forEach((playerId, memory) -> memory.get(key, index).ifPresent(v -> action.invoke(playerId, v)));
    }
    public <Index, T> Disposable listenUpdating(MemoryKey<Index, T> key, Action3<UUID, Index, @Nullable T> callback) {
        UUID uid = UUID.randomUUID();
        var listeners = listenUpdating.computeIfAbsent(key, v -> new ConcurrentHashMap<>());
        listeners.put(uid, new MemoryListenUpdating<>(key, callback));
        return () -> {
            listeners.remove(uid);
            listenUpdating.values().removeIf(ConcurrentHashMap::isEmpty);
        };
    }
}
