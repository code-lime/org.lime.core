package org.lime.docs.json;

import com.google.gson.JsonPrimitive;
import org.lime.docs.IIndexDocs;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IComment {
    String line();
    default String build() { return "<comment>//"+line()+"</comment>"; }

    static IComment empty() { return () -> ""; }

    static IComment link(IIndexDocs index) { return index::link; }
    static IComment text(String text) { return () -> text; }
    private static IComment raw(String text, String style) { return () -> "<"+style+">"+text+"</"+style+">"; }
    static IComment raw(JsonPrimitive primitive) { return raw(primitive.getAsString(), primitive.isBoolean() ? "bool" : "string"); }
    static IComment raw(Enum<?> value) { return raw(new JsonPrimitive(value.name())); }
    static IComment raw(String value) { return raw(new JsonPrimitive(value)); }
    static IComment raw(Number number) { return raw(new JsonPrimitive(number)); }
    static IComment field(String name) { return raw(name, "name"); }
    static IComment warning(String text) { return raw(text, "warning"); }

    static IComment italic(IComment other) { return () -> "<i>"+other.line()+"</i>"; }
    static IComment bold(IComment other) { return () -> "<b>"+other.line()+"</b>"; }

    static IComment join(IComment... list) { return () -> Stream.of(list).map(IComment::line).collect(Collectors.joining()); }
    static IComment or(IComment... list) { return or(List.of(list)); }
    static IComment or(Collection<IComment> list) { return () -> list.stream().map(IComment::line).collect(Collectors.joining(" or ")); }

    default IComment italic() { return italic(this); }
    default IComment bold() { return bold(this); }
    default IComment append(IComment next) { return join(this, next); }
    default IComment or(IComment other) { return or(this, other); }
}














