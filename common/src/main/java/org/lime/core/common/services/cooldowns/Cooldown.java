package org.lime.core.common.services.cooldowns;

import com.google.inject.TypeLiteral;

import java.time.Duration;

public final class Cooldown<Key> {
    private final CooldownData<Key> data;
    private final long defaultCooldownMillis;

    public Cooldown(
            Cooldowns owner,
            TypeLiteral<Key> keyType,
            String group,
            long defaultCooldownMillis) {
        this.data = owner.cooldownData(group, keyType);
        this.defaultCooldownMillis = defaultCooldownMillis;
    }

    public void set(Key key) {
        setMillis(key, defaultCooldownMillis);
    }
    public void setDuration(Key key, Duration cooldown) {
        setMillis(key, cooldown.toMillis());
    }
    public void setMillis(Key key, long ms) {
        data.setCooldown(key, ms);
    }

    public boolean use(Key key) {
        return useMillis(key, defaultCooldownMillis);
    }
    public boolean useDuration(Key key, Duration cooldown) {
        return useMillis(key, cooldown.toMillis());
    }
    public boolean useMillis(Key key, long ms) {
        return data.useCooldown(key, ms);
    }

    public boolean has(Key key) {
        return data.hasCooldown(key);
    }
    public double percent(Key key) {
        return data.percentCooldown(key);
    }

    public Duration getDuration(Key key) {
        return Duration.ofMillis(getMillis(key));
    }
    public long getMillis(Key key) {
        return data.getCooldown(key);
    }

    public TypeLiteral<Key> keyType() {
        return data.key().keyType();
    }
    public String group() {
        return data.key().group();
    }
    public long defaultCooldownMillis() {
        return defaultCooldownMillis;
    }

    @Override
    public String toString() {
        return "Cooldown[" +
                "key=" + data.key() + ", " +
                "defaultCooldownMillis=" + defaultCooldownMillis + ']';
    }
}
