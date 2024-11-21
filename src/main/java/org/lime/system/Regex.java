package org.lime.system;

import com.google.common.collect.Streams;
import org.lime.system.execute.Execute;
import org.lime.system.execute.Func1;
import org.lime.system.tuple.Tuple;
import org.lime.system.tuple.Tuple2;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

public class Regex {
    private static final ConcurrentHashMap<String, Pattern> patterns = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Tuple2<String, String>, Iterable<MatchResult>> iterables = new ConcurrentHashMap<>();
    private static void tryClearCompare() {
        if (iterables.size() > 1000) iterables.clear();
    }

    private static final Pattern NONE_PATTERN = Pattern.compile("^#^");

    private static Pattern compileRegex(String regex) {
        try {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            return NONE_PATTERN;
        }
    }

    public static boolean compareRegex(String input, String regex) {
        Pattern pattern = patterns.getOrDefault(regex, null);
        if (pattern == null) patterns.put(regex, pattern = compileRegex(regex));
        return pattern.matcher(input).matches();
    }
    public static <T> Stream<T> filterRegex(Iterable<T> input, Func1<T, String> format, String regex) {
        return filterRegex(input, format, regex, false);
    }
    public static <T>Stream<T> filterRegex(Iterable<T> input, Func1<T, String> format, String regex, boolean part) {
        return Streams.stream(input).filter(filterRegex(format, regex, part)::invoke);
    }
    public static <T> Func1<T, Boolean> filterRegex(Func1<T, String> format, String regex) {
        return filterRegex(format, regex, false);
    }
    public static <T> Func1<T, Boolean> filterRegex(Func1<T, String> format, String regex, boolean part) {
        Pattern pattern = patterns.computeIfAbsent(regex, Regex::compileRegex);
        return Execute.func(v -> {
            Matcher matcher = pattern.matcher(format.invoke(v));
            return part ? matcher.find() : matcher.matches();
        });
    }
    public static Iterable<MatchResult> iterableRegex(String input, String regex) {
        return iterables.compute(Tuple.of(input, regex), (k, v) -> {
            if (v != null) return v;
            Pattern pattern = patterns.getOrDefault(regex, null);
            if (pattern == null) patterns.put(regex, pattern = compileRegex(regex));
            return iterableRegex(pattern, input);
        });
    }
    public static Iterable<MatchResult> iterableRegex(final Pattern p, final String input) {
        return () -> new Iterator<MatchResult>() {
            // Use a matcher internally.
            final Matcher matcher = p.matcher(input);
            // Keep a match around that supports any interleaving of hasNext/next calls.
            MatchResult pending;

            public boolean hasNext() {
                // Lazily fill pending, and avoid calling find() multiple times if the
                // clients call hasNext() repeatedly before sampling via next().
                if (pending == null && matcher.find()) pending = matcher.toMatchResult();
                return pending != null;
            }

            public MatchResult next() {
                // Fill pending if necessary (as when clients call next() without
                // checking hasNext()), throw if not possible.
                if (!hasNext()) throw new NoSuchElementException();
                // Consume pending so next call to hasNext() does a find().
                MatchResult next = pending;
                pending = null;
                return next;
            }

            /**
             * Required to satisfy the interface, but unsupported.
             */
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
