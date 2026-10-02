package org.lime.core.common.api.commands.brigadier.arguments;

import com.google.gson.*;
import com.mojang.brigadier.*;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.*;
import com.mojang.brigadier.suggestion.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.*;
import org.lime.core.common.utils.execute.Func1;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Maps one vanilla-parsed SNBT value to Gson and provides typed completion. */
public final class SnbtJsonArgument<T, N> implements BaseMappedArgument<T, N> {
    private final ArgumentType<N> nativeType;
    private final Func1<N, JsonElement> json;
    private final JsonInput input;
    private final TypeAdapter<T> adapter;
    private final Function<Component, Message> message;
    private final @Nullable Function<CommandContext<?>, JsonInput> inputSelector;

    public SnbtJsonArgument(@NotNull ArgumentType<N> nativeType, @NotNull Func1<N, JsonElement> json, @NotNull JsonInput input,
                            @NotNull TypeAdapter<T> adapter, @NotNull Function<Component, Message> message,
                            @Nullable Function<CommandContext<?>, JsonInput> inputSelector) {
        this.nativeType = nativeType;
        this.json = json;
        this.input = input;
        this.adapter = adapter;
        this.message = message;
        this.inputSelector = inputSelector;
    }

    @Override
    public @NotNull ArgumentType<N> nativeType() {
        return nativeType;
    }

    @Override
    public @NotNull T convert(@NotNull N value) throws CommandSyntaxException {
        JsonElement normalized = input.normalize(json.invoke(value));
        try {
            return adapter.fromJsonTree(normalized);
        } catch (JsonParseException | IllegalStateException | NumberFormatException exception) {
            throw new SimpleCommandExceptionType(new LiteralMessage("Invalid JSON value: " + exception.getMessage())).create();
        }
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> suggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        JsonInput selected = inputSelector == null ? input : inputSelector.apply(context);
        JsonInput.View root = selected.root();
        if (root.any())
            return nativeType.listSuggestions(context, builder);
        if (root.none())
            return Suggestions.empty();
        CursorInput cursor = new CursorParser(builder.getRemaining()).parse();
        SuggestionCollector collector = new SuggestionCollector(builder);
        InputCompleter completer = new InputCompleter(selected, collector);
        if (cursor instanceof PropertyInput property) {
            JsonInput.View node = selected.at(property.path(), property.context());
            if (node.any())
                return nativeType.listSuggestions(context, builder.createOffset(builder.getStart() + property.containerStart()));
            if (property.prefix() == null)
                return nativeType.listSuggestions(context, builder.createOffset(builder.getStart() + property.tokenStart()));
            completer.property(property, node);
        } else if (cursor instanceof ValueInput value) {
            JsonInput.View node = selected.at(value.path(), value.context());
            if (node.none())
                return Suggestions.empty();
            if (completer.value(value, node))
                return nativeType.listSuggestions(context, builder.createOffset(builder.getStart() + value.tokenStart()))
                        .thenApply(suggestions -> Suggestions.merge(builder.getInput(), List.of(collector.build(), suggestions)));
        }
        return CompletableFuture.completedFuture(collector.build());
    }

    private final class InputCompleter {
        private final JsonInput input;
        private final SuggestionCollector collector;

        private InputCompleter(JsonInput input, SuggestionCollector collector) {
            this.input = input;
            this.collector = collector;
        }

        private void property(PropertyInput property, JsonInput.View node) {
            char quote = quotation(collector.remaining().substring(property.tokenStart()));
            for (var entry : node.properties().entrySet()) {
                if (property.usedProperties().contains(entry.getKey()) || !startsWithIgnoreCase(entry.getKey(), property.prefix()))
                    continue;
                JsonInput.View child = input.at(append(property.path(), entry.getKey()), property.context());
                collector.addAt(property.tokenStart(), encodeKey(entry.getKey(), quote) + ":", tooltip(child, null));
            }
            if (node.properties().isEmpty() && !property.prefix().isEmpty() && node.additionalPropertiesAllowed())
                collector.addAt(property.tokenStart(), encodeKey(property.prefix(), quote) + ":", message.apply(Component.text("Property name", NamedTextColor.AQUA)));
            if (!property.afterSeparator() && property.prefix().isEmpty() && node.canCloseObject(property.usedProperties()))
                collector.addAt(property.tokenStart(), "}", null);
        }

