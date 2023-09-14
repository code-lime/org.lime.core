package org.lime.docs;

import org.lime.system.execute.Func0;

import javax.annotation.Nullable;

public interface IIndexDocs {
    @Nullable IIndexDocs parent();

    default String marker() { return "<a id=\""+ indexWithParent()+"\"></a>" + title(); }
    default String link() { return "<a href=\""+ hrefWithParent()+"\">"+title()+"</a>"; }

    String title();
    String index();

    default String indexWithParent() {
        IIndexDocs parent = parent();
        return (parent == null ? "" : (parent.indexWithParent() + "/")) + index();
    }
    default String hrefWithParent() { return "#" + indexWithParent(); }

    static IIndexDocs url(String title, String url) {
        return new IIndexDocs() {
            @Override public String index() { return url; }
            @Override public @Nullable IIndexDocs parent() { return null; }
            @Override public String title() { return title; }
            @Override public String hrefWithParent() { return indexWithParent(); }
        };
    }
    static IIndexDocs raw(String title, String index, @Nullable IIndexDocs parent) {
        return new IIndexDocs() {
            @Override public @Nullable IIndexDocs parent() { return parent; }
            @Override public String title() { return title; }
            @Override public String index() { return index; }
        };
    }

    static IIndexDocs remote(Func0<IIndexDocs> getter) {
        return new IIndexDocs() {
            IIndexDocs current() { return getter.invoke(); }

            @Nullable @Override public IIndexDocs parent() { return current().parent(); }
            @Override public String marker() { return current().marker(); }
            @Override public String link() { return current().link(); }
            @Override public String title() { return current().title(); }
            @Override public String index() { return current().index(); }
            @Override public String indexWithParent() { return current().indexWithParent(); }
            @Override public String hrefWithParent() { return current().hrefWithParent(); }
        };
    }
}
