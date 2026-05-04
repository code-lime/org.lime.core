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
            ConcurrentHashMap<MemoryKey<?>, Object> memory) {
        public <T> Optional<T> get(MemoryKey<T> key) {
            return Optional.ofNullable(memory.get(key))
                    .map(key::castOrNull);
        }
        public <T> Optional<T> getOrCreate(MemoryKey<T> key, Supplier<T> supplier) {
            return key.cast(memory.computeIfAbsent(key, v -> supplier.get()));
        }
        public <T> void set(MemoryKey<T> key, @Nullable T value) {
            if (value == null) memory.remove(key);
            else memory.put(key, value);
        }
        public <T> boolean has(MemoryKey<T> key) {
            return memory.containsKey(key);
        }
        public <T> boolean remove(MemoryKey<T> key) {
            return memory.remove(key) != null;
        }

        public <T> void modify(MemoryKey<T> key, Func1<@Nullable T, @Nullable T> modify) {
            memory.compute(key, (k,v) -> modify.invoke(key.castOrNull(v)));
        }
    }

    @Inject ScheduleTaskService taskService;

    private final ConcurrentHashMap<UUID, Memory> memories = new ConcurrentHashMap<>();

    protected abstract Stream<UUID> onlinePlayerIds();

    @Override
    public Disposable register() {
        memories.clear();
        onlinePlayerIds().forEach(this::handleLogin);
        return Disposable.combine(
                taskService.builder()
                        .withCallback(this::syncUpdate)
                        .withLoop(Duration.ofSeconds(2))
                        .execute(),
                memories::clear
        );
    }
    protected void syncUpdate() {
        var playerIds = onlinePlayerIds().collect(Collectors.toSet());
        memories.keySet().removeIf(v -> !playerIds.remove(v));
        playerIds.forEach(this::handleLogin);
    }

    protected void handleLogin(UUID playerId) {
        memories.put(playerId, new Memory(new ConcurrentHashMap<>()));
    }
    protected void handleLogout(UUID playerId) {
        memories.remove(playerId);
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
            public void modifyEvery(Func2<UUID, T, T> modify) {
                BaseConnectionStorageService.this.modifyEvery(key, modify);
            }
            @Override
            public void every(Action2<UUID, T> action) {
                BaseConnectionStorageService.this.every(key, action);
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
    private <T> void modifyEvery(MemoryKey<T> key, Func2<UUID, T, T> modify) {
        memories.forEach((playerId, memory) -> memory.modify(key, v -> modify.invoke(playerId, v)));
    }
    private <T> void every(MemoryKey<T> key, Action2<UUID, T> action) {
        memories.forEach((playerId, memory) -> memory.get(key).ifPresent(v -> action.invoke(playerId, v)));
    }
}
