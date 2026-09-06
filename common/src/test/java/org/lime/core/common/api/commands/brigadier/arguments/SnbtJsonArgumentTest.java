package org.lime.core.common.api.commands.brigadier.arguments;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.*;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.*;
import com.mojang.brigadier.exceptions.*;
import com.mojang.brigadier.suggestion.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class SnbtJsonArgumentTest {
    private static final Gson GSON = new GsonBuilder().enableComplexMapKeySerialization().serializeNulls().create();
    private static final Object SOURCE = new Object();

    @Test
    void convertsRawAndDeepTypedValues() throws CommandSyntaxException {
        SnbtJsonArgument<JsonElement, JsonElement> raw = raw();
        for (String value : List.of("{\"value\":1}", "[1,2]", "\"text\"", "4")) {
            JsonElement json = JsonParser.parseString(value);
            assertEquals(json, raw.convert(json));
        }

        DeepObject object = typed(TypeToken.get(DeepObject.class)).convert(JsonParser.parseString("""
                {
                  "id":"root",
                  "value":{"name":"value","count":3,"mode":"FORCE","values":["first"],"attributes":{"SAFE":[1,2]}},
                  "values":[],"modes":{},"groups":{},"nested":[]
                }
                """));
        assertEquals("root", object.id());
        assertEquals(DeepMode.FORCE, object.value().mode());
        assertEquals(List.of(1, 2), object.value().attributes().get(DeepMode.SAFE));

        TypeToken<List<Map<DeepMode, Map<String, List<DeepClass>>>>> complexType = new TypeToken<>() {};
        var complex = typed(complexType).convert(JsonParser.parseString("""
                [{"FORCE":{"group":[{"id":"nested","children":[{}]}]}}]
                """));
        DeepClass nested = complex.get(0).get(DeepMode.FORCE).get("group").get(0);
        assertEquals("nested", nested.id);
        assertEquals("default-class", nested.children.get(0).id);
    }

    @Test
    void preservesClassDefaultsAndNormalizesTypedBooleans() throws CommandSyntaxException {
        SnbtJsonArgument<DeepClass, JsonElement> argument = typed(TypeToken.get(DeepClass.class));
        DeepClass defaults = argument.convert(new JsonObject());
        assertEquals("default-class", defaults.id);
        assertEquals(42, defaults.count);
        assertTrue(defaults.enabled);
        assertEquals(DeepMode.SAFE, defaults.mode);
        assertEquals("default-object", defaults.object.id());
        assertFalse(defaults.objects.isEmpty());
        assertTrue(defaults.modes.containsKey(DeepMode.SAFE));
        assertTrue(defaults.groups.containsKey(DeepMode.SAFE));
        assertTrue(defaults.children.isEmpty());

        DeepClass partial = argument.convert(JsonParser.parseString("{\"id\":\"custom\",\"children\":[{}]}"));
        assertEquals("custom", partial.id);
        assertEquals(42, partial.count);
        assertEquals("default-class", partial.children.get(0).id);

        JsonObject value = JsonParser.parseString("{\"enabled\":1}").getAsJsonObject();
        assertTrue(typed(TypeToken.get(Flags.class)).convert(value).enabled());
        assertTrue(value.get("enabled").getAsBoolean());
        JsonObject raw = JsonParser.parseString("{\"enabled\":1}").getAsJsonObject();
        assertSame(raw, raw().convert(raw));
        assertTrue(raw.get("enabled").getAsJsonPrimitive().isNumber());
    }

    @Test
    void reportsGsonFailureAsCommandSyntax() {
        CommandSyntaxException exception = assertThrows(CommandSyntaxException.class,
                () -> typed(TypeToken.get(Integer.class)).convert(new JsonObject()));
        assertTrue(exception.getMessage().contains("Invalid JSON value"));
    }

    @Test
    void usesNativeSuggestionsOnlyForOpaqueNodes() {
        ArgumentType<JsonElement> nativeType = new JsonNativeType() {
            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                return builder.suggest("native").buildFuture();
            }
        };
        assertEquals(Set.of("native"), texts(raw(nativeType), ""));

        JsonInput.Node mode = JsonInput.scalar(JsonInput.Type.STRING, List.of(new JsonPrimitive("SAFE")));
        JsonInput input = JsonInput.of(JsonInput.object(Map.of("raw", JsonInput.any(), "mode", mode)));
        SnbtJsonArgument<JsonElement, JsonElement> nested = argument(input, GSON.getAdapter(JsonElement.class), nativeType);
        assertEquals(Set.of("native"), texts(nested, "{raw:"));
        assertEquals(Set.of("\"SAFE\""), texts(nested, "{mode:"));

        RuntimeException failure = new RuntimeException("native failure");
        ArgumentType<JsonElement> failing = new JsonNativeType() {
            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                return CompletableFuture.failedFuture(failure);
            }
        };
        assertSame(failure, assertThrows(CompletionException.class, () -> suggestions(raw(failing), "")).getCause());
        assertTrue(texts(argument(JsonInput.of(JsonInput.none()), GSON.getAdapter(JsonElement.class), nativeType), "").isEmpty());
    }

    @Test
    void infersEnumObjectsMapsAndRecursiveTypes() {
        SnbtJsonArgument<DeepMode, JsonElement> mode = typed(TypeToken.get(DeepMode.class));
        assertSuggestions(mode, "", Map.of("\"SAFE\"", new StringRange(0, 0), "\"FORCE\"", new StringRange(0, 0)));
        assertSuggestions(mode, "S", Map.of("\"SAFE\"", new StringRange(0, 1)));
        assertSuggestions(mode, "\"SA", Map.of("\"SAFE\"", new StringRange(0, 3)));
        assertTrue(texts(mode, "SAFE").isEmpty());
        assertTrue(texts(mode, "UNKNOWN").isEmpty());

        SnbtJsonArgument<DeepClass, JsonElement> object = typed(TypeToken.get(DeepClass.class));
        assertTrue(texts(object, "{").containsAll(List.of("id", "count", "enabled", "mode", "children", "}")));
        assertSuggestions(object, "{mo", Map.of("mode", new StringRange(1, 3), "modes", new StringRange(1, 3)));
        assertSuggestions(object, "{mode", Map.of(":", new StringRange(5, 5)));
        assertEquals(Set.of("\"SAFE\"", "\"FORCE\""), texts(object, "{mode:"));
        assertTrue(texts(object, "{children:[{").contains("children"));

        SnbtJsonArgument<Map<DeepMode, DeepObject>, JsonElement> map = typed(new TypeToken<>() {});
        assertEquals(Set.of("SAFE", "FORCE", "}"), texts(map, "{"));
        assertSuggestions(map, "{SA", Map.of("SAFE", new StringRange(1, 3)));
    }

    @Test
    void completesEveryCursorStateWithOneRange() {
        SnbtJsonArgument<DeepClass, JsonElement> object = typed(TypeToken.get(DeepClass.class));
        assertState(object, "{\"mo", Set.of("\"mode\"", "\"modes\""), new StringRange(1, 4));
        assertState(object, "{mode", Set.of(":"), new StringRange(5, 5));
        assertState(object, "{\"mode\"", Set.of(":"), new StringRange(7, 7));
        assertState(object, "{mode:", Set.of("\"SAFE\"", "\"FORCE\""), new StringRange(6, 6));
        assertState(object, "{mode:S", Set.of("\"SAFE\""), new StringRange(6, 7));
        assertFalse(texts(object, "{mode:SAFE,").contains("}"));

        SnbtJsonArgument<List<DeepMode>, JsonElement> list = typed(new TypeToken<>() {});
        assertState(list, "[", Set.of("\"SAFE\"", "\"FORCE\"", "]"), new StringRange(1, 1));
        assertState(list, "[S", Set.of("\"SAFE\""), new StringRange(1, 2));
        assertState(list, "[SAFE", Set.of(",", "]"), new StringRange(5, 5));
        assertState(list, "[SAFE,", Set.of("\"SAFE\"", "\"FORCE\""), new StringRange(6, 6));

        SnbtJsonArgument<Map<String, String>, JsonElement> arbitrary = typed(new TypeToken<>() {});
        assertState(arbitrary, "{", Set.of("}"), new StringRange(1, 1));
        assertState(arbitrary, "{abc", Set.of(":"), new StringRange(4, 4));
        assertState(arbitrary, "{\"abc", Set.of("\""), new StringRange(5, 5));
        assertState(arbitrary, "{abc:\"value", Set.of("\""), new StringRange(11, 11));

        for (String invalid : List.of("{]", "[}", "{mode=", "{mode:\"bad\\"))
            assertTrue(texts(object, invalid).isEmpty(), invalid);
    }

    @Test
    void appliesPropertyColonAndValueAsSeparateTabs() {
        SnbtJsonArgument<DeepClass, JsonElement> argument = typed(TypeToken.get(DeepClass.class));
        String prefix = "/test.json class ";
        String input = prefix + "{mod";

        input = suggestion(argument, input, prefix.length(), "mode").apply(input);
        assertEquals(prefix + "{mode", input);
        input = suggestion(argument, input, prefix.length(), ":").apply(input);
        assertEquals(prefix + "{mode:", input);
        input = suggestion(argument, input, prefix.length(), "\"SAFE\"").apply(input);
        assertEquals(prefix + "{mode:\"SAFE\"", input);

        input = prefix + "{\"mod";
        input = suggestion(argument, input, prefix.length(), "\"mode\"").apply(input);
        assertEquals(prefix + "{\"mode\"", input);
        assertEquals(":", suggestion(argument, input, prefix.length(), ":").getText());
    }

    private static SnbtJsonArgument<JsonElement, JsonElement> raw() {
        return raw(new JsonNativeType());
    }

    private static SnbtJsonArgument<JsonElement, JsonElement> raw(ArgumentType<JsonElement> nativeType) {
        return argument(JsonInput.raw(), GSON.getAdapter(JsonElement.class), nativeType);
    }

    private static <T> SnbtJsonArgument<T, JsonElement> typed(TypeToken<T> type) {
        return argument(JsonInput.of(GSON, type), GSON.getAdapter(type), new JsonNativeType());
    }

    private static <T> SnbtJsonArgument<T, JsonElement> argument(JsonInput input, TypeAdapter<T> adapter, ArgumentType<JsonElement> nativeType) {
        return new SnbtJsonArgument<>(nativeType, value -> value, input, adapter);
    }

    private static Set<String> texts(SnbtJsonArgument<?, JsonElement> argument, String input) {
        return texts(suggestions(argument, input));
    }

    private static Set<String> texts(Suggestions suggestions) {
        Set<String> values = new LinkedHashSet<>();
        suggestions.getList().forEach(value -> values.add(value.getText()));
        return values;
    }

    private static Suggestions suggestions(SnbtJsonArgument<?, JsonElement> argument, String input) {
        return suggestions(argument, input, 0);
    }

    private static Suggestions suggestions(SnbtJsonArgument<?, JsonElement> argument, String input, int start) {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        CommandContext<Object> context = new CommandContextBuilder<>(dispatcher, SOURCE, dispatcher.getRoot(), 0).build(input);
        return argument.suggestions(context, new SuggestionsBuilder(input, start)).join();
    }

    private static void assertSuggestions(SnbtJsonArgument<?, JsonElement> argument, String input, Map<String, StringRange> expected) {
        Map<String, StringRange> actual = new LinkedHashMap<>();
        suggestions(argument, input).getList().forEach(value -> actual.put(value.getText(), value.getRange()));
        assertEquals(expected, actual);
    }

    private static void assertState(SnbtJsonArgument<?, JsonElement> argument, String input, Set<String> expected, StringRange range) {
        Suggestions suggestions = suggestions(argument, input);
        assertEquals(expected, texts(suggestions), input);
        assertTrue(suggestions.getList().stream().allMatch(value -> value.getRange().equals(range)), input);
    }

    private static Suggestion suggestion(SnbtJsonArgument<?, JsonElement> argument, String input, int start, String expected) {
        return suggestions(argument, input, start).getList().stream().filter(value -> value.getText().equals(expected)).findFirst().orElseThrow();
    }

    private record Flags(boolean enabled) {}

    private enum DeepMode { SAFE, FORCE }

    private record DeepValue(String name, int count, DeepMode mode, List<String> values, Map<DeepMode, List<Integer>> attributes) {}

    private record DeepObject(String id, DeepValue value, List<DeepValue> values, Map<DeepMode, DeepValue> modes,
                              Map<DeepMode, Map<String, List<DeepValue>>> groups, List<Map<DeepMode, Map<String, DeepValue>>> nested) {}

    private static final class DeepClass {
        private String id = "default-class";
        private int count = 42;
        private boolean enabled = true;
        private DeepMode mode = DeepMode.SAFE;
        private DeepObject object = new DeepObject("default-object",
                new DeepValue("default-value", 7, DeepMode.SAFE, List.of("first", "second"), Map.of(DeepMode.SAFE, List.of(1, 2))),
                List.of(), Map.of(), Map.of(), List.of());
        private List<DeepObject> objects = List.of(object);
        private Map<DeepMode, DeepObject> modes = Map.of(DeepMode.SAFE, object);
        private Map<DeepMode, Map<String, List<DeepObject>>> groups = Map.of(DeepMode.SAFE, Map.of("default", List.of(object)));
        private List<DeepClass> children = List.of();
    }

    private static class JsonNativeType implements ArgumentType<JsonElement> {
        @Override
        public JsonElement parse(StringReader reader) {
            JsonElement value = JsonParser.parseString(reader.getRemaining());
            reader.setCursor(reader.getTotalLength());
            return value;
        }
    }
}
