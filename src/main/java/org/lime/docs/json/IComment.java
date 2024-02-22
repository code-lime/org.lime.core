package org.lime.docs.json;

import com.google.gson.JsonPrimitive;
import org.lime.docs.IIndexDocs;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IComment {
    String line(IIndexDocs current);
    default String build(IIndexDocs current) { return "<comment>//"+line(current)+"</comment>"; }

    static IComment empty() { return current -> ""; }

    static IComment link(IIndexDocs index) { return index::link; }
    static IComment linkCurrent() { return current -> current.link(current); }
    static IComment text(String text) { return current -> text; }
    private static IComment raw(String text, String style) { return current -> "<"+style+">"+text+"</"+style+">"; }
    static IComment raw(JsonPrimitive primitive) { return raw(primitive.getAsString(), primitive.isBoolean() ? "bool" : "string"); }
    static IComment raw(Enum<?> value) { return raw(new JsonPrimitive(value.name())); }
    static IComment raw(String value) { return raw(new JsonPrimitive(value)); }
    static IComment raw(Number number) { return raw(new JsonPrimitive(number)); }
    static IComment raw(boolean bool) { return raw(new JsonPrimitive(bool)); }
    static IComment field(String name) { return raw(name, "name"); }
    static IComment warning(String text) { return raw(text, "warning"); }
    static IComment range(Number from, Number to) { return current -> "от <string>"+from+"</string> до <string>"+to+"</string>"; }

    static IComment italic(IComment other) { return current -> "<i>"+other.line(current)+"</i>"; }
    static IComment bold(IComment other) { return current -> "<b>"+other.line(current)+"</b>"; }

    static IComment join(IComment... list) { return current -> Stream.of(list).map(v -> v.line(current)).collect(Collectors.joining()); }
    static IComment or(IComment... list) { return or(List.of(list)); }
    static IComment or(Collection<IComment> list) { return current -> list.stream().map(v -> v.line(current)).collect(Collectors.joining(" or ")); }

    default IComment italic() { return italic(this); }
    default IComment bold() { return bold(this); }
    default IComment append(IComment next) { return join(this, next); }
    default IComment or(IComment other) { return or(this, other); }
}














