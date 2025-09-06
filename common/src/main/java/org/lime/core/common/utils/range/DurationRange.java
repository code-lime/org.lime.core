package org.lime.core.common.utils.range;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class DurationRange
        extends BaseRange<DurationRange, Duration> {
    public static final Factory<DurationRange, Duration> FACTORY = createFactory(DurationRange.class, DurationRange::new);

    public DurationRange(@NotNull Duration a, @NotNull Duration b) {
        super(a, b, FACTORY);
    }

    public static DurationRange of(Duration a, Duration b) {
        return new DurationRange(a, b);
    }
}
