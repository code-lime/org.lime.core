package org.lime.core.common.services.cooldowns;

record CooldownTime(
        long beginTime,
        long endTime) {
    public boolean isEnd(long now) {
        return endTime < now;
    }

    public long left(long now) {
        return Math.max(endTime - now, 0);
    }

    public double percent(long now) {
        long endDelta = endTime - beginTime;
        long nowDelta = now - beginTime;
        return nowDelta <= 0 ? 0
                : nowDelta >= endDelta ? 1
                  : nowDelta / (double) endDelta;
    }

    public static CooldownTime of(long ms) {
        return of(System.currentTimeMillis(), ms);
    }

    public static CooldownTime of(long now, long ms) {
        return new CooldownTime(now, now + Math.max(0, ms));
    }
}
