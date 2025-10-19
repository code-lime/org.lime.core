package org.lime.core.common.utils;

import net.kyori.adventure.util.Ticks;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DurationUtils {
    public static final TemporalUnit TICKS_UNIT = new TemporalUnit() {
        private final long milliseconds = Ticks.SINGLE_TICK_DURATION_MS;
        private final Duration duration = Duration.ofMillis(this.milliseconds);
        @Override
        public Duration getDuration() {
            return duration;
        }
        @Override
        public boolean isDurationEstimated() {
            return false;
        }
        @Override
        public boolean isDateBased() {
            return false;
        }
        @Override
        public boolean isTimeBased() {
            return true;
        }
        @SuppressWarnings("unchecked")
        @Override
        public <R extends Temporal> R addTo(final R temporal, final long amount) {
            return (R) temporal.plus(this.getDuration().multipliedBy(amount));
        }
        @Override
        public long between(final Temporal start, final Temporal end) {
            return start.until(end, ChronoUnit.MILLIS) / this.milliseconds;
        }

        @Override
        public String toString() {
            return "Ticks";
        }
    };
    public record UnitSymbol(char symbol, TemporalUnit unit) {
        public static UnitSymbol of(char symbol, TemporalUnit unit) {
            return new UnitSymbol(symbol, unit);
        }
    }
    public static LinkedHashMap<Character, UnitSymbol> UNITS = Stream.of(
            UnitSymbol.of('w', ChronoUnit.WEEKS),
            UnitSymbol.of('d', ChronoUnit.DAYS),
            UnitSymbol.of('h', ChronoUnit.HOURS),
            UnitSymbol.of('m', ChronoUnit.MINUTES),
            UnitSymbol.of('s', ChronoUnit.SECONDS),
            UnitSymbol.of('t', TICKS_UNIT)
    ).collect(Collectors.toMap(UnitSymbol::symbol, v -> v, (x, y) -> y, LinkedHashMap::new));

    public static final int MILLIS_PER_TICK = 50;

    private static void addPart(StringBuilder builder, long value, String suffix) {
        if (value <= 0)
            return;
        builder.append(value).append(suffix);
    }
    public static String write(Duration value) {
        if (value.isNegative())
            return "0s";

        Duration remaining = value;

        StringBuilder builder = new StringBuilder();

        for (UnitSymbol us : UNITS.values()) {
            char symbol = us.symbol();
            TemporalUnit unit = us.unit();

            Duration unitDuration = unit.getDuration();
            if (unitDuration.isZero() || unitDuration.isNegative())
                continue;

            long amount = remaining.dividedBy(unitDuration);

            if (amount > 0) {
                addPart(builder, amount, String.valueOf(symbol));
                remaining = remaining.minus(unitDuration.multipliedBy(amount));
            }
        }

        if (builder.isEmpty())
            builder.append("0s");

        return builder.toString();
    }
    public static Duration read(String value) {
        long amount = 0;
        Duration duration = Duration.ZERO;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isDigit(ch)) {
                amount = amount * 10 + (ch - '0');
                continue;
            }
            if (amount <= 0)
                continue;
            UnitSymbol us = UNITS.get(ch);
            if (us == null)
                throw new IllegalStateException("Unexpected character: " + ch);
            var unit = us.unit();
            if (unit.isDurationEstimated())
                duration = duration.plus(unit.getDuration().multipliedBy(amount));
            else
                duration = duration.plus(amount, unit);
            amount = 0;
        }

        if (amount > 0)
            duration = duration.plusSeconds(amount);

        return duration;
    }
    public static Stream<String> readVariants(String value) {
        if (!value.isEmpty()) {
            char ch = value.charAt(value.length() - 1);
            if (Character.isDigit(ch))
                return UNITS.keySet().stream().map(v -> value + v);
        }
        return UNITS.keySet().stream().map(v -> value + "1" + v);
    }

    public static Duration ofTicks(long ticks) {
        return Duration.ofMillis(ticks * MILLIS_PER_TICK);
    }
    public static long toTicks(Duration duration) {
        return duration.toMillis() / MILLIS_PER_TICK;
    }
}
