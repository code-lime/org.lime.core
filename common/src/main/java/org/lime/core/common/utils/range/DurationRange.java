package org.lime.core.common.utils.range;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class DurationRange
        extends BaseRange<DurationRange, Duration> {
    public static final Factory<DurationRange, Duration> FACTORY = createFactory(DurationRange.class, DurationRange::new);

    public DurationRange(@NotNull Duration a, @NotNull Duration b) {
        super(a, b, FACTORY);
    }

    @Override
    public @NotNull Duration percent(double percent) {
        var min = this.min.toNanos();
        var max = this.max.toNanos();

        return Duration.ofNanos(Math.round(min + (max - min) * percent));
    }

    public static DurationRange of(Duration a, Duration b) {
        return new DurationRange(a, b);
    }
    public static DurationRange of(Duration value) {
        return of(value, value);
    }
}