        /** Returns whether the native parser should also complete this value's syntax. */
        private boolean value(ValueInput value, JsonInput.View node) {
            JsonInput.View container = input.at(parentPath(value.path()), value.context());
            if (value.closing() == ']' && !container.canAddArrayItem(arrayIndex(value.path())))
                return false;
            String error = null;
            if (value.value() != null) {
                try {
                    input.normalizeAndValidate(node, value.path(), value.value());
                } catch (JsonInput.ValidationException exception) {
                    error = exception.getMessage();
                }
                if (error == null) {
                    afterValue(value, container);
                    return false;
                }
            }
            if (node.any())
                return true;
            String raw = collector.remaining().substring(value.tokenStart(), value.tokenEnd());
            char quote = quotation(raw);
            String prefix = raw;
            if (quote != '\0') {
                if (value.value() != null && value.value().isJsonPrimitive() && value.value().getAsJsonPrimitive().isString())
                    prefix = value.value().getAsString();
                else {
                    JsonElement decoded = readQuotedPrefix(raw, quote);
                    if (decoded == null)
                        return true;
                    prefix = decoded.getAsString();
                }
            }
            Map<String, String> candidates = new LinkedHashMap<>();
            for (JsonElement candidate : node.values())
                toSnbt(candidate, quote == '\0' ? '"' : quote).ifPresent(encoded -> candidates.putIfAbsent(logicalValue(candidate), encoded));
            if (node.values().isEmpty())
                for (JsonInput.Type type : node.types())
                    if (type != JsonInput.Type.STRING || quote == '\0')
                        type.suggestions().forEach(candidate -> candidates.putIfAbsent(candidate, candidate));
            String typedPrefix = prefix;
            boolean hasMatch = candidates.keySet().stream().anyMatch(candidate -> startsWithIgnoreCase(candidate, typedPrefix));
            String correction = error != null && !hasMatch ? error : null;
            Message tooltip = tooltip(node, correction);
            String trailing = collector.remaining().substring(value.tokenEnd());
            for (var entry : candidates.entrySet())
                if (correction != null || startsWithIgnoreCase(entry.getKey(), prefix))
                    collector.addAt(value.tokenStart(), entry.getValue() + trailing, tooltip);
            if (quote != '\0' && value.value() == null && node.values().isEmpty() && node.types().contains(JsonInput.Type.STRING))
                collector.addAt(value.tokenEnd(), Character.toString(quote), tooltip);
            if (!value.afterSeparator() && raw.isEmpty() && value.closing() == ']' && arrayIndex(value.path()) == 0 && container.canCloseArray(0))
                collector.addAt(value.tokenStart(), "]", null);
            return value.value() == null && !raw.isEmpty() && quote == '\0' && !hasMatch;
        }

        private void afterValue(ValueInput value, JsonInput.View container) {
            int offset = collector.remaining().length();
            if (value.closing() == '}') {
                boolean canAdd = container.additionalPropertiesAllowed()
                        || container.properties().keySet().stream().anyMatch(property -> !value.usedProperties().contains(property));
                if (canAdd)
                    collector.addAt(offset, ",", null);
                if (container.canCloseObject(value.usedProperties()))
                    collector.addAt(offset, "}", null);
                for (var entry : container.properties().entrySet())
                    if (!value.usedProperties().contains(entry.getKey())) {
                        JsonInput.View child = input.at(append(parentPath(value.path()), entry.getKey()), value.context());
                        collector.addAt(offset, ", " + encodeKey(entry.getKey(), '\0') + ":", tooltip(child, null));
                    }
            } else if (value.closing() == ']') {
                int count = arrayIndex(value.path()) + 1;
                if (container.canAddArrayItem(count))
                    collector.addAt(offset, ",", null);
                if (container.canCloseArray(count))
                    collector.addAt(offset, "]", null);
            }
        }

