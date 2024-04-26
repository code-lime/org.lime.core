package org.lime.system;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.lime.system.execute.*;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;
import org.lime.system.utils.IterableUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class json {
    private static final JsonParser parser = new JsonParser();

    public static JsonElement parse(String json) { return parser.parse(json); }
    public static JsonElement parse(Reader json) { return parser.parse(json); }
    public static JsonElement parse(JsonReader json) { return parser.parse(json); }

    public static abstract class builder<T extends JsonElement> {
        private static List<Object> toList(Object array) {
            int length = Array.getLength(array);
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < length; i++) list.add(Array.get(array, i));
            return list;
        }

        public static builder<?> byObject(Object value) {
            if (value == null) return builder.element.create();
            else if (value instanceof builder<?> dat) return dat;
            else if (value instanceof String dat) return builder.element.create(dat);
            else if (value instanceof Number dat) return builder.element.create(dat);
            else if (value instanceof Boolean dat) return builder.element.create(dat);
            else if (value instanceof Character dat) return builder.element.create(dat);
            else if (value instanceof Enum<?> dat) return builder.element.create(dat.name());
            else if (value instanceof JsonElement dat) return builder.element.create(dat);
            else if (value instanceof Map<?, ?> dat) return object().add(dat, String::valueOf, v -> v);
            else if (value instanceof Iterable<?> dat) return array().add(dat, v -> v);
            else if (value.getClass().isArray()) return byObject(toList(value));
            else return builder.element.create(value.toString());
        }

        public static class element extends builder<JsonElement> {
            private final JsonElement json;

            private element(JsonElement json) { this.json = json; }
            private element() { this(JsonNull.INSTANCE); }
            private element(String value) { this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value)); }
            private element(Number value) { this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value)); }
            private element(Boolean value) { this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value)); }
            private element(Character value) { this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value)); }
            public static element create() { return new element(); }
            public static element create(JsonElement value) { return new element(value); }
            public static element create(builder<?> value) { return new element(value.build()); }
            public static element create(String value) { return new element(value); }
            public static element create(Number value) { return new element(value); }
            public static element create(Boolean value) { return new element(value); }
            public static element create(Character value) { return new element(value); }

            @Override public JsonElement build() { return json; }
        }
        public static class object extends builder<JsonObject> {
            private final JsonObject json;

            private object(JsonObject json) { this.json = json; }
            private object() { this(new JsonObject()); }

            public object addObject(String key, Func1<object, object> value) { return add(key, value.invoke(new object())); }
            public object addArray(String key, Func1<array, array> value) { return add(key, value.invoke(new array())); }

            public <TValue> object add(Map<String, TValue> map) {
                map.forEach(this::add);
                return this;
            }
            public <TKey, TValue> object add(Map<TKey, TValue> map, Func1<TKey, String> key, Func1<TValue, Object> value) {
                map.forEach((k, v) -> add(key.invoke(k), value.invoke(v)));
                return this;
            }
            public <T> object add(Iterable<T> list, Func1<T, String> key, Func1<T, Object> value) {
                list.forEach(i -> add(key.invoke(i), value.invoke(i)));
                return this;
            }
            public <T> object add(Iterator<T> list, Func1<T, String> key, Func1<T, Object> value) {
                list.forEachRemaining(i -> add(key.invoke(i), value.invoke(i)));
                return this;
            }

            public object add(String key, JsonElement value) {
                json.add(key, value);
                return this;
            }
            public object add(JsonObject json) {
                json.entrySet().forEach(kv -> this.add(kv.getKey(), kv.getValue()));
                return this;
            }

            public object addNull(String key) { return add(key, byObject(null)); }
            public object add(String key, builder<?> value) { return value == null ? addNull(key) : add(key, value.build()); }
            public object add(String key, Object value) { return add(key, byObject(value)); }
            public object add(object json) { return add(json.build()); }

            @Override public JsonObject build() { return json; }
        }
        public static class array extends builder<JsonArray> {
            private final JsonArray json;

            private array(JsonArray json) { this.json = json; }
            private array() { this(new JsonArray()); }

            public <T> array add(Iterable<T> items) {
                items.forEach(this::add);
                return this;
            }
            public <In, T> array add(Iterable<In> items, Func1<In, T> format) {
                items.forEach(item -> add(format.invoke(item)));
                return this;
            }
            public <T> array add(Iterator<T> items) {
                items.forEachRemaining(this::add);
                return this;
            }
            public <In, T> array add(Iterator<In> items, Func1<In, T> format) {
                items.forEachRemaining(item -> add(format.invoke(item)));
                return this;
            }
            public <In, T> array add(In[] items, Func1<In, T> format) {
                for (In item : items) add(format.invoke(item));
                return this;
            }
            public array add(JsonElement value) {
                json.add(value);
                return this;
            }

            public array addObject(Func1<object, object> value) { return add(value.invoke(new object())); }
            public array addArray(Func1<array, array> value) { return add(value.invoke(new array())); }
            public array addNull() { return add(byObject(null)); }
            public array add(builder<?> value) { return add(value.build()); }
            public array add(Object value) { return add(byObject(value)); }

            @Override public JsonArray build() { return json; }
        }
        public static class getter extends builder<JsonElement> {
            private final List<Toast2<String, Func1<JsonElement, JsonElement>>> list = new ArrayList<>();
            private final JsonElement base;

            private getter(JsonElement base) { this.base = base; }

            public getter of(int index) {
                list.add(Toast.of("[" + index + "]", json -> json != null && json.isJsonArray() ? json.getAsJsonArray().get(index) : null));
                return this;
            }
            public getter last() {
                list.add(Toast.of("[last]", json -> {
                    if (!json.isJsonArray()) return null;
                    JsonArray arr = json.getAsJsonArray();
                    int length = arr.size();
                    return length == 0 ? null : arr.get(length - 1);
                }));
                return this;
            }
            public getter of(String key) {
                list.add(Toast.of("." + key, json -> json != null && json.isJsonObject() ? json.getAsJsonObject().get(key) : null));
                return this;
            }

            @Override public JsonElement build() {
                JsonElement json = base;
                for (Toast2<String, Func1<JsonElement, JsonElement>> item : list)
                    json = item.val1.invoke(json);
                return json;
            }

            public JsonArray array() {
                JsonElement json = build();
                return json == null || !json.isJsonArray() ? null : json.getAsJsonArray();
            }
            public JsonObject object() {
                JsonElement json = build();
                return json == null || !json.isJsonObject() ? null : json.getAsJsonObject();
            }
            public JsonPrimitive primitive() {
                JsonElement json = build();
                return json == null || !json.isJsonPrimitive() ? null : json.getAsJsonPrimitive();
            }

            public <T> T other(Func1<JsonElement, T> parse, T def) { return otherFunc(parse, () -> def); }
            public <T> T other(Func1<JsonElement, T> parse) {
                return otherFunc(parse, () -> {
                    throw new IllegalArgumentException("Path 'JSON." + list.stream().map(v -> v.val0).collect(Collectors.joining()) + "' not founded!");
                });
            }
            public <T> T otherFunc(Func1<JsonElement, T> parse, Func0<T> def) {
                JsonElement json = build();
                if (json == null) return def.invoke();
                try {
                    return parse.invoke(json);
                } catch (Exception e) {
                    return def.invoke();
                }
            }
        }

        public abstract T build();

        @Override public String toString() { return build().toString(); }
    }

    public static builder.object object() { return new builder.object(); }

    public static builder.array array() { return new builder.array(); }

    public static builder<?> by(Object obj) { return builder.byObject(obj); }

    public static builder.object of(JsonObject json) { return new builder.object(json); }
    public static builder.array of(JsonArray json) { return new builder.array(json); }

    public static JsonObject object(Func1<builder.object, builder.object> func) { return func.invoke(object()).build(); }
    public static JsonArray array(Func1<builder.array, builder.array> func) { return func.invoke(array()).build(); }
    public static builder.getter getter(JsonElement json) { return new builder.getter(json); }

    public static void clear(JsonObject json) { json.entrySet().clear(); }
    public static void clear(JsonArray json) { IterableUtils.removeAll(json.iterator()); }

    public static JsonObject deepCopy(JsonObject jsonObject) {
        JsonObject result = new JsonObject();
        jsonObject.entrySet().forEach(kv -> result.add(kv.getKey(), deepCopy(kv.getValue())));
        return result;
    }
    public static JsonArray deepCopy(JsonArray jsonArray) {
        JsonArray result = new JsonArray();
        jsonArray.forEach(e -> result.add(deepCopy(e)));
        return result;
    }
    public static <T extends JsonElement>T deepCopy(T jsonElement) {
        if (jsonElement == null) return null;
        else if (jsonElement.isJsonPrimitive() || jsonElement.isJsonNull()) return jsonElement;
        else if (jsonElement.isJsonObject()) return (T)deepCopy(jsonElement.getAsJsonObject());
        else if (jsonElement.isJsonArray()) return (T)deepCopy(jsonElement.getAsJsonArray());
        else throw new UnsupportedOperationException("Unsupported element: " + jsonElement);
    }

    public static String format(JsonElement json) {
        return format(json, "    ");
    }
    public static String format(JsonElement json, String indent) {
        try {
            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setIndent(indent);
            jsonWriter.setLenient(true);
            com.google.gson.internal.Streams.write(json, jsonWriter);
            return stringWriter.toString();
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public static Component formatComponent(JsonElement json) {
        return formatComponent(json, "    ");
    }
    public static Component formatComponent(JsonElement json, @Nullable String indent) {
        return indent == null
                ? Component.join(JoinConfiguration.noSeparators(), formatComponentLines(json, ""))
                : Component.join(JoinConfiguration.newlines(), formatComponentLines(json, indent));
    }

    private static final TextColor formatColorString = TextColor.color(0xD69D85);
    private static final TextColor formatColorValue = TextColor.color(0x569CD6);
    private static final TextColor formatColorName = TextColor.color(0X00FFFF);

    private static Collection<Component> formatComponentLines(JsonElement json, String indent) {
        String raw = json.toString();
        if (json instanceof JsonObject object) {
            List<Component> lines = new ArrayList<>();
            object.asMap().forEach((key, item) -> {
                int lastIndex = lines.size() - 1;
                if (lastIndex >= 0)
                    lines.set(lastIndex, lines.get(lastIndex).append(Component.text(",")));

                boolean isFirst = true;
                for (Component line : formatComponentLines(item, indent)) {
                    if (isFirst) {
                        isFirst = false;
                        lines.add(Component.text(indent)
                                .append(Component.text(new JsonPrimitive(key).toString())
                                        .color(formatColorName)
                                        .hoverEvent(HoverEvent.showText(Component.text("Скопировать ")
                                                .append(Component.text(key).color(formatColorName))))
                                        .clickEvent(ClickEvent.copyToClipboard(key)))
                                .append(Component.text(": "))
                                .append(line));
                    } else {
                        lines.add(Component.text(indent).append(line));
                    }
                }
            });

            if (lines.isEmpty())
                return Collections.singletonList(Component.text("{}")
                        .hoverEvent(HoverEvent.showText(Component.text("Скопировать ")
                                .append(Component.text("{...}").color(formatColorValue))))
                        .clickEvent(ClickEvent.copyToClipboard(raw)));
            lines.add(0, Component.text("{")
                    .hoverEvent(HoverEvent.showText(Component.text("Скопировать ")
                            .append(Component.text("{...}").color(formatColorValue))))
                    .clickEvent(ClickEvent.copyToClipboard(raw)));
            lines.add(Component.text("}")
                    .hoverEvent(HoverEvent.showText(Component.text("Скопировать ")
                            .append(Component.text("{...}").color(formatColorValue))))
                    .clickEvent(ClickEvent.copyToClipboard(raw)));
            return lines;
        } else if (json instanceof JsonArray array) {
            List<Component> lines = new ArrayList<>();
            for (JsonElement item : array) {
                int lastIndex = lines.size() - 1;
                if (lastIndex >= 0)
                    lines.set(lastIndex, lines.get(lastIndex).append(Component.text(",")));

                for (Component line : formatComponentLines(item, indent)) {
                    lines.add(Component.text(indent).append(line));
                }
            }
            if (lines.isEmpty())
                return Collections.singletonList(Component.text("[]")
                        .hoverEvent(HoverEvent.showText(Component.text("Скопировать ")
                                .append(Component.text("[...]").color(formatColorValue))))
                        .clickEvent(ClickEvent.copyToClipboard(raw)));
            lines.add(0, Component.text("[")
                    .hoverEvent(HoverEvent.showText(Component.text("Скопировать ")
                            .append(Component.text("[...]").color(formatColorValue))))
                    .clickEvent(ClickEvent.copyToClipboard(raw)));
            lines.add(Component.text("]")
                    .hoverEvent(HoverEvent.showText(Component.text("Скопировать ")
                            .append(Component.text("[...]").color(formatColorValue))))
                    .clickEvent(ClickEvent.copyToClipboard(raw)));
            return lines;
        } else if (json instanceof JsonPrimitive primitive) {
            Component formatted;
            TextColor color;
            if (primitive.isBoolean()) {
                boolean value = primitive.getAsBoolean();

                color = formatColorValue;
                formatted = Component.text(value)
                        .color(formatColorValue);
            } else if (primitive.isNumber()) {
                Number value = primitive.getAsNumber();

                color = formatColorString;
                formatted = value instanceof Integer
                        ? Component.text(value.intValue())
                        : Component.text(value.doubleValue());
                formatted = formatted.color(formatColorString);
            } else if (primitive.isString()) {
                color = formatColorString;
                formatted = Component.empty()
                        .append(Component.text('"').color(formatColorName))
                        .append(Component.text(raw.substring(1, raw.length() - 1)).color(formatColorString))
                        .append(Component.text('"').color(formatColorName));

                raw = primitive.getAsString();
            } else throw new IllegalArgumentException("Json '"+json+"' not supported");

            return Collections.singletonList(formatted
                    .hoverEvent(HoverEvent.showText(Component.text("Скопировать ")
                            .append(Component.text(raw).color(color))))
                    .clickEvent(ClickEvent.copyToClipboard(raw)));
        } else if (json instanceof JsonNull) {
            return Collections.singletonList(Component.text("null")
                    .color(formatColorValue)
                    .hoverEvent(HoverEvent.showText(Component.text("Скопировать ")
                            .append(Component.text("null").color(formatColorValue))))
                    .clickEvent(ClickEvent.copyToClipboard("null")));
        } else throw new IllegalArgumentException("Json '"+json+"' not supported");
    }

    public static JsonObject editStringToObject(JsonObject jsonObject, Func1<String, JsonElement> edit) {
        JsonObject result = new JsonObject();
        jsonObject.entrySet().forEach(kv -> result.add(edit.invoke(kv.getKey()).getAsString(), editStringToObject(kv.getValue(), edit)));
        return result;
    }
    public static JsonArray editStringToObject(JsonArray jsonArray, Func1<String, JsonElement> edit) {
        JsonArray result = new JsonArray();
        jsonArray.forEach(e -> result.add(editStringToObject(e, edit)));
        return result;
    }
    public static JsonElement editStringToObject(JsonElement jsonElement, Func1<String, JsonElement> edit) {
        if (jsonElement.isJsonNull()) return jsonElement;
        else if (jsonElement.isJsonPrimitive()) return jsonElement.getAsJsonPrimitive().isString() ? edit.invoke(jsonElement.getAsString()) : jsonElement;
        else if (jsonElement.isJsonObject()) return editStringToObject(jsonElement.getAsJsonObject(), edit);
        else if (jsonElement.isJsonArray()) return editStringToObject(jsonElement.getAsJsonArray(), edit);
        else throw new UnsupportedOperationException("Unsupported element: " + jsonElement);
    }

    public static JsonObject modifyObjectByKey(JsonObject jsonObject, Func3<String, JsonElement, JsonObject, Boolean> edit) {
        JsonObject result = new JsonObject();
        jsonObject.entrySet().forEach(kv -> {
            if (!edit.invoke(kv.getKey(), kv.getValue(), result))
                result.add(kv.getKey(), modifyObjectByKey(kv.getValue(), edit));
        });
        return result;
    }
    public static JsonArray modifyObjectByKey(JsonArray jsonArray, Func3<String, JsonElement, JsonObject, Boolean> edit) {
        JsonArray result = new JsonArray();
        jsonArray.forEach(e -> result.add(modifyObjectByKey(e, edit)));
        return result;
    }
    public static JsonElement modifyObjectByKey(JsonElement jsonElement, Func3<String, JsonElement, JsonObject, Boolean> edit) {
        if (jsonElement.isJsonNull()) return jsonElement;
        else if (jsonElement.isJsonPrimitive()) return jsonElement;
        else if (jsonElement.isJsonObject()) return modifyObjectByKey(jsonElement.getAsJsonObject(), edit);
        else if (jsonElement.isJsonArray()) return modifyObjectByKey(jsonElement.getAsJsonArray(), edit);
        else throw new UnsupportedOperationException("Unsupported element: " + jsonElement);
    }
}













