package org.lime.docs;

import org.lime.system.execute.Func0;
import org.lime.system.execute.Func1;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface IIndexGroup extends IGroup, IIndexDocs {
    void changeParent(@Nullable IParent parent);
    Stream<String> context();

    @Override default Stream<String> lines() { return Stream.concat(marker(), context()); }

    static IIndexGroup empty(String title, String index, @Nullable IIndexDocs parent) {
        return new IIndexGroup() {
            private IParent _parent = parent;
            @Override public void changeParent(@Nullable IParent parent) { _parent = parent; }
            @Override public Stream<String> context() { return Stream.empty(); }
            @Override public @Nullable IParent parent() { return _parent; }
            @Override public String title() { return title; }
            @Override public String index() { return index; }
        };
    }
    static IIndexGroup empty(String title, @Nullable IIndexDocs parent) {
        return empty(title, title.toLowerCase(), parent);
    }

    static IIndexGroup raw(String title, String index, @Nullable IIndexDocs parent, Func1<IIndexDocs, Stream<String>> lines) {
        return new IIndexGroup() {
            private IParent _parent = parent;
            @Override public void changeParent(@Nullable IParent parent) { _parent = parent; }
            @Override public Stream<String> context() { return lines.invoke(this); }
            @Override public @Nullable IParent parent() { return _parent; }
            @Override public String title() { return title; }
            @Override public String index() { return index; }
        };
    }
    static IIndexGroup raw(String title, @Nullable IIndexDocs parent, Func1<IIndexDocs, Stream<String>> lines) {
        return raw(title, title.toLowerCase(), parent, lines);
    }

    static IIndexGroup raw(String title, String index, @Nullable IIndexDocs parent, Func0<Stream<String>> lines) {
        return raw(title, index, parent, v -> lines.invoke());
    }
    static IIndexGroup raw(String title, @Nullable IIndexDocs parent, Func0<Stream<String>> lines) {
        return raw(title, parent, v -> lines.invoke());
    }

    default IParentGroup withChild(IGroup child) { return withChilds(child); }
    default <T extends IGroup>IParentGroup withChilds(T... childs) { return withChilds(List.of(childs)); }
    default <T extends IGroup>IParentGroup withChilds(Collection<T> childs) {
        childs.forEach(v -> {
            if (v instanceof IIndexGroup group)
                group.changeParent(this);
        });
        return new IParentGroup() {
            @Override public Stream<? extends IGroup> childs() {
                return IIndexGroup.this instanceof IParentGroup parent
                        ? Stream.concat(parent.childs(), childs.stream())
                        : childs.stream();
            }
            @Override public void changeParent(@Nullable IParent parent) { IIndexGroup.this.changeParent(parent); }
            @Override public Stream<String> context() { return IIndexGroup.this.context(); }
            @Override public @Nullable IParent parent() { return IIndexGroup.this.parent(); }
            @Override public String title() { return IIndexGroup.this.title(); }
            @Override public String index() { return IIndexGroup.this.index(); }
        };
    }
}
