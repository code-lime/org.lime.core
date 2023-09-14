package org.lime.docs.json;

import java.util.stream.Stream;

public interface IJProperty {
    Stream<String> lines(boolean isLast);

    static IJProperty any() { return isLast -> Stream.of(IJElement.ANY_TEXT + (isLast ? "" : ",")); }
}
