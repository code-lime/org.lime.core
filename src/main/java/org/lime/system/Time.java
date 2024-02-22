package org.lime.system;

import org.apache.commons.lang.StringUtils;
import org.lime.system.execute.Func1;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class Time {
    public static Calendar moscowNow() { return Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")); }
    public static Calendar moscowTime(long totalMs) {
        Calendar calendar = moscowNow();
        calendar.setTimeInMillis(totalMs);
        return calendar;
    }
    public static Calendar zeroTime() { return moscowTime(0); }

    public static int formattedTime(String time) {
        int sign = 1;
        int total = 0;
        int value = 0;
        for (char ch : time.toCharArray()) {
            if ('0' <= ch && ch <= '9') {
                value = value * 10 + ch - '0';
                continue;
            }
            switch (ch) {
                case '-' -> sign = -1;
                case 's' -> {
                    total += value * sign;
                    sign = 1;
                    value = 0;
                }
                case 'm' -> {
                    total += value * 60 * sign;
                    sign = 1;
                    value = 0;
                }
                case 'h' -> {
                    total += value * 60 * 60 * sign;
                    sign = 1;
                    value = 0;
                }
                case 'd' -> {
                    total += value * 60 * 60 * 24 * sign;
                    sign = 1;
                    value = 0;
                }
                case 'w' -> {
                    total += value * 60 * 60 * 24 * 7 * sign;
                    sign = 1;
                    value = 0;
                }
                default -> value = 0;
            }
        }
        total += value;
        return total;
    }

    public static String formatTime(int total_sec)
    {
        int sec = total_sec % 60;
        total_sec /= 60;
        int min = total_sec % 60;
        int hour = total_sec / 60;

        return String.format("%02d:%02d:%02d", hour, min, sec);
    }

    public static Integer compareCalendar(Calendar calendar1, Calendar calendar2) {
        Func1<Integer, Integer> comparer = v -> Integer.compare(calendar1.get(v), calendar2.get(v));

        int[] checks = new int[]{
                Calendar.YEAR,
                Calendar.MONTH,
                Calendar.DAY_OF_MONTH,
                Calendar.HOUR_OF_DAY,
                Calendar.MINUTE,
                Calendar.SECOND,
                Calendar.MILLISECOND
        };
        for (int check : checks) {
            int value = comparer.invoke(check);
            if (value != 0) return value;
        }
        return 0;
    }
    public static void applyDate(Calendar calendar, String text) {
        if (text.contains("-")) {
            String[] args = text.split("-");
            calendar.set(Calendar.YEAR, Integer.parseInt(args[0]));
            calendar.set(Calendar.MONTH, Integer.parseInt(args[1]) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(args[2]));
        } else {
            String[] args = text.split("\\.");
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(args[0]));
            calendar.set(Calendar.MONTH, Integer.parseInt(args[1]) - 1);
            calendar.set(Calendar.YEAR, Integer.parseInt(args[2]));
        }
    }
    public static void applyTime(Calendar calendar, String text) {
        String[] args = text.split(Pattern.quote(text.contains("-") ? "-" : ":"));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(args[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(args[1]));
        calendar.set(Calendar.SECOND, Integer.parseInt(args[2]));
    }
    public static Calendar parseCalendar(String text) {
        return parseCalendar(text, false);
    }
    public static Calendar parseCalendar(String text, boolean reverse) {
        String[] args = text.split(Pattern.quote(text.contains("_") ? "_" : " "));
        Calendar calendar = moscowNow();
        if (args.length == 1) {
            applyTime(calendar, "00:00:00");
            applyDate(calendar, args[0]);
        } else {
            applyTime(calendar, reverse ? args[1] : args[0]);
            applyDate(calendar, reverse ? args[0] : args[1]);
        }
        return calendar;
    }
    public static String formatCalendar(Calendar calendar, boolean withTime) {
        return formatCalendar(calendar, withTime, false);
    }
    public static String formatCalendar(Calendar calendar, boolean withTime, boolean reverse) {
        if (calendar == null) return "";
        StringBuilder builderTime = new StringBuilder();
        if (withTime)
            builderTime
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)), 2, '0'))
                    .append(':')
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MINUTE)), 2, '0'))
                    .append(':')
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.SECOND)),  2, '0'));

        StringBuilder builderDate = new StringBuilder()
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), 2, '0'))
                .append('.')
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MONTH) + 1), 2, '0'))
                .append('.')
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.YEAR)), 4, '0'));

        if (builderTime.isEmpty()) return builderDate.toString();
        return reverse
                ? builderDate.append(" ").append(builderTime).toString()
                : builderTime.append(" ").append(builderDate).toString();
    }
    public static String formatMiniCalendar(Calendar calendar, boolean withTime) {
        if (calendar == null) return "";
        StringBuilder builder = new StringBuilder();
        if (withTime) {
            builder
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)), 2, '0'))
                    .append(':')
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MINUTE)), 2, '0'))
                    .append(' ');
        }
        return builder
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), 2, '0'))
                .append('.')
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MONTH) + 1), 2, '0'))
                .append('.')
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.YEAR)), 4, '0').substring(2)).toString();
    }

    private static final int SECONDS_IN_MINUTE = 60;
    private static final int SECONDS_IN_HOUR = SECONDS_IN_MINUTE * 60;
    private static final int SECONDS_IN_DAY = SECONDS_IN_HOUR * 24;
    private static final int SECONDS_IN_WEEK = SECONDS_IN_DAY * 7;

    public enum Format {
        FORMATTED("%s", total -> {
            if (total == 0) return new Object[] { "0" };

            int sign = total >= 0 ? 1 : -1;
            total *= sign;

            int weeks = total / SECONDS_IN_WEEK;
            int days = (total % SECONDS_IN_WEEK) / SECONDS_IN_DAY;
            int hours = (total % SECONDS_IN_DAY) / SECONDS_IN_HOUR;
            int minutes = (total % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE;
            int seconds = total % SECONDS_IN_MINUTE;

            StringBuilder builder = new StringBuilder();

            if (sign < 0) builder.append("-");

            if (weeks != 0) builder.append(weeks).append("w");
            if (days != 0) builder.append(days).append("d");
            if (hours != 0) builder.append(hours).append("h");
            if (minutes != 0) builder.append(minutes).append("m");
            if (seconds != 0) builder.append(seconds).append("s");

            return new Object[] { builder.toString() };
        }),
        DAY_TIME("%d %02d:%02d:%02d", total -> new Object[] {
                total / SECONDS_IN_DAY,
                (total % SECONDS_IN_DAY) / SECONDS_IN_HOUR,
                (total % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE,
                total % SECONDS_IN_MINUTE
        }),
        HOUR_TIME("%02d:%02d:%02d", total -> new Object[] {
                total / SECONDS_IN_HOUR,
                (total % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE,
                total % SECONDS_IN_MINUTE
        }),
        MINUTE_TIME("%02d:%02d", total -> new Object[] {
                total / SECONDS_IN_MINUTE,
                total % SECONDS_IN_MINUTE
        }),
        SECOND_TIME("%02d", total -> new Object[] { total });

        public final String format;
        public final Func1<Integer, Object[]> convert;
        Format(String format, Func1<Integer, Object[]> convert) {
            this.format = format;
            this.convert = convert;
        }
        public String format(int totalSec) {
            return String.format(format, convert.invoke(totalSec));
        }
    }
    public static String formatTotalTime(int totalSec, Format format) {
        return format.format(totalSec);
    }
    public static String formatTotalTime(long ms, Format format) {
        return formatTotalTime((int)(ms / 1000), format);
    }
}
