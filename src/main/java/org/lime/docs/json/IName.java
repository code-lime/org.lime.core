package org.lime.docs.json;

import org.lime.docs.IIndexDocs;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface IName {
    String line(boolean require, IIndexDocs current);

    static IName link(IIndexDocs index) { return (require, current) -> index.link(current); }
    static IName raw(String name) { return (require, current) -> "<name>\"" + (require ? "" : "?") + name + "\"</name>"; }
    static IName or(IName... names) { return or(List.of(names)); }
    static IName or(Collection<IName> names) { return (require, current) -> names.stream().map(v -> v.line(require, current)).collect(Collectors.joining(" or ")); }

    default IName or(IName other) { return or(this, other); }
}
