package org.lime.core.paper.commands;

import com.google.gson.*;
import com.google.inject.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.ApiMirrorRootNode;
import io.papermc.paper.command.brigadier.Commands;
import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import org.bukkit.craftbukkit.CraftRegistry;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lime.core.common.api.commands.brigadier.arguments.JsonInput;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.services.CustomArgumentUtility;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

import static org.junit.jupiter.api.Assertions.*;

class SnbtJsonArgumentTest {
    private static final Object SOURCE = new Object();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CraftRegistry.setMinecraftRegistry(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }

    @Test
    void parsesOneVanillaSnbtValueAndLeavesFollowingLiteral() throws Exception {
        ArgumentType<JsonElement> argument = NativeCommandConsumerFactory.INSTANCE.json(JsonInput.raw(), new Gson().getAdapter(JsonElement.class));
        StringReader reader = new StringReader("{enabled:true} apply");

        JsonElement value = argument.parse(reader);

        assertEquals(1, value.getAsJsonObject().get("enabled").getAsInt());
        assertEquals(" apply", reader.getRemaining());
        assertEquals(2, argument.parse(new StringReader("{value:1,value:2}")).getAsJsonObject().get("value").getAsInt());
    }

    @Test
    void completesNativeNumbersQuotationAndNestedValues() {
        Gson gson = new Gson();
        JsonInput input = JsonInput.of(gson, com.google.gson.reflect.TypeToken.get(CompletionData.class));
        ArgumentType<CompletionData> value = NativeCommandConsumerFactory.INSTANCE.json(input, gson.getAdapter(CompletionData.class));
        CommandDispatcher<Object> dispatcher = dispatcher(value);
        String prefix = "test ";
        for (String key : List.of("{mo", "{'mo", "{\"mo", "{'\\u006do")) {
            String quote = key.startsWith("{'") ? "'" : key.startsWith("{\"") ? "\"" : "";
            var suggestions = complete(dispatcher, prefix + key);
            var selected = suggestions.getList().stream().filter(candidate -> candidate.getText().equals(quote + "mode" + quote + ":")).findFirst().orElseThrow();
            assertEquals(prefix + "{" + quote + "mode" + quote + ":", selected.apply(prefix + key));
            assertTrue(selected.getTooltip().getString().contains("SAFE"));
        }
        for (String scalar : List.of("{enabled:1", "{enabled:0b", "{enabled:true", "{enabled:bool(1)", "{count:0x10", "{count:1_000", "{count:1sb")) {
            String command = prefix + scalar;
            var close = complete(dispatcher, command).getList().stream().filter(candidate -> candidate.getText().equals("}")).findFirst().orElseThrow();
            assertEquals(command + "}", close.apply(command));
            assertDoesNotThrow(() -> value.parse(new StringReader((command + "}").substring(prefix.length()))));
        }
        for (String quote : List.of("'", "\"")) {
            String command = prefix + "{name:" + quote + "hello";
            var close = complete(dispatcher, command).getList().stream().filter(candidate -> candidate.getText().equals(quote)).findFirst().orElseThrow();
            assertEquals(command + quote, close.apply(command));
            command = prefix + "{mode:" + quote + "SA";
            String expected = quote + "SAFE" + quote;
            var mode = complete(dispatcher, command).getList().stream().filter(candidate -> candidate.getText().equals(expected)).findFirst().orElseThrow();
            assertEquals(prefix + "{mode:" + expected, mode.apply(command));
        }
        String nested = prefix + "{children:[{name:'first',count:2},{name:'second'}],mode:";
        assertEquals(Set.of("\"SAFE\"", "\"FORCE\""), texts(complete(dispatcher, nested)));
        assertEquals(Set.of("'SAFE'"), texts(complete(dispatcher, prefix + "{mode:'\\u0053A")));
        assertTrue(texts(complete(dispatcher, prefix + "{name:'hello\\")).contains("n"));
        var opaque = dispatcher(NativeCommandConsumerFactory.INSTANCE.json(
                JsonInput.of(JsonInput.object(Map.of("raw", JsonInput.any(), "count", JsonInput.scalar(JsonInput.Type.INTEGER)))), gson.getAdapter(JsonElement.class)));
        String opaqueCommand = prefix + "{raw:{foo";
        var separator = complete(opaque, opaqueCommand).getList().stream().filter(candidate -> candidate.getText().equals(":")).findFirst().orElseThrow();
        assertEquals(opaqueCommand + ":", separator.apply(opaqueCommand));
        assertTrue(texts(complete(opaque, prefix + "{raw:{foo:1}")).contains(", count:"));
    }

