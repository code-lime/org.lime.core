package org.lime.core.common.utils.json.builder;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.lime.core.common.utils.system.execute.Func1;
import org.lime.core.common.utils.system.execute.Func3;
import org.lime.core.common.utils.system.utils.IterableUtils;

import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.*;

public class Json {
    public static JsonElement parse(String json) { return JsonParser.parseString(json); }
    public static JsonElement parse(Reader json) { return JsonParser.parseReader(json); }
    public static JsonElement parse(JsonReader json) { return JsonParser.parseReader(json); }

    public static ObjectBuilder object() { return new ObjectBuilder(); }
    public static ArrayBuilder array() { return new ArrayBuilder(); }

    public static BaseBuilder<?> by(Object obj) { return BaseBuilder.byObject(obj); }

    public static ObjectBuilder of(JsonObject json) { return new ObjectBuilder(json); }
    public static ArrayBuilder of(JsonArray json) { return new ArrayBuilder(json); }

    public static JsonObject object(Func1<ObjectBuilder, ObjectBuilder> func) { return func.invoke(object()).build(); }
    public static JsonArray array(Func1<ArrayBuilder, ArrayBuilder> func) { return func.invoke(array()).build(); }
    public static GetterBuilder getter(JsonElement json) { return new GetterBuilder(json); }

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
    private static final TextColor formatColorName = TextColor.color(0x00FFFF);

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













