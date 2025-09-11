package org.lime.core.common.utils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public class DurationUtils {
    private static final int MILLIS_PER_TICK = 50;

    private static void addPart(StringBuilder builder, long value, String suffix) {
        if (value <= 0)
            return;
        builder.append(value).append(suffix);
    }
    public static String write(Duration value) {
        if (value.isNegative())
            return "0s";

        StringBuilder builder = new StringBuilder();

        addPart(builder, value.toMillisPart() / MILLIS_PER_TICK, "t");
        addPart(builder, value.toSecondsPart(), "s");
        addPart(builder, value.toMinutesPart(), "m");
        addPart(builder, value.toHoursPart(), "h");
        addPart(builder, value.toDays(), "d");

        if (builder.isEmpty())
            builder.append("0s");

        return builder.toString();
    }
    public static Duration read(String value) {
        long amount = 0;
        Duration duration = Duration.ZERO;

        for (char ch : value.toCharArray()) {
            if ('0' <= ch && ch <= '9') {
                amount = amount * 10 + (ch - '0');
            } else {
                if (ch == 't') {
                    duration = duration.plus(amount * MILLIS_PER_TICK, ChronoUnit.MILLIS);
                } else {
                    TemporalUnit unit = switch (ch) {
                        case 's' -> ChronoUnit.SECONDS;
                        case 'm' -> ChronoUnit.MINUTES;
                        case 'h' -> ChronoUnit.HOURS;
                        case 'd' -> ChronoUnit.DAYS;
                        default -> throw new IllegalStateException("Unexpected character: " + ch);
                    };
                    duration = duration.plus(amount, unit);
                }
                amount = 0;
            }
        }

        if (amount > 0)
            duration = duration.plusSeconds(amount);

        return duration;
    }
}
