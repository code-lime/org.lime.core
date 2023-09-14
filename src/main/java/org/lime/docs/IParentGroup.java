package org.lime.docs;

import java.util.stream.Stream;

public interface IParentGroup extends IIndexGroup {
    Stream<IGroup> childs();

    @Override default Stream<String> lines() {
        return Stream.concat(
                IIndexGroup.super.lines(),
                childs()
                        .flatMap(_v -> Stream.concat(
                                Stream.of(""),
                                _v.lines().map(v -> {
                                    int length = v.length();
                                    int level = 0;
                                    boolean end = false;
                                    for (int i = 0; i < length; i++) {
                                        char ch = v.charAt(0);
                                        switch (ch) {
                                            case '>' -> level++;
                                            case ' ' -> {}
                                            default -> end = true;
                                        }
                                        if (end) break;
                                        v = v.substring(1);
                                    }
                                    if (v.startsWith("#")) v = " #" + v;
                                    return ">".repeat(level + 1) + v;
                                })
                        ))
        );
    }
}
