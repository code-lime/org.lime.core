package org.lime.core.common.docs.json;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import org.lime.core.common.docs.BaseGroup;
import org.lime.core.common.system.tuple.Tuple;
import org.lime.core.common.system.tuple.Tuple2;

import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JsonEnumInfo extends BaseGroup<ImmutableMap<IJElement, Optional<IComment>>> {
    public JsonEnumInfo(String title, String index, ImmutableMap<IJElement, Optional<IComment>> element, ImmutableList<IComment> comments) {
        super(title, index, element, comments);
    }
    public JsonEnumInfo(JsonEnumInfo old, IJElement item, @Nullable IComment comment) {
        this(old.title(), old.index(), ImmutableMap.<IJElement, Optional<IComment>>builder()
                .putAll(old.element())
                .put(item, Optional.ofNullable(comment))
                .build(), old.comments());
    }

    private static <T extends Enum<T>>Tuple2<IJElement, Optional<IComment>> getElementOf(T value) {
        IJElement element = null;
        IComment comment = null;
        if (value instanceof IEnumDocs docs) {
            element = docs.docsElement();
            comment = docs.docsComment();
        }
        if (element == null) element = IJElement.raw(value.name());
        return Tuple.of(element, Optional.ofNullable(comment));
    }

    public static JsonEnumInfo of(String title, String index, ImmutableMap<IJElement, Optional<IComment>> items, IComment... comments) {
        return new JsonEnumInfo(title, index, items, ImmutableList.copyOf(comments));
    }
    public static JsonEnumInfo of(String title, String index, ImmutableList<IJElement> items, IComment... comments) {
        return new JsonEnumInfo(title, index, items.stream().collect(ImmutableMap.toImmutableMap(k -> k, k -> Optional.empty())), ImmutableList.copyOf(comments));
    }
    public static JsonEnumInfo of(String title, String index, IComment... comments) { return new JsonEnumInfo(title, index, ImmutableMap.of(), ImmutableList.copyOf(comments)); }
    public static <T extends Enum<T>>JsonEnumInfo of(String title, String index, Class<T> tEnum, IComment... comments) {
        return of(title, index, Arrays.stream(tEnum.getEnumConstants()), comments);
    }
    public static <T extends Enum<T>>JsonEnumInfo of(String title, String index, Stream<T> values, IComment... comments) {
        return of(title, index, values
                        .map(JsonEnumInfo::getElementOf)
                        .collect(ImmutableMap.toImmutableMap(k -> k.val0, v -> v.val1)),
                comments);
    }

    public static JsonEnumInfo of(String title, ImmutableMap<IJElement, Optional<IComment>> items, IComment... comments) { return of(title, title.toLowerCase(), items, comments); }
    public static JsonEnumInfo of(String title, ImmutableList<IJElement> items, IComment... comments) { return of(title, title.toLowerCase(), items, comments); }
    public static JsonEnumInfo of(String title, IComment... comments) { return of(title, title.toLowerCase(), comments); }
    public static <T extends Enum<T>>JsonEnumInfo of(String title, Class<T> tEnum, IComment... comments) { return of(title, title.toLowerCase(), tEnum, comments); }
    public static <T extends Enum<T>>JsonEnumInfo of(String title, Stream<T> values, IComment... comments) { return of(title, title.toLowerCase(), values, comments); }

    public JsonEnumInfo add(IJElement item) { return add(item, null); }
    public JsonEnumInfo add(IJElement item, @Nullable IComment comment) { return new JsonEnumInfo(this, item, comment); }

    @Override public Stream<String> elementContext() {
        return Streams.concat(
                Stream.of("<pre><code>Список возможных значений:"),
                element().entrySet().stream().flatMap(kv -> item(kv.getKey(), kv.getValue())),
                Stream.of("</code></pre>")
        );
    }

    private Stream<String> item(IJElement item, Optional<IComment> comment) {
        List<String> lines = item.lines(this).collect(Collectors.toList());
        int size = lines.size();
        return switch (size) {
            case 0 -> throw new IllegalArgumentException("LINES IS ZERO IN ENUM INFO " + title());
            case 1 -> Stream.of("<warning>- </warning>" + lines.get(0) + comment.map(v -> v.build(this)).map(v -> " " + v).orElse(""));
            default -> {
                String first = lines.remove(0);
                yield Streams.concat(
                        Stream.of("<warning>- </warning>" + first + comment.map(v -> v.build(this)).map(v -> " " + v).orElse("")),
                        lines.stream());
            }
        };
    }
}