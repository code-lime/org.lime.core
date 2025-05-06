package org.lime.core.common.docs;

import org.lime.core.common.system.execute.Func0;

import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public interface IIndexDocs extends IParent {
    String RAW_HREF_PREFIX = "raw-href-";

    static Stream<String> marker(String index, String title, int level) {
        //return Stream.of("<a id=\""+ index+"\"></a>" + title);
        return Stream.of(
                "",
                "<empty>",
                "",
                "# "+index,
                "",
                "</empty>",
                "",
                "#".repeat(level) + " " + title
        );
    }
    default Stream<String> marker() { return marker(RAW_HREF_PREFIX+indexWithParent(), title(), 2); }
    default String link(IParent current) { return "<a href=\"#"+ hrefWithParent(current)+"\">"+title()+"</a>"; }

    String title();
    String index();

    default String indexWithParent() {
        IParent parent = parent();
        return (parent instanceof IIndexDocs parentDocs ? parentDocs.indexWithParent() + "-" : "") + index();
    }
    default String hrefWithParent(IParent current) {
        String fragment = indexWithParent();
        String index = RAW_HREF_PREFIX + fragment;
        if (this == current)
            return index;
        String absolute = IPath.parentPath(this);
        Path pathAbsolute = Paths.get(absolute);
        Path pathBase = Paths.get(IPath.parentPath(current));
        if (pathAbsolute.equals(pathBase))
            return index;
        Path pathCurrent = pathBase.getParent();
        if (pathCurrent == null)
            return "!!!ERROR GET CURRENT PATH FROM '" + pathAbsolute + "' AND '" + pathBase + "' IN '" + fragment + "'!!!";
        //if (pathCurrent == null) pathCurrent = pathBase;

        Path pathRelative = pathCurrent.relativize(pathAbsolute);

        String file = pathRelative.toString();
        if (file.isEmpty())
            return index;

        return IParent.parentTree(current)
                .flatMap(v -> v instanceof IHrefLink href ? Stream.of(href) : Stream.empty())
                .findAny()
                .map(href -> href.addHref(title(), index, absolute.isEmpty() ? null : file, fragment))
                .orElseGet(() -> {
                    return "!!!ERROR CREATE HREF FROM '" + pathAbsolute + "' AND '" + pathBase + "' IN '" + fragment + "'!!!";
                    //file + "#" + fragment
                });
    }

    static IIndexDocs url(String title, String url) {
        return new IIndexDocs() {
            @Override public String index() { return url; }
            @Override public @Nullable IIndexDocs parent() { return null; }
            @Override public String title() { return title; }
            @Override public String hrefWithParent(IParent current) { return indexWithParent(); }
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

            @Nullable @Override public IParent parent() { return current().parent(); }
            @Override public Stream<String> marker() { return current().marker(); }
            @Override public String link(IParent owner) { return current().link(owner); }
            @Override public String title() { return current().title(); }
            @Override public String index() { return current().index(); }
            @Override public String indexWithParent() { return current().indexWithParent(); }
            @Override public String hrefWithParent(IParent current) { return current().hrefWithParent(current); }
        };
    }

    default IIndexDocs href() {
        return IIndexDocs.remote(() -> this);
    }
}
