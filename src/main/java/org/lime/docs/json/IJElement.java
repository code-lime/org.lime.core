package org.lime.docs.json;

import com.google.common.collect.Streams;
import com.google.gson.JsonPrimitive;
import org.lime.docs.IIndexDocs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface IJElement {
    Stream<String> lines();

    String ANY_TEXT = "<any>...</any>";

    static IJElement link(IIndexDocs index) { return () -> Stream.of(index.link()); }
    private static IJElement raw(String text, String style) { return () -> Stream.of("<"+style+">"+text+"</"+style+">"); }
    static IJElement raw(JsonPrimitive primitive) { return raw(primitive.toString(), primitive.isBoolean() ? "bool" : "string"); }
    static IJElement raw(String value) { return raw(new JsonPrimitive(value)); }
    static IJElement raw(Number number) { return raw(new JsonPrimitive(number)); }
    static IJElement range(Number from, Number to) { return () -> Stream.of("от <string>"+from+"</string> до <string>"+to+"</string>"); }
    static IJElement bool() { return () -> Stream.of("<bool>true</bool>/<bool>false</bool>"); }
    static IJElement nullable() { return raw("null", "bool"); }

    static IJElement text(String text) { return raw(text, "string"); }

    static IJElement any() { return () -> Stream.of(ANY_TEXT); }

    /*
    private static <T>Stream<T> combine(Stream<T> stream, system.Func2<T, T, T> combine) {

        system.Toast1<T> last = system.toast(null);
        system.Toast1<Integer> status = system.toast(0);
        return Stream.concat(stream.flatMap(v -> {
            if (last.val0 == null) {
                last.val0 = v;
                status.val0 = 1;
                return Stream.empty();
            }
            status.val0 = 2;
            return Stream.of(combine.invoke(last.val0, v));
        }), Stream.of("").flatMap(v -> status.val0 != 2 ? Stream.of(last.val0) : Stream.<T>empty()));
    }

    private static <T>Stream<system.Toast2<T, Boolean>> lastStream(Stream<T> stream) {
        Iterator<T> iterator = stream.iterator();
        Iterator<system.Toast2<T, Boolean>> result = new Iterator<system.Toast2<T, Boolean>>() {
            @Override public boolean hasNext() { return iterator.hasNext(); }
            @Override public system.Toast2<T, Boolean> next() { return system.toast(iterator.next(), !iterator.hasNext()); }
        };
        return Streams.stream(result);
    }
    private static <T>Stream<T> joinWithLast(Stream<T> a, Stream<T> b, system.Func2<T, T, T> join) {
        return lastStream(a).flatMap(v -> {
            if (!v.val1) return Stream.of(v.val0);
            system.Toast1<Boolean> isFirst = system.toast(true);
            return Stream.concat(b.map(e -> {
                if (isFirst.val0) {
                    isFirst.val0 = false;
                    return join.invoke(v.val0, e);
                }
                return e;
            }), Stream.of(v.val0).filter(e -> !isFirst.val0));
        });
    }
    /*private static <T, A>Stream<A> joinWithLast(Stream<T> a, Stream<T> b, system.Func1<T, A> format, system.Func2<T, T, A> join) {
        return lastStream(a).flatMap(v -> {
            if (!v.val1) return Stream.of(format.invoke(v.val0));
            return
        })
    }*/

    static IJElement concat(String delimiter, IJElement... elements) {
        return () -> switch (elements.length) {
            case 0 -> throw new IllegalArgumentException("`CONCAT` ELEMENTS IS ZERO");
            case 1 -> elements[0].lines();
            default -> {
                List<String> result = new ArrayList<>();
                for (IJElement element : elements) {
                    List<String> lines = element.lines().collect(Collectors.toList());
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
    default IJElement concat(String delimiter, IJElement element) { return concat(delimiter, new IJElement[] { element }); }
    static IJElement or(IJElement... elements) { return concat(" or ", elements); }
    default IJElement or(IJElement element) { return or(this, element); }

    private static IJElement listByElements(IJElement... elements) { return listByElements(List.of(elements)); }
    private static IJElement listByElements(List<IJElement> elements) {
        return () -> {
            int count = elements.size();
            return count == 0 ? Stream.of("[]") : Streams.concat(Stream.of("["), IntStream.range(0, count).boxed().flatMap(i -> {
                Stream<String> lines = elements.get(i).lines();
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
    static IJElement anyObject(IJProperty property) { return JObject.of(property, property, IJProperty.any()); }
}























