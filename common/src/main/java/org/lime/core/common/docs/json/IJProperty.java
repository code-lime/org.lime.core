package org.lime.core.common.docs.json;

import org.lime.core.common.docs.IIndexDocs;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IJProperty {
    Stream<String> lines(IIndexDocs current, boolean isLast);

    static IJProperty any() { return (current, isLast) -> Stream.of(IJElement.ANY_TEXT + (isLast ? "" : ",")); }
    static IJProperty base(IIndexDocs base) { return (current, isLast) -> Stream.of(IJElement.link(base).lines(current).collect(Collectors.joining("")) + (isLast ? "" : ",")); }
}
