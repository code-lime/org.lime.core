package org.lime.docs;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface IIndexGroup extends IGroup, IIndexDocs {
    void setParent(@Nullable IIndexGroup parent);
    Stream<String> context();

    @Override default Stream<String> lines() { return Stream.concat(Stream.of("## " + title()), context()); }

    static IIndexGroup empty(String title, String index, @Nullable IIndexDocs parent) {
        return new IIndexGroup() {
            private IIndexDocs _parent = parent;
            @Override public void setParent(@Nullable IIndexGroup parent) { _parent = parent; }
            @Override public Stream<String> context() { return Stream.empty(); }
            @Override public @Nullable IIndexDocs parent() { return _parent; }
            @Override public String title() { return title; }
            @Override public String index() { return index; }
        };
    }

    default IParentGroup withChild(IGroup child) { return withChilds(child); }
    default IParentGroup withChilds(IGroup... childs) { return withChilds(List.of(childs)); }
    default IParentGroup withChilds(Collection<IGroup> childs) {
        childs.forEach(v -> {
            if (v instanceof IIndexGroup group)
                group.setParent(this);
        });
        return new IParentGroup() {
            @Override public Stream<IGroup> childs() {
                return IIndexGroup.this instanceof IParentGroup parent
                        ? Stream.concat(parent.childs(), childs.stream())
                        : childs.stream();
            }
            @Override public void setParent(@Nullable IIndexGroup parent) { IIndexGroup.this.setParent(parent); }
            @Override public Stream<String> context() { return IIndexGroup.this.context(); }
            @Override public @Nullable IIndexDocs parent() { return IIndexGroup.this.parent(); }
            @Override public String title() { return IIndexGroup.this.title(); }
            @Override public String index() { return IIndexGroup.this.index(); }
        };
    }
}