        private Message tooltip(JsonInput.View node, @Nullable String error) {
            Component component = error == null ? Component.empty() : Component.text(error + "\n", NamedTextColor.RED);
            String types = node.any() ? "SNBT" : String.join(" | ", node.types().stream().map(type -> type.name().toLowerCase(Locale.ROOT)).toList());
            component = component.append(Component.text(types, NamedTextColor.AQUA));
            if (!node.values().isEmpty())
                component = component.append(Component.text("\n" + String.join(", ", node.values().stream().map(SnbtJsonArgument::logicalValue).toList()), NamedTextColor.YELLOW));
            return message.apply(component);
        }
    }

    private @Nullable JsonElement readQuotedPrefix(String raw, char quote) {
        N parsed;
        try {
            parsed = nativeType.parse(new StringReader(raw + quote));
        } catch (CommandSyntaxException exception) {
            return null;
        }
        return json.invoke(parsed);
    }

    private static int arrayIndex(List<Object> path) {
        return (Integer)path.get(path.size() - 1);
    }

    private static List<Object> parentPath(List<Object> path) {
        return path.isEmpty() ? List.of() : path.subList(0, path.size() - 1);
    }

    private static List<Object> append(List<Object> path, Object value) {
        List<Object> result = new ArrayList<>(path.size() + 1);
        result.addAll(path);
        result.add(value);
        return result;
    }

