package org.lime.core.common.services.cooldowns;

import com.google.common.util.concurrent.AtomicDouble;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

record CooldownData<Key>(
        CooldownKey<Key> key,
        ConcurrentHashMap<Key, CooldownTime> cooldowns) {
    public void setCooldown(Key key, long ms) {
        cooldowns.put(key, CooldownTime.of(ms));
    }
    public long getCooldown(Key key) {
        AtomicLong wait = new AtomicLong(0);
        cooldowns.computeIfPresent(key, (k, next) -> {
            long now = System.currentTimeMillis();
            long left = next.left(now);
            if (left <= 0)
                return null;
            wait.set(left);
            return next;
        });
        return wait.get();
    }
    public double percentCooldown(Key key) {
        AtomicDouble percent = new AtomicDouble(0);
        cooldowns.computeIfPresent(key, (k, next) -> {
            long now = System.currentTimeMillis();
            if (next.isEnd(now))
                return null;
            percent.set(next.percent(now));
            return next;
        });
        return percent.get();
    }
    public boolean useCooldown(Key key, long ms) {
        AtomicBoolean used = new AtomicBoolean(false);
        cooldowns.compute(key, (kk, v) -> {
            long now = System.currentTimeMillis();
            if (v == null || v.isEnd(now)) {
                used.set(true);
                return CooldownTime.of(now, ms);
            }
            return v;
        });
        return used.get();
    }
    public boolean hasCooldown(Key key) {
        AtomicBoolean has = new AtomicBoolean(false);
        cooldowns.computeIfPresent(key, (k, next) -> {
            long now = System.currentTimeMillis();
            if (next.isEnd(now))
                return null;
            has.set(true);
            return next;
        });
        return has.get();
    }

    public void update(long now) {
        cooldowns.values().removeIf(v -> v.isEnd(now));
    }
    public void reset() {
        cooldowns.clear();
    }

    public static <Key>CooldownData<Key> create(CooldownKey<Key> key) {
        return new CooldownData<>(key, new ConcurrentHashMap<>());
    }
}
