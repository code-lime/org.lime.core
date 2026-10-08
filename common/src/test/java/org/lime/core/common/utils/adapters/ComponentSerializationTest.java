package org.lime.core.common.utils.adapters;

import com.google.gson.*;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;
import org.lime.core.common.utils.PlaceholderComponent;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ComponentSerializationTest {
    @Test
    void preservesComponentsAndPlaceholderTemplates() {
        var gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapterFactory(new TestAdapters().factory()).create();
        var component = MiniMessage.miniMessage().deserialize("<gold>Hello <click:run_command:'/utility'>Ada</click>");
        var encoded = gson.toJson(component, Component.class);
        assertEquals(component, gson.fromJson(encoded, Component.class));

        var template = new PlaceholderComponent("<green><player>: <stage>");
        assertEquals("\"<green><player>: <stage>\"", gson.toJson(template));
        var restored = gson.fromJson(gson.toJson(template), PlaceholderComponent.class);
        assertEquals(template.renderPlaceholders(Map.of("player", Component.text("Ada"), "stage", Component.text("End"))),
                restored.renderPlaceholders(Map.of("player", Component.text("Ada"), "stage", Component.text("End"))));
    }

    private static class TestAdapters extends CommonGsonTypeAdapters {
        TypeAdapterFactory factory() { return miniMessage(MiniMessage.miniMessage()); }
        @Override public java.util.stream.Stream<TypeAdapterFactory> factories() { return java.util.stream.Stream.of(factory()); }
    }
}
