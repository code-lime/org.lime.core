package org.lime.core.common.services.memories;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.Service;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action2;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.execute.Func2;

import java.time.Duration;
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
            ConcurrentHashMap<MemoryKey<?>, Object> memory,
            ConcurrentHashMap<MemoryKey<?>, ConcurrentHashMap<UUID, MemoryListenUpdating<?>>> listenUpdating)
            implements Disposable {
        public <T> Optional<T> get(MemoryKey<T> key) {
            return Optional.ofNullable(memory.get(key))
                    .map(key::castOrNull);
        }
        public <T> Optional<T> getOrCreate(MemoryKey<T> key, Supplier<T> supplier) {
            return key.cast(memory.computeIfAbsent(key, v -> {
                var value = supplier.get();
                handleListen(key, value);
                return value;
            }));
        }
        public <T> void set(MemoryKey<T> key, @Nullable T value) {
            if (value == null) memory.remove(key);
            else memory.put(key, value);
            handleListen(key, value);
        }
        public <T> boolean has(MemoryKey<T> key) {
            return memory.containsKey(key);
        }
        public <T> boolean remove(MemoryKey<T> key) {
            if (memory.remove(key) == null)
                return false;
            handleListen(key, null);
            return true;
        }

        public <T> void modify(MemoryKey<T> key, Func1<@Nullable T, @Nullable T> modify) {
            memory.compute(key, (k,v) -> {
                var newValue = modify.invoke(key.castOrNull(v));
                if (v != newValue)
                    handleListen(key, newValue);
                return newValue;
            });
        }

        private void handleListen(MemoryKey<?> key, @Nullable Object value) {
            var listeners = listenUpdating.get(key);
            if (listeners == null || listeners.isEmpty())
                return;
            for (var listener : listeners.values())
                listener.tryHandle(uid, value);
        }

        @Override
        public void close() {
            memory.entrySet().removeIf(v -> {
                handleListen(v.getKey(), null);
                return true;
            });
        }
    }

    @Inject ScheduleTaskService taskService;

    private final ConcurrentHashMap<UUID, Memory> memories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MemoryKey<?>, ConcurrentHashMap<UUID, MemoryListenUpdating<?>>> listenUpdating = new ConcurrentHashMap<>();

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

    public <T> MemoryStorage<T> createStorage(MemoryKey<T> key) {
        return new MemoryStorage<>() {
            @Override
            public MemoryKey<T> key() {
                return key;
            }
            @Override
            public Optional<T> get(UUID playerId) {
                return BaseConnectionStorageService.this.get(key, playerId);
            }
            @Override
            public Optional<T> getOrCreate(UUID playerId, Supplier<T> supplier) {
                return BaseConnectionStorageService.this.getOrCreate(key, playerId, supplier);
            }
            @Override
            public void set(UUID playerId, @Nullable T value) {
                BaseConnectionStorageService.this.set(key, playerId, value);
            }
            @Override
            public boolean has(UUID playerId) {
                return BaseConnectionStorageService.this.has(key, playerId);
            }
            @Override
            public boolean remove(UUID playerId) {
                return BaseConnectionStorageService.this.remove(key, playerId);
            }
            @Override
            public void modify(UUID playerId, Func1<@Nullable T, @Nullable T> modify) {
                BaseConnectionStorageService.this.modify(key, playerId, modify);
            }
            @Override
            public void modifyEvery(Func2<UUID, @Nullable T, @Nullable T> modify) {
                BaseConnectionStorageService.this.modifyEvery(key, modify);
            }
            @Override
            public void every(Action2<UUID, T> action) {
                BaseConnectionStorageService.this.every(key, action);
            }
            @Override
            public Disposable listenUpdating(Action2<UUID, @Nullable T> callback) {
                return BaseConnectionStorageService.this.listenUpdating(key, callback);
            }
        };
    }
    public <T> MemoryStorage<T> createStorage(
            TypeLiteral<T> type,
            InjectMemoryConnection key) {
        return createStorage(type, key.key());
    }
    public <T> MemoryStorage<T> createStorage(
            TypeLiteral<T> type,
            String key) {
        if (InjectMemoryConnection.UNIQUE_KEY.equals(key))
            key = "unique#" + UUID.randomUUID();
        return createStorage(new MemoryKey<>(key, type));
    }

    public <T> Optional<T> get(MemoryKey<T> key, UUID playerId) {
        return Optional.ofNullable(memories.get(playerId)).flatMap(v -> v.get(key));
    }
    private <T> Optional<T> getOrCreate(MemoryKey<T> key, UUID playerId, Supplier<T> supplier) {
        return Optional.ofNullable(memories.get(playerId)).flatMap(v -> v.getOrCreate(key, supplier));
    }
    public <T> void set(MemoryKey<T> key, UUID playerId, @Nullable T value) {
        Optional.ofNullable(memories.get(playerId)).ifPresent(v -> v.set(key, value));
    }
    public <T> boolean has(MemoryKey<T> key, UUID playerId) {
        return Optional.ofNullable(memories.get(playerId)).map(v -> v.has(key)).orElse(false);
    }
    public <T> boolean remove(MemoryKey<T> key, UUID playerId) {
        return Optional.ofNullable(memories.get(playerId)).map(v -> v.remove(key)).orElse(false);
    }
    public <T> void modify(MemoryKey<T> key, UUID playerId, Func1<@Nullable T, @Nullable T> modify) {
        Optional.ofNullable(memories.get(playerId)).ifPresent(v -> v.modify(key, modify));
    }
    private <T> void modifyEvery(MemoryKey<T> key, Func2<UUID, @Nullable T, @Nullable T> modify) {
        memories.forEach((playerId, memory) -> memory.modify(key, v -> modify.invoke(playerId, v)));
    }
    private <T> void every(MemoryKey<T> key, Action2<UUID, T> action) {
        memories.forEach((playerId, memory) -> memory.get(key).ifPresent(v -> action.invoke(playerId, v)));
    }
    private <T> Disposable listenUpdating(MemoryKey<T> key, Action2<UUID, @Nullable T> callback) {
        UUID uid = UUID.randomUUID();
        var listeners = listenUpdating.computeIfAbsent(key, v -> new ConcurrentHashMap<>());
        listeners.put(uid, new MemoryListenUpdating<>(key, callback));
        return () -> listeners.remove(uid);
    }
}
