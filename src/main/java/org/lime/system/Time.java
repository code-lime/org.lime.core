package org.lime.system;

import org.apache.commons.lang.StringUtils;
import org.lime.system.execute.Func1;

import java.util.Calendar;
import java.util.TimeZone;

public class Time {
    public static Calendar moscowNow() { return Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")); }
    public static Calendar moscowTime(long totalMs) {
        Calendar calendar = moscowNow();
        calendar.setTimeInMillis(totalMs);
        return calendar;
    }
    public static Calendar zeroTime() { return moscowTime(0); }

    public static int formattedTime(String time) {
        int total = 0;
        int value = 0;
        for (char ch : time.toCharArray()) {
            if ('0' <= ch && ch <= '9') {
                value = value * 10 + ch - '0';
                continue;
            }
            switch (ch) {
                case 's' -> {
                    total += value;
                    value = 0;
                }
                case 'm' -> {
                    total += value * 60;
                    value = 0;
                }
                case 'h' -> {
                    total += value * 60 * 60;
                    value = 0;
                }
                case 'd' -> {
                    total += value * 60 * 60 * 24;
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
        String[] args = text.split("\\.");
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(args[0]));
        calendar.set(Calendar.MONTH, Integer.parseInt(args[1]) - 1);
        calendar.set(Calendar.YEAR, Integer.parseInt(args[2]));
    }
    public static void applyTime(Calendar calendar, String text) {
        String[] args = text.split(":");
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(args[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(args[1]));
        calendar.set(Calendar.SECOND, Integer.parseInt(args[2]));
    }
    public static Calendar parseCalendar(String text) {
        String[] args = text.split(" ");
        Calendar calendar = moscowNow();
        if (args.length == 1) {
            applyTime(calendar, "00:00:00");
            applyDate(calendar, args[0]);
        } else {
            applyTime(calendar, args[0]);
            applyDate(calendar, args[1]);
        }
        return calendar;
    }
    public static String formatCalendar(Calendar calendar, boolean withTime) {
        if (calendar == null) return "";
        StringBuilder builder = new StringBuilder();
        if (withTime) {
            builder
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)), 2, '0'))
                    .append(':')
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MINUTE)), 2, '0'))
                    .append(':')
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.SECOND)),  2, '0'))
                    .append(' ');
        }
        return builder
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), 2, '0'))
                .append('.')
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MONTH) + 1), 2, '0'))
                .append('.')
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.YEAR)), 4, '0')).toString();
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

    public enum Format {
        DAY_TIME("%d %02d:%02d:%02d", total -> new Object[] { total / (60*60*24), (total % (60*60*24)) / (60*60), (total % (60*60)) / 60, total % 60 }),
        HOUR_TIME("%02d:%02d:%02d", total -> new Object[] { total / (60*60), (total % (60*60)) / 60, total % 60 }),
        MINUTE_TIME("%02d:%02d", total -> new Object[] { total / 60, total % 60 }),
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
