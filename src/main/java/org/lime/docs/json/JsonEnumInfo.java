package org.lime.docs.json;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import org.lime.system.toast.*;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class JsonEnumInfo implements IIndexGroup {
    private final String title;
    private final String index;
    private final ImmutableMap<IJElement, Optional<IComment>> items;

    @Override public String title() { return title; }
    @Override public String index() {return index; }
    public ImmutableMap<IJElement, Optional<IComment>> items() { return items; }

    private IIndexDocs parent = null;
    @Override @Nullable public IIndexDocs parent() { return parent; }
    @Override public void setParent(@Nullable IIndexGroup parent) { this.parent = parent; }

    public JsonEnumInfo(String title, String index, ImmutableMap<IJElement, Optional<IComment>> items) {
        this.title = title;
        this.index = index;
        this.items = items;
    }
    public JsonEnumInfo(JsonEnumInfo old, IJElement item, Optional<IComment> comment) {
        this(old.title(), old.index(), ImmutableMap.<IJElement, Optional<IComment>>builder()
                .putAll(old.items())
                .put(item, comment)
                .build()
        );
    }

    public static JsonEnumInfo of(String title, String index, ImmutableMap<IJElement, Optional<IComment>> items) { return new JsonEnumInfo(title, index, items); }
    public static JsonEnumInfo of(String title, String index, ImmutableList<IJElement> items) { return new JsonEnumInfo(title, index, items.stream().collect(ImmutableMap.toImmutableMap(k -> k, k -> Optional.empty()))); }
    public static JsonEnumInfo of(String title, String index) { return new JsonEnumInfo(title, index, ImmutableMap.of()); }
    public static <T extends Enum<T>>JsonEnumInfo of(String title, String index, Class<T> tEnum) {
        return of(title, index, Arrays.stream(tEnum.getEnumConstants())
                .map(Enum::name)
                .map(IJElement::raw)
                .collect(ImmutableList.toImmutableList())
        );
    }

    public static JsonEnumInfo of(String title, ImmutableMap<IJElement, Optional<IComment>> items) { return of(title, title.toLowerCase(), items); }
    public static JsonEnumInfo of(String title, ImmutableList<IJElement> items) { return of(title, title.toLowerCase(), items); }
    public static JsonEnumInfo of(String title) { return of(title, title.toLowerCase()); }
    public static <T extends Enum<T>>JsonEnumInfo of(String title, Class<T> tEnum) { return of(title, title.toLowerCase(), tEnum); }

    public JsonEnumInfo add(IJElement item) { return add(item, Optional.empty()); }
    public JsonEnumInfo add(IJElement item, Optional<IComment> comment) { return new JsonEnumInfo(this, item, comment); }

    @Override public Stream<String> context() {
        return Streams.concat(
                Stream.of("<pre><code>Список возможных значений:"),
                items.entrySet().stream().flatMap(kv -> item(kv.getKey(), kv.getValue())),
                Stream.of("</code></pre>")
        );
    }

    private Stream<String> item(IJElement item, Optional<IComment> comment) {
        Toast1<Integer> i = Toast.of(-1);
        List<String> lines = item.lines().collect(Collectors.toList());
        int size = lines.size();
        return switch (size) {
            case 0 -> throw new IllegalArgumentException("LINES IS ZERO IN ENUM INFO " + title);
            case 1 -> Stream.of("<warning> - </warning>" + lines.get(0) + comment.map(IComment::build).map(v -> " " + v).orElse(""));
            default -> {
                String first = lines.remove(0);
                yield Streams.concat(
                        Stream.of("<warning> - </warning>" + first + comment.map(IComment::build).map(v -> " " + v).orElse("")),
                        lines.stream());
            }
        };
    }
}