    private static char quotation(String value) {
        return !value.isEmpty() && (value.charAt(0) == '"' || value.charAt(0) == '\'') ? value.charAt(0) : '\0';
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return prefix.isEmpty() || value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static String logicalValue(JsonElement value) {
        return value.isJsonPrimitive() ? value.getAsJsonPrimitive().getAsString() : value.toString();
    }

    private static Optional<String> toSnbt(JsonElement value, char quote) {
        if (value.isJsonNull())
            return Optional.empty();
        if (value.isJsonPrimitive())
            return Optional.of(value.getAsJsonPrimitive().isString() ? quote(value.getAsString(), quote) : value.toString());
        List<String> entries = new ArrayList<>();
        if (value.isJsonArray()) {
            for (JsonElement item : value.getAsJsonArray()) {
                Optional<String> encoded = toSnbt(item, quote);
                if (encoded.isEmpty())
                    return Optional.empty();
                entries.add(encoded.get());
            }
            return Optional.of("[" + String.join(",", entries) + "]");
        }
        for (var entry : value.getAsJsonObject().entrySet()) {
            Optional<String> encoded = toSnbt(entry.getValue(), quote);
            if (encoded.isEmpty())
                return Optional.empty();
            entries.add(encodeKey(entry.getKey(), '\0') + ":" + encoded.get());
        }
        return Optional.of("{" + String.join(",", entries) + "}");
    }

    private static String encodeKey(String value, char quote) {
        return quote == '\0' && !value.isEmpty() && value.chars().allMatch(character -> StringReader.isAllowedInUnquotedString((char)character))
                ? value : quote(value, quote == '\0' ? '"' : quote);
    }

    private static String quote(String value, char quote) {
        StringBuilder result = new StringBuilder(value.length() + 2).append(quote);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == quote || current == '\\')
                result.append('\\').append(current);
            else switch (current) {
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (current < 0x20)
                        result.append(String.format("\\u%04x", (int)current));
                    else
                        result.append(current);
                }
            }
        }
        return result.append(quote).toString();
    }

    private sealed interface CursorInput permits ValueInput, PropertyInput, NoInput {
    }

    private record ValueInput(List<Object> path, int tokenStart, int tokenEnd, @Nullable JsonElement value,
                              Set<String> usedProperties, char closing, boolean afterSeparator, JsonElement context) implements CursorInput {
    }

    private record PropertyInput(List<Object> path, int containerStart, int tokenStart, @Nullable String prefix, Set<String> usedProperties,
                                 boolean afterSeparator, JsonElement context) implements CursorInput {
    }

    private enum NoInput implements CursorInput {
        INSTANCE,
        ;
    }

    private static final class SuggestionCollector {
        private final SuggestionsBuilder root;
        private final Map<Integer, SuggestionsBuilder> builders = new LinkedHashMap<>();

        private SuggestionCollector(SuggestionsBuilder root) {
            this.root = root;
        }

        private String remaining() {
            return root.getRemaining();
        }

        private void addAt(int offset, String value, @Nullable Message tooltip) {
            builders.computeIfAbsent(root.getStart() + offset, root::createOffset).suggest(value, tooltip);
        }

        private Suggestions build() {
            return Suggestions.merge(root.getInput(), builders.values().stream().map(SuggestionsBuilder::build).toList());
        }
    }

    private final class CursorParser {
        private final String input;
        private JsonElement context;
        private int cursor;

        private CursorParser(String input) {
            this.input = input;
        }

        private CursorInput parse() {
            skipWhitespace();
            context = !end() && peek() == '[' ? new JsonArray() : new JsonObject();
            CursorInput result = parseValue(List.of(), Set.of(), '\0', false);
            return result == null ? NoInput.INSTANCE : result;
        }

        private @Nullable CursorInput parseValue(List<Object> path, Set<String> used, char closing, boolean afterSeparator) {
            skipWhitespace();
            int start = cursor;
            if (end())
                return new ValueInput(path, start, start, null, used, closing, afterSeparator, context);
            if ((peek() == '{' || peek() == '[') && !containerClosed(start))
                return peek() == '{' ? parseObject(path) : parseArray(path);
            StringReader reader = new StringReader(input);
            reader.setCursor(start);
            N parsed;
            try {
                parsed = nativeType.parse(reader);
            } catch (CommandSyntaxException exception) {
                if (peek() == '{')
                    return parseObject(path);
                if (peek() == '[')
                    return parseArray(path);
                if (peek() == '}' || peek() == ']' || peek() == ',')
                    return NoInput.INSTANCE;
                return new ValueInput(path, start, input.length(), null, used, closing, afterSeparator, context);
            }
            JsonElement value = json.invoke(parsed);
            cursor = reader.getCursor();
            int tokenEnd = cursor;
            if (!path.isEmpty())
                set(context, path, 0, value);
            skipWhitespace();
            return end() ? new ValueInput(path, start, tokenEnd, value, used, closing, afterSeparator, context) : null;
        }

        /** Finds a boundary only; the native parser still validates every completed value. */
        private boolean containerClosed(int start) {
            int depth = 0;
            char quote = '\0';
            for (int i = start; i < input.length(); i++) {
                char current = input.charAt(i);
                if (quote != '\0') {
                    if (current == '\\')
                        i++;
                    else if (current == quote)
                        quote = '\0';
                } else if (current == '"' || current == '\'')
                    quote = current;
                else if (current == '{' || current == '[')
                    depth++;
                else if ((current == '}' || current == ']') && --depth == 0)
                    return true;
            }
            return false;
        }

        private @Nullable CursorInput parseObject(List<Object> path) {
            if (!path.isEmpty())
                set(context, path, 0, new JsonObject());
            int containerStart = cursor++;
            Set<String> used = new LinkedHashSet<>();
            boolean afterSeparator = false;
            while (true) {
                skipWhitespace();
                if (end())
                    return new PropertyInput(path, containerStart, cursor, "", used, afterSeparator, context);
                if (peek() == '}') {
                    cursor++;
                    return null;
                }
                int start = cursor;
                String key;
                if (quotation(input.substring(cursor)) != '\0') {
                    char quote = input.charAt(cursor++);
                    boolean escaped = false;
                    boolean closed = false;
                    while (!end()) {
                        char current = input.charAt(cursor++);
                        if (escaped)
                            escaped = false;
                        else if (current == '\\')
                            escaped = true;
                        else if (current == quote) {
                            closed = true;
                            break;
                        }
                    }
                    String raw = input.substring(start, cursor);
                    JsonElement decoded = readQuotedPrefix(closed ? raw.substring(0, raw.length() - 1) : raw, quote);
                    if (!closed || end())
                        return new PropertyInput(path, containerStart, start, decoded == null ? null : decoded.getAsString(), used, afterSeparator, context);
                    if (decoded == null)
                        return NoInput.INSTANCE;
                    key = decoded.getAsString();
                } else {
                    while (!end() && !Character.isWhitespace(peek()) && peek() != ':' && peek() != ',' && peek() != '}' && peek() != ']')
                        cursor++;
                    key = input.substring(start, cursor);
                    if (key.isEmpty() || !key.chars().allMatch(character -> StringReader.isAllowedInUnquotedString((char)character)))
                        return NoInput.INSTANCE;
                    if (end())
                        return new PropertyInput(path, containerStart, start, key, used, afterSeparator, context);
                }
                skipWhitespace();
                if (end())
                    return new PropertyInput(path, containerStart, start, key, used, afterSeparator, context);
                if (peek() != ':')
                    return NoInput.INSTANCE;
                cursor++;
                used.add(key);
                List<Object> childPath = append(path, key);
                set(context, childPath, 0, JsonNull.INSTANCE);
                CursorInput result = parseValue(childPath, used, '}', false);
                if (result != null)
                    return result;
                if (end())
                    return NoInput.INSTANCE;
                if (peek() == '}') {
                    cursor++;
                    return null;
                }
                if (peek() != ',')
                    return NoInput.INSTANCE;
                cursor++;
                afterSeparator = true;
            }
        }

        private @Nullable CursorInput parseArray(List<Object> path) {
            if (!path.isEmpty())
                set(context, path, 0, new JsonArray());
            cursor++;
            skipWhitespace();
            if (!end() && (peek() == 'B' || peek() == 'I' || peek() == 'L') && cursor + 1 < input.length() && input.charAt(cursor + 1) == ';')
                cursor += 2;
            int index = 0;
            while (true) {
                skipWhitespace();
                if (!end() && peek() == ']') {
                    cursor++;
                    return null;
                }
                CursorInput result = parseValue(append(path, index), Set.of(), ']', index > 0);
                if (result != null)
                    return result;
                if (end())
                    return NoInput.INSTANCE;
                if (peek() == ']') {
                    cursor++;
                    return null;
                }
                if (peek() != ',')
                    return NoInput.INSTANCE;
                cursor++;
                index++;
            }
        }

        private void skipWhitespace() {
            while (!end() && Character.isWhitespace(peek()))
                cursor++;
        }

        private boolean end() {
            return cursor >= input.length();
        }

        private char peek() {
            return input.charAt(cursor);
        }

        private void set(JsonElement parent, List<Object> path, int depth, JsonElement value) {
            Object part = path.get(depth);
            if (depth == path.size() - 1) {
                if (part instanceof String key)
                    parent.getAsJsonObject().add(key, value);
                else {
                    JsonArray array = parent.getAsJsonArray();
                    while (array.size() <= (Integer)part)
                        array.add(JsonNull.INSTANCE);
                    array.set((Integer)part, value);
                }
                return;
            }
            Object next = path.get(depth + 1);
            JsonElement child = part instanceof String key && parent.isJsonObject() ? parent.getAsJsonObject().get(key)
                    : part instanceof Integer index && parent.isJsonArray() && index < parent.getAsJsonArray().size() ? parent.getAsJsonArray().get(index) : null;
            if (child == null || child.isJsonNull())
                child = next instanceof String ? new JsonObject() : new JsonArray();
            if (part instanceof String key)
                parent.getAsJsonObject().add(key, child);
            else {
                JsonArray array = parent.getAsJsonArray();
                while (array.size() <= (Integer)part)
                    array.add(JsonNull.INSTANCE);
                array.set((Integer)part, child);
            }
            set(child, path, depth + 1, value);
        }
    }
}
