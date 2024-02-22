package org.lime.docs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.lime.docs.json.IComment;

import javax.annotation.Nullable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseGroup<T> implements IIndexGroup {
    private final String title;
    private final String index;
    private final T element;
    private final ImmutableList<IComment> comments;

    @Override public String title() { return title; }
    @Override public String index() { return index; }
    public T element() { return element; }
    public ImmutableList<IComment> comments() { return comments; }

    private IParent parent = null;
    @Override @Nullable public IParent parent() { return parent; }
    @Override public void changeParent(@Nullable IParent parent) { this.parent = parent; }

    public BaseGroup(String title, String index, T element, ImmutableList<IComment> comments) {
        this.title = title;
        this.index = index;
        this.element = element;
        this.comments = comments;
    }

    public abstract Stream<String> elementContext();

    @Override public final Stream<String> context() {
        return Streams.concat(
                comments.isEmpty() ? Stream.empty() : Stream.of(comments.stream().map(v -> v.line(this)).collect(Collectors.joining("<br>")), ""),
                elementContext()
        );
    }
}