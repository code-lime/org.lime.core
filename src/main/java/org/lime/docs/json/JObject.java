package org.lime.docs.json;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record JObject(
        ImmutableList<IJProperty> properties
) implements IJElement {
    public JObject(JObject old, Iterable<IJProperty> property, boolean appendFirst) {
        this(appendFirst
                ? ImmutableList.<IJProperty>builder().addAll(property).addAll(old.properties()).build()
                : ImmutableList.<IJProperty>builder().addAll(old.properties()).addAll(property).build()
        );
    }

    public static JObject of() { return new JObject(ImmutableList.of()); }
    public static JObject of(Stream<IJProperty> properties) { return new JObject(properties.collect(ImmutableList.toImmutableList())); }
    public static JObject of(Iterator<IJProperty> properties) { return new JObject(ImmutableList.copyOf(properties)); }
    public static JObject of(Iterable<IJProperty> properties) { return new JObject(ImmutableList.copyOf(properties)); }
    public static JObject of(IJProperty... properties) { return new JObject(ImmutableList.copyOf(properties)); }
    public JObject add(IJProperty... property) { return new JObject(this, List.of(property), false); }
    public JObject addFirst(IJProperty... property) { return new JObject(this, List.of(property), true); }

    @Override public Stream<String> lines() {
        int count = properties.size();
        return count == 0 ? Stream.of("{}") : Streams.concat(
                Stream.of("{"),
                IntStream.range(0, count)
                    .boxed()
                    .flatMap(i -> properties.get(i).lines(i == count - 1))
                    .map(v -> "\t" + v),
                Stream.of("}")
        );
    }
}
























