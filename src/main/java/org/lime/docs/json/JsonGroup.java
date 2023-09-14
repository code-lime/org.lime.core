package org.lime.docs.json;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.lime.system.toast.*;
import org.lime.docs.IIndexDocs;
import org.lime.docs.IIndexGroup;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public final class JsonGroup implements IIndexGroup {
    private final String title;
    private final String index;
    private final IJElement element;
    private final ImmutableList<String> comments;

    @Override public String title() { return title; }
    @Override public String index() { return index; }
    public IJElement element() { return element; }
    public ImmutableList<String> comments() { return comments; }

    private IIndexDocs parent = null;
    @Override @Nullable public IIndexDocs parent() { return parent; }
    @Override public void setParent(@Nullable IIndexGroup parent) { this.parent = parent; }

    public JsonGroup(String title, String index, IJElement element, ImmutableList<String> comments) {
        this.title = title;
        this.index = index;
        this.element = element;
        this.comments = comments;
    }

    public static JsonGroup of(String title, String index, IJElement element) { return new JsonGroup(title, index, element, ImmutableList.of()); }
    public static JsonGroup of(String title, String index, IJElement element, String... comments) { return new JsonGroup(title, index, element, ImmutableList.copyOf(comments)); }

    public static JsonGroup of(String title, IJElement element) { return of(title, title.toLowerCase(), element); }
    public static JsonGroup of(String title, IJElement element, String... comments) { return of(title, title.toLowerCase(), element, comments); }

    @Override public Stream<String> context() {
        Toast1<Boolean> isFirst = Toast.of(true);
        return Streams.concat(
                comments.isEmpty() ? Stream.empty() : Stream.of(String.join("<br>", comments), ""),
                element.lines()
                        .map(v -> {
                            if (isFirst.val0) {
                                isFirst.val0 = false;
                                return "<pre><code>" + v;
                            }
                            return v;
                        }),
                Stream.of("</code></pre>")
                        .map(v -> (isFirst.val0 ? "<pre><code>" : "")+v)
        );
    }
}