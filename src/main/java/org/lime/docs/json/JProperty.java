package org.lime.docs.json;

import com.google.common.collect.Streams;
import org.lime.system.toast.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record JProperty(
        IName name,
        boolean require,
        IJElement value,
        Optional<IComment> comment
) implements IJProperty {
    @Override public Stream<String> lines(boolean isLast) {
        Toast1<Integer> i = Toast.of(-1);
        List<String> lines = value.lines().collect(Collectors.toList());
        int size = lines.size();
        return switch (size) {
            case 0 -> throw new IllegalArgumentException("LINES IS ZERO IN FIELD " + name.line(false));
            case 1 -> Stream.of(name.line(require) + ": " + lines.get(0) + (isLast ? "" : ",") + comment.map(IComment::build).map(v -> " " + v).orElse(""));
            default -> {
                String last = lines.remove(size - 1);
                String first = lines.remove(0);
                yield Streams.concat(
                        Stream.of(name.line(require) + ": " + first + comment.map(IComment::build).map(v -> " " + v).orElse("")),
                        lines.stream(),
                        Stream.of(last + (isLast ? "" : ",")));
            }
        };
    }

    public static JProperty require(IName name, IJElement value) { return property(true, name, value); }
    public static JProperty require(IName name, IJElement value, IComment comment) { return property(true, name, value, comment); }

    public static JProperty optional(IName name, IJElement value) { return property(false, name, value); }
    public static JProperty optional(IName name, IJElement value, IComment comment) { return property(false, name, value, comment); }

    public static JProperty property(boolean require, IName name, IJElement value) { return new JProperty(name, require, value, Optional.empty()); }
    public static JProperty property(boolean require, IName name, IJElement value, IComment comment) { return new JProperty(name, require, value, Optional.of(comment)); }
}









