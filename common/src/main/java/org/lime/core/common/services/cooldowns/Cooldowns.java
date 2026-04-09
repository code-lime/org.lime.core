package org.lime.core.common.services.cooldowns;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import org.lime.core.common.api.BindService;
import org.lime.core.common.api.Service;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.utils.Disposable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@BindService
public class Cooldowns
        implements Service {
    @Inject ScheduleTaskService taskService;

    private final ConcurrentHashMap<CooldownKey<?>, CooldownData<?>> cooldowns = new ConcurrentHashMap<>();

    @Override
    public Disposable register() {
        cooldowns.values().forEach(CooldownData::reset);
        return taskService.builder()
                .withLoopTicks(10)
                .withCallback(this::update)
                .execute();
    }
    @Override
    public void unregister() {
        cooldowns.values().forEach(CooldownData::reset);
    }

    private void update() {
        if (cooldowns.isEmpty())
            return;

        long now = System.currentTimeMillis();
        cooldowns.values().forEach(v -> v.update(now));
    }

    //CooldownAt
    public <Key>Cooldown<Key> group(TypeLiteral<Key> keyType, InjectCooldown annotation) {
        return group(keyType, annotation.group(), annotation.defaultCooldownMillis());
    }
    public <Key>Cooldown<Key> group(TypeLiteral<Key> keyType, String group, long defaultCooldownMillis) {
        if (InjectCooldown.UNIQUE_GROUP.equals(group))
            group = "unique#" + UUID.randomUUID();
        return new Cooldown<>(this, keyType, group, defaultCooldownMillis);
    }
    public <Key>Cooldown<Key> uniqueGroup(TypeLiteral<Key> keyType, long defaultCooldownMillis) {
        return group(keyType, InjectCooldown.UNIQUE_GROUP, defaultCooldownMillis);
    }

    <Key>CooldownData<Key> cooldownData(String group, TypeLiteral<Key> keyType) {
        var result = cooldowns.computeIfAbsent(new CooldownKey<>(group, keyType), CooldownData::create);
        //noinspection unchecked
        return (CooldownData<Key>)result;
    }

    public <Key>void setCooldown(String group, TypeLiteral<Key> keyType, Key key, long ms) {
        cooldownData(group, keyType).setCooldown(key, ms);
    }
    public <Key>boolean useCooldown(String group, TypeLiteral<Key> keyType, Key key, long ms) {
        return cooldownData(group, keyType).useCooldown(key, ms);
    }
    public <Key>long getCooldown(String group, TypeLiteral<Key> keyType, Key key) {
        return cooldownData(group, keyType).getCooldown(key);
    }
    public <Key> boolean hasCooldown(String group, TypeLiteral<Key> keyType, Key key) {
        return cooldownData(group, keyType).hasCooldown(key);
    }
}
