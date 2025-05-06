package org.lime.core.common.docs.json;

import com.google.common.collect.Streams;
import com.google.gson.JsonPrimitive;
import org.lime.core.common.docs.IIndexDocs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface IJElement {
    Stream<String> lines(IIndexDocs current);

    String ANY_TEXT = "<any>...</any>";

    static IJElement link(IIndexDocs index) { return current -> Stream.of(index.link(current)); }
    static IJElement linkCurrent() { return current -> Stream.of(current.link(current)); }
    static IJElement linkParent() { return current -> Stream.of(Objects.requireNonNull((IIndexDocs)current.parent()).link(current)); }
    private static IJElement raw(String text, String style) { return current -> Stream.of("<"+style+">"+text+"</"+style+">"); }
    static IJElement raw(JsonPrimitive primitive) { return raw(primitive.toString(), primitive.isBoolean() ? "bool" : "string"); }
    static IJElement raw(String value) { return raw(new JsonPrimitive(value)); }
    static IJElement raw(Number number) { return raw(new JsonPrimitive(number)); }
    static IJElement raw(boolean bool) { return raw(new JsonPrimitive(bool)); }
    static IJElement range(Number from, Number to) { return current -> Stream.of("от <string>"+from+"</string> до <string>"+to+"</string>"); }
    static IJElement bool() { return current -> Stream.of("<bool>true</bool>/<bool>false</bool>"); }
    static IJElement nullable() { return raw("null", "bool"); }

    static IJElement text(String text) { return raw(text, "string"); }
    static IJElement field(String name) { return raw(name, "name"); }

    static IJElement any() { return current -> Stream.of(ANY_TEXT); }

    static <T extends IJElement>IJElement concat(String delimiter, T... elements) {
        return current -> switch (elements.length) {
            case 0 -> throw new IllegalArgumentException("`CONCAT` ELEMENTS IS ZERO");
            case 1 -> elements[0].lines(current);
            default -> {
                List<String> result = new ArrayList<>();
                for (IJElement element : elements) {
                    List<String> lines = element.lines(current).collect(Collectors.toList());
                    if (result.isEmpty()) {
                        result.addAll(lines);
                        continue;
                    }
                    if (lines.isEmpty()) throw new IllegalArgumentException("ONE OF ELEMENTS IN `CONCAT` IS ZERO");
                    int index = result.size() - 1;
                    result.set(index,  result.get(index) + delimiter + lines.remove(0));
                    result.addAll(lines);
                }
                yield result.stream();
            }
        };
    }
    static <T extends IJElement>IJElement concat(String delimiter, List<T> elements) {
        return current -> switch (elements.size()) {
            case 0 -> throw new IllegalArgumentException("`CONCAT` ELEMENTS IS ZERO");
            case 1 -> elements.get(0).lines(current);
            default -> {
                List<String> result = new ArrayList<>();
                for (IJElement element : elements) {
                    List<String> lines = element.lines(current).collect(Collectors.toList());
                    if (result.isEmpty()) {
                        result.addAll(lines);
                        continue;
                    }
                    if (lines.isEmpty()) throw new IllegalArgumentException("ONE OF ELEMENTS IN `CONCAT` IS ZERO");
                    int index = result.size() - 1;
                    result.set(index,  result.get(index) + delimiter + lines.remove(0));
                    result.addAll(lines);
                }
                yield result.stream();
            }
        };
    }
    default IJElement concat(String delimiter, IJElement element) { return concat(delimiter, this, element); }

    static <T extends IJElement>IJElement or(T... elements) { return concat(" or ", elements); }
    static <T extends IJElement>IJElement or(List<T> elements) { return concat(" or ", elements); }
    default IJElement or(IJElement element) { return or(this, element); }

    static <T extends IJElement>IJElement join(T... elements) { return concat("", elements); }
    static <T extends IJElement>IJElement join(List<T> elements) { return concat("", elements); }
    default IJElement join(IJElement element) { return join(this, element); }

    static IJElement comment(IJElement element, IComment comment) {
        return current -> {
            List<String> result = element.lines(current).collect(Collectors.toList());
            if (result.isEmpty()) throw new IllegalArgumentException("ELEMENT IN `COMMENT` IS ZERO");
            int index = result.size() - 1;
            result.set(index, result.get(index) + " " + comment.build(current));
            return result.stream();
        };
    }
    default IJElement comment(IComment comment) { return comment(this, comment); }

    private static IJElement listByElements(IJElement... elements) { return listByElements(List.of(elements)); }
    private static IJElement listByElements(List<IJElement> elements) {
        return current -> {
            int count = elements.size();
            return count == 0 ? Stream.of("[]") : Streams.concat(Stream.of("["), IntStream.range(0, count).boxed().flatMap(i -> {
                Stream<String> lines = elements.get(i).lines(current);
                if (i == count - 1) return lines;

                List<String> list = lines.collect(Collectors.toList());
                if (list.isEmpty()) return Stream.empty();
                String last = list.remove(list.size() - 1);
                return Stream.concat(list.stream(), Stream.of(last + ","));
            }).map(v -> "\t" + v), Stream.of("]"));
        };
    }

    static IJElement list(IJElement... elements) { return listByElements(elements); }
    static IJElement list(List<IJElement> elements) { return listByElements(elements); }

    static IJElement anyList(IJElement element) { return listByElements(element, element, any()); }
    static IJElement anyListWithPrefix(IJElement element, IJElement... prefix) {
        return listByElements(Stream.concat(Arrays.stream(prefix), Stream.of(element, element, any())).toList());
    }
    static IJElement anyObject(IJProperty property) { return JObject.of(property, property, IJProperty.any()); }
    static IJElement anyObjectWithPrefix(IJProperty property, IJProperty... prefix) { return JObject.of(Stream.concat(Arrays.stream(prefix), Stream.of(property, property, IJProperty.any())).toList()); }
}























