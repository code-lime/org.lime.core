package org.lime.core.common.api.commands.brigadier.arguments;

import com.google.gson.*;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.*;
import com.mojang.brigadier.suggestion.*;
import org.jetbrains.annotations.*;
import org.lime.core.common.utils.execute.Func1;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Maps one vanilla-parsed SNBT value to Gson and provides typed completion. */
public final class SnbtJsonArgument<T, N> implements BaseMappedArgument<T, N> {
    private final ArgumentType<N> nativeType;
    private final Func1<N, JsonElement> json;
    private final JsonInput input;
    private final TypeAdapter<T> adapter;

    public SnbtJsonArgument(@NotNull ArgumentType<N> nativeType, @NotNull Func1<N, JsonElement> json, @NotNull JsonInput input, @NotNull TypeAdapter<T> adapter) {
        this.nativeType = nativeType;
        this.json = json;
        this.input = input;
        this.adapter = adapter;
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
            throw syntax("Invalid JSON value: " + exception.getMessage());
        }
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> suggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        if (input.root().any())
            return nativeType.listSuggestions(context, new SuggestionsBuilder(builder.getInput(), builder.getStart()));
        if (input.root().none())
            return Suggestions.empty();
        CursorInput cursor = new CursorParser(builder.getRemaining()).parse();
        if (cursor instanceof ValueInput value && input.at(value.path()).any())
            return nativeType.listSuggestions(context, new SuggestionsBuilder(builder.getInput(), builder.getStart()));
        SuggestionCollector collector = new SuggestionCollector(builder);
        cursor.complete(new InputCompleter(collector));
        return CompletableFuture.completedFuture(collector.build());
    }

    private final class InputCompleter implements Completer {
        private final SuggestionCollector collector;

        private InputCompleter(SuggestionCollector collector) {
            this.collector = collector;
        }

        @Override
        public void value(ValueInput input) {
            if (input.suffix()) {
                if (input.closing() == ']' && !canAddArrayItem(input.path()))
                    return;
                if (valueComplete(input))
                    addAfterValueSuggestions(collector, new SuffixInput(input.path(), input.tokenStart() + input.rawPrefix().length(), input.usedProperties(), input.closing(), false));
                else
                    addValueSuggestions(collector, input, false);
                return;
            }

            if (input.closing() != ']' || canAddArrayItem(input.path()))
                addValueSuggestions(collector, input, input.closing() == '\0');
            if (!input.afterSeparator() && input.closing() == ']' && input.prefix().isEmpty()
                    && input.path().get(input.path().size() - 1).equals(0)
                    && SnbtJsonArgument.this.input.at(parentPath(input.path())).canCloseArray(0))
                collector.addAt(input.tokenStart(), "]", null);
        }

        @Override
        public void property(PropertyInput input) {
            addObjectKeySuggestions(collector, input);
            if (!input.afterSeparator() && input.prefix().isEmpty()
                    && SnbtJsonArgument.this.input.at(input.path()).canCloseObject(input.usedProperties()))
                collector.addAt(input.tokenStart(), "}", null);
        }

        @Override
        public void suffix(SuffixInput input) {
            if (input.colon())
                collector.addAt(input.tokenStart(), ":", null);
            else
                addAfterValueSuggestions(collector, input);
        }
    }

    private void addObjectKeySuggestions(SuggestionCollector collector, PropertyInput input) {
        JsonInput.View node = this.input.at(input.path());
        Map<String, JsonInput.Node> properties = node.properties();
        boolean quoted = quoted(input.rawPrefix());
        JsonInput.Node exact = properties.get(input.prefix());
        if (exact != null && !input.usedProperties().contains(input.prefix())) {
            collector.addAt(input.tokenStart() + input.rawPrefix().length(), quoted ? "\"" : ":", null);
            return;
        }
        for (var entry : properties.entrySet()) {
            if (input.usedProperties().contains(entry.getKey()) || !startsWithIgnoreCase(entry.getKey(), input.prefix()))
                continue;
            collector.addAt(input.tokenStart(), quoted ? quote(entry.getKey()) : encodeKey(entry.getKey()), null);
        }

        if (properties.isEmpty() && !input.prefix().isEmpty() && node.additionalPropertiesAllowed())
            collector.addAt(input.tokenStart() + input.rawPrefix().length(), quoted ? "\"" : ":", "Property name");
    }

    private void addValueSuggestions(SuggestionCollector collector, ValueInput input, boolean root) {
        JsonInput.View node = this.input.at(input.path());
        Map<String, String> values = new LinkedHashMap<>();
        node.values().forEach(value -> addValue(values, value));

        boolean constrainedValues = !values.isEmpty();
        if (!constrainedValues) {
            for (JsonInput.Type type : node.types())
                if (type != JsonInput.Type.STRING || !quoted(input.rawPrefix()))
                    type.suggestions().forEach(value -> values.putIfAbsent(value, value));
        }

        for (var entry : values.entrySet()) {
            if (startsWithIgnoreCase(entry.getKey(), input.prefix()) || startsWithIgnoreCase(entry.getValue(), input.rawPrefix()))
                collector.addAt(input.tokenStart(), entry.getValue(), null);
        }

        if (quoted(input.rawPrefix()) && !constrainedValues && node.types().contains(JsonInput.Type.STRING))
            collector.addAt(input.tokenStart() + input.rawPrefix().length(), "\"", null);

        if (root && valueComplete(input))
            collector.clear();
    }

    private boolean valueComplete(ValueInput input) {
        if (input.rawPrefix().isEmpty() || quoted(input.rawPrefix()))
            return false;

        JsonInput.View node = this.input.at(input.path());
        if (!node.values().isEmpty())
            return node.values().stream().map(this::logicalValue).anyMatch(input.prefix()::equals);
        return node.types().stream().anyMatch(type -> type.complete(input.prefix()));
    }

    private void addAfterValueSuggestions(SuggestionCollector collector, SuffixInput input) {
        List<Object> containerPath = parentPath(input.path());
        JsonInput.View container = this.input.at(containerPath);
        if (input.closing() == '}') {
            Map<String, JsonInput.Node> properties = container.properties();
            boolean hasUnusedProperty = properties.keySet().stream().anyMatch(property -> !input.usedProperties().contains(property));
            boolean canAddProperty = hasUnusedProperty || container.additionalPropertiesAllowed();
            if (canAddProperty)
                collector.addAt(input.tokenStart(), ",", null);
            if (container.canCloseObject(input.usedProperties()))
                collector.addAt(input.tokenStart(), "}", null);

            if (canAddProperty)
                for (var entry : properties.entrySet())
                    if (!input.usedProperties().contains(entry.getKey()))
                        collector.addAt(input.tokenStart(), ", " + encodeKey(entry.getKey()), null);
        } else if (input.closing() == ']') {
            int count = arrayCount(input.path());
            if (container.canAddArrayItem(count))
                collector.addAt(input.tokenStart(), ",", null);
            if (container.canCloseArray(count))
                collector.addAt(input.tokenStart(), "]", null);
        }
    }

    private int arrayCount(List<Object> path) {
        if (path.isEmpty() || !(path.get(path.size() - 1) instanceof Integer index))
            return 1;
        return index + 1;
    }

    private boolean canAddArrayItem(List<Object> path) {
        if (path.isEmpty() || !(path.get(path.size() - 1) instanceof Integer index))
            return true;
        return input.at(parentPath(path)).canAddArrayItem(index);
    }

    private void addValue(Map<String, String> values, JsonElement value) {
        toSnbt(value).ifPresent(snbt -> values.putIfAbsent(logicalValue(value), snbt));
    }

    private String logicalValue(JsonElement value) {
        return value.isJsonPrimitive() ? value.getAsJsonPrimitive().getAsString() : value.toString();
    }

    private Optional<String> toSnbt(JsonElement value) {
        if (value.isJsonNull())
            return Optional.empty();
        if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            return Optional.of(primitive.isString() ? quote(primitive.getAsString()) : primitive.toString());
        }
        if (value.isJsonArray()) {
            List<String> items = new ArrayList<>();
            for (JsonElement item : value.getAsJsonArray()) {
                Optional<String> encoded = toSnbt(item);
                if (encoded.isEmpty())
                    return Optional.empty();
                items.add(encoded.get());
            }
            return Optional.of("[" + String.join(",", items) + "]");
        }

        List<String> entries = new ArrayList<>();
        for (var entry : value.getAsJsonObject().entrySet()) {
            Optional<String> encoded = toSnbt(entry.getValue());
            if (encoded.isEmpty())
                return Optional.empty();
            entries.add(encodeKey(entry.getKey()) + ":" + encoded.get());
        }
        return Optional.of("{" + String.join(",", entries) + "}");
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

    private static boolean quoted(String value) {
        return !value.isEmpty() && (value.charAt(0) == '"' || value.charAt(0) == '\'');
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return prefix.isEmpty() || value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static String encodeKey(String value) {
        return !value.isEmpty() && value.chars().allMatch(SnbtJsonArgument::isSafeKeyCharacter) ? value : quote(value);
    }

    private static boolean isSafeKeyCharacter(int value) {
        return value >= '0' && value <= '9' || value >= 'A' && value <= 'Z' || value >= 'a' && value <= 'z'
                || value == '_' || value == '-' || value == '.' || value == '+';
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '\\' -> result.append("\\\\");
                case '"' -> result.append("\\\"");
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
        return result.append('"').toString();
    }

    private static CommandSyntaxException syntax(String message) {
        return new SimpleCommandExceptionType(new LiteralMessage(message)).create();
    }

    private sealed interface CursorInput permits ValueInput, PropertyInput, SuffixInput, NoInput {
        void complete(Completer completer);
    }

    private record ValueInput(List<Object> path, int tokenStart, String prefix, String rawPrefix, Set<String> usedProperties, char closing, boolean afterSeparator, boolean suffix) implements CursorInput {
        @Override
        public void complete(Completer completer) {
            completer.value(this);
        }
    }

    private record PropertyInput(List<Object> path, int tokenStart, String prefix, String rawPrefix, Set<String> usedProperties, boolean afterSeparator) implements CursorInput {
        @Override
        public void complete(Completer completer) {
            completer.property(this);
        }
    }

    private record SuffixInput(List<Object> path, int tokenStart, Set<String> usedProperties, char closing, boolean colon) implements CursorInput {
        @Override
        public void complete(Completer completer) {
            completer.suffix(this);
        }
    }

    private enum NoInput implements CursorInput {
        INSTANCE;

        @Override
        public void complete(Completer completer) {
        }
    }

    private interface Completer {
        void value(ValueInput input);

        void property(PropertyInput input);

        void suffix(SuffixInput input);
    }

    private static final class SuggestionCollector {
        private final SuggestionsBuilder root;
        private final Set<String> keys = new HashSet<>();
        private final Map<Integer, SuggestionsBuilder> builders = new LinkedHashMap<>();

        private SuggestionCollector(SuggestionsBuilder root) {
            this.root = root;
        }

        private void addAt(int localOffset, String value, @Nullable String tooltip) {
            if (value.isEmpty())
                return;
            int absoluteOffset = root.getStart() + Math.max(0, localOffset);
            if (!keys.add(absoluteOffset + "\u0000" + value))
                return;

            SuggestionsBuilder target = builders.computeIfAbsent(absoluteOffset, root::createOffset);
            if (tooltip == null || tooltip.isBlank())
                target.suggest(value);
            else
                target.suggest(value, new LiteralMessage(tooltip));
        }

        private Suggestions build() {
            if (builders.isEmpty())
                return new SuggestionsBuilder(root.getInput(), root.getStart()).build();
            return Suggestions.merge(root.getInput(), builders.values().stream().map(SuggestionsBuilder::build).toList());
        }

        private void clear() {
            keys.clear();
            builders.clear();
        }
    }

    private static final class CursorParser {
        private final String input;
        private int cursor;

        private CursorParser(String input) {
            this.input = input;
        }

        private CursorInput parse() {
            skipWhitespace();
            if (end())
                return value(List.of(), cursor, "", "", Set.of(), '\0', false, false);

            CursorInput result = parseValue(new ArrayList<>(), Set.of(), '\0');
            if (result != null)
                return result;
            skipWhitespace();
            return NoInput.INSTANCE;
        }

        private @Nullable CursorInput parseValue(List<Object> path, Set<String> usedProperties, char closing) {
            skipWhitespace();
            if (end())
                return value(path, cursor, "", "", usedProperties, closing, false, false);

            char current = peek();
            if (current == '{')
                return parseObject(path);
            if (current == '[')
                return parseArray(path);
            if (current == '"' || current == '\'') {
                int start = cursor;
                StringToken token = readQuoted();
                if (!token.valid())
                    return NoInput.INSTANCE;
                return token.complete() ? null : value(path, start, token.value(), input.substring(start), usedProperties, closing, false, false);
            }
            if (current == '}' || current == ']' || current == ',')
                return NoInput.INSTANCE;

            int start = cursor;
            while (!end() && !isValueDelimiter(peek()))
                cursor++;
            String token = input.substring(start, cursor);
            if (token.isEmpty())
                return NoInput.INSTANCE;
            if (end())
                return value(path, start, token, token, usedProperties, closing, false, closing != '\0');
            return null;
        }

        private @Nullable CursorInput parseObject(List<Object> path) {
            cursor++;
            Set<String> usedProperties = new LinkedHashSet<>();
            skipWhitespace();
            if (end())
                return property(path, cursor, "", "", usedProperties, false);
            if (peek() == '}') {
                cursor++;
                return null;
            }

            while (true) {
                skipWhitespace();
                if (end())
                    return property(path, cursor, "", "", usedProperties, false);

                int keyStart = cursor;
                String key;
                if (peek() == '"' || peek() == '\'') {
                    StringToken token = readQuoted();
                    if (!token.valid())
                        return NoInput.INSTANCE;
                    if (!token.complete())
                        return property(path, keyStart, token.value(), input.substring(keyStart), usedProperties, false);
                    key = token.value();
                } else {
                    while (!end() && !isKeyDelimiter(peek()))
                        cursor++;
                    key = input.substring(keyStart, cursor);
                    if (key.isEmpty())
                        return NoInput.INSTANCE;
                    if (end())
                        return property(path, keyStart, key, key, usedProperties, false);
                }

                usedProperties.add(key);
                skipWhitespace();
                if (end())
                    return suffix(append(path, key), cursor, usedProperties, '}', true);
                if (peek() != ':')
                    return NoInput.INSTANCE;
                cursor++;

                List<Object> childPath = append(path, key);
                skipWhitespace();
                if (end())
                    return value(childPath, cursor, "", "", usedProperties, '}', false, false);
                CursorInput result = parseValue(childPath, usedProperties, '}');
                if (result != null)
                    return result;

                skipWhitespace();
                if (end())
                    return suffix(childPath, cursor, usedProperties, '}', false);
                if (peek() == '}') {
                    cursor++;
                    return null;
                }
                if (peek() != ',')
                    return NoInput.INSTANCE;
                cursor++;
                skipWhitespace();
                if (end())
                    return property(path, cursor, "", "", usedProperties, true);
            }
        }

        private @Nullable CursorInput parseArray(List<Object> path) {
            cursor++;
            skipWhitespace();
            if (!end() && (peek() == 'B' || peek() == 'I' || peek() == 'L') && cursor + 1 < input.length() && input.charAt(cursor + 1) == ';') {
                cursor += 2;
                skipWhitespace();
            }

            int index = 0;
            if (end())
                return value(append(path, index), cursor, "", "", Set.of(), ']', false, false);
            if (peek() == ']') {
                cursor++;
                return null;
            }

            while (true) {
                CursorInput result = parseValue(append(path, index), Set.of(), ']');
                if (result != null)
                    return result;
                skipWhitespace();
                if (end())
                    return suffix(append(path, index), cursor, Set.of(), ']', false);
                if (peek() == ']') {
                    cursor++;
                    return null;
                }
                if (peek() != ',')
                    return NoInput.INSTANCE;
                cursor++;
                index++;
                skipWhitespace();
                if (end())
                    return value(append(path, index), cursor, "", "", Set.of(), ']', true, false);
            }
        }

        private StringToken readQuoted() {
            char quote = input.charAt(cursor++);
            StringBuilder value = new StringBuilder();
            boolean escaped = false;
            while (!end()) {
                char current = input.charAt(cursor++);
                if (escaped) {
                    value.append(switch (current) {
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> current;
                    });
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    return new StringToken(value.toString(), true, true);
                } else {
                    value.append(current);
                }
            }
            return new StringToken(value.toString(), false, !escaped);
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

        private static boolean isValueDelimiter(char value) {
            return Character.isWhitespace(value) || value == ',' || value == ']' || value == '}';
        }

        private static boolean isKeyDelimiter(char value) {
            return Character.isWhitespace(value) || value == ':' || value == ',' || value == '}';
        }

        private static List<Object> append(List<Object> path, Object value) {
            List<Object> result = new ArrayList<>(path.size() + 1);
            result.addAll(path);
            result.add(value);
            return result;
        }

        private static ValueInput value(List<Object> path, int tokenStart, String prefix, String rawPrefix, Set<String> usedProperties, char closing, boolean afterSeparator, boolean suffix) {
            return new ValueInput(path, tokenStart, prefix, rawPrefix, usedProperties, closing, afterSeparator, suffix);
        }

        private static PropertyInput property(List<Object> path, int tokenStart, String prefix, String rawPrefix, Set<String> usedProperties, boolean afterSeparator) {
            return new PropertyInput(path, tokenStart, prefix, rawPrefix, usedProperties, afterSeparator);
        }

        private static SuffixInput suffix(List<Object> path, int tokenStart, Set<String> usedProperties, char closing, boolean colon) {
            return new SuffixInput(path, tokenStart, usedProperties, closing, colon);
        }

        private record StringToken(String value, boolean complete, boolean valid) {
        }
    }
}