    @Test
    void sendsCorrectionsAndColoredTooltipsWithOneNativeReplacementRange() {
        Gson gson = new Gson();
        var dispatcher = dispatcher(NativeCommandConsumerFactory.INSTANCE.json(JsonInput.of(gson, com.google.gson.reflect.TypeToken.get(CompletionData.class)), gson.getAdapter(CompletionData.class)));
        for (String tail : List.of("{mode:UNKNOWN", "{count:garbage", "{count:garbage ")) {
            String command = "test " + tail;
            var suggestions = complete(dispatcher, command);
            assertFalse(suggestions.isEmpty());
            assertTrue(suggestions.getList().stream().noneMatch(candidate -> candidate.getText().equals("}") || candidate.getText().equals(",")));
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                ClientboundCommandSuggestionsPacket.STREAM_CODEC.encode(buffer, new ClientboundCommandSuggestionsPacket(7, suggestions));
                var decoded = ClientboundCommandSuggestionsPacket.STREAM_CODEC.decode(buffer).toSuggestions();
                assertEquals(suggestions.getRange(), decoded.getRange());
                for (var candidate : decoded.getList()) {
                    assertEquals(decoded.getRange(), candidate.getRange());
                    String corrected = candidate.apply(command);
                    assertFalse(corrected.contains("UNKNOWN") || corrected.contains("garbage") || corrected.contains("§"));
                    Component tooltip = assertInstanceOf(Component.class, candidate.getTooltip());
                    assertTrue(tooltip.getString().contains("Invalid value"));
                    assertTrue(tooltip.toFlatList().stream().anyMatch(part -> part.getStyle().getColor() != null
                            && part.getStyle().getColor().getValue() == 0xFF5555));
                    assertTrue(corrected.startsWith("test {"));
                    assertDoesNotThrow(() -> dispatcher.execute(corrected.stripTrailing() + "}", SOURCE));
                    assertEquals(corrected + " untouched", candidate.apply(command + " untouched"));
                }
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void resolvesCurrentSiblingSchemaWithoutMaterializationOrStaleCatalogHints() {
        Gson gson = new Gson();
        AtomicReference<String> variant = new AtomicReference<>("red");
        JsonInput input = JsonInput.of(JsonInput.array(JsonInput.object(Map.of("kind", JsonInput.scalar(JsonInput.Type.STRING),
                "data", JsonInput.dependent("kind", kind -> JsonInput.object(Map.of("mode", JsonInput.scalar(JsonInput.Type.STRING,
                        List.of(new JsonPrimitive(kind.getAsString().equals("alpha") ? variant.get() : "blue"))))))))));
        var dispatcher = dispatcher(NativeCommandConsumerFactory.INSTANCE.json(input, gson.getAdapter(JsonElement.class)));
        String command = "test [{kind:'alpha',data:{mode:";
        assertEquals(Set.of("\"red\""), texts(complete(dispatcher, command)));
        variant.set("green");
        assertEquals(Set.of("\"green\""), texts(complete(dispatcher, command)));
        assertEquals(Set.of("\"blue\""), texts(complete(dispatcher, "test [{kind:'alpha',kind:'beta',data:{mode:")));
        assertTrue(complete(dispatcher, "test [{data:{mode:").isEmpty());
        assertEquals(Set.of("\"green\""), texts(complete(dispatcher, "test [{kind:'beta',data:{}},{kind:'alpha',data:{mode:")));
    }

    private static <T> CommandDispatcher<Object> dispatcher(ArgumentType<T> value) {
        CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("test").then(argument("value", value).executes(context -> 1)));
        return dispatcher;
    }

    private static com.mojang.brigadier.suggestion.Suggestions complete(CommandDispatcher<Object> dispatcher, String command) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(command, SOURCE)).join();
    }

    private static Set<String> texts(com.mojang.brigadier.suggestion.Suggestions suggestions) {
        Set<String> texts = new LinkedHashSet<>();
        suggestions.getList().forEach(candidate -> texts.add(candidate.getText()));
        return texts;
    }

    private record CompletionData(String name, boolean enabled, int count, Mode mode, List<CompletionData> children) {}

    private enum Mode {
        SAFE,
        FORCE,
    }

    private static TypeAdapter<JsonElement> adapter(Gson gson, CommandContext<?> context) {
        String mode = context.getArgument("mode", String.class);
        return new ProviderAdapter(gson.getAdapter(JsonElement.class), mode);
    }

    private static final class ProviderAdapter extends TypeAdapter<JsonElement> implements JsonInput.Provider {
        private final TypeAdapter<JsonElement> delegate;
        private final String mode;

        private ProviderAdapter(TypeAdapter<JsonElement> delegate, String mode) {
            this.delegate = delegate;
            this.mode = mode;
        }

        @Override public void write(com.google.gson.stream.JsonWriter out, JsonElement value) throws IOException { delegate.write(out, value); }
        @Override public JsonElement read(com.google.gson.stream.JsonReader in) throws IOException { return delegate.read(in); }
        @Override public JsonInput.@NonNull Node input(JsonInput.@NonNull Context context) {
            return mode.equals("flag") ? JsonInput.scalar(JsonInput.Type.BOOLEAN)
                    : JsonInput.scalar(JsonInput.Type.STRING, List.of(new JsonPrimitive(mode.equals("alpha") ? "red" : "blue")));
        }
    }
}
