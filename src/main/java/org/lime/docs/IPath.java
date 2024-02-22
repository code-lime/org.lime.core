package org.lime.docs;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IPath extends IParent {
    String pathPart();

    static String parentPath(IParent parent) {
        List<String> parts = IParent.parentTree(parent)
                .flatMap(v -> v instanceof IPath path ? Stream.of(path.pathPart()) : Stream.empty())
                .collect(Collectors.toList());
        Collections.reverse(parts);
        return String.join("/", parts);
    }
}
