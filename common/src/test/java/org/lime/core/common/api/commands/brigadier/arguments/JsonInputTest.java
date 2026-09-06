package org.lime.core.common.api.commands.brigadier.arguments;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JsonInputTest {
    @Test
    void requiresCustomAdaptersToProvideInput() {
        Gson missing = new GsonBuilder().registerTypeAdapter(External.class, new ExternalAdapter()).create();
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> JsonInput.of(missing, TypeToken.get(External.class)));
        assertTrue(exception.getMessage().contains(External.class.getName()));

        Gson provided = new GsonBuilder().registerTypeAdapter(External.class, new ProviderAdapter()).create();
        JsonInput input = JsonInput.of(provided, TypeToken.get(Holder.class));
        JsonInput.View external = input.at(List.of("external"));
        assertEquals(Set.of(JsonInput.Type.STRING), external.types());
        assertEquals(List.of(new JsonPrimitive("provided")), external.values());
    }

    @Test
    void infersSerializedEnumsAndInheritedGenericFields() {
        JsonInput input = JsonInput.of(new Gson(), TypeToken.get(Child.class));

        assertEquals(Set.of("local_name", "inherited_value"), input.root().properties().keySet());
        assertEquals(List.of(new JsonPrimitive("safe-mode"), new JsonPrimitive("FORCE")),
                input.at(List.of("inherited_value")).values());
    }

    @Test
    void composesObjectsChoicesAndTuples() {
        JsonInput.Node kind = JsonInput.scalar(JsonInput.Type.STRING, List.of(new JsonPrimitive("display")));
        JsonInput.Node discriminator = JsonInput.object(Map.of("kind", kind), JsonInput.any(), Set.of("kind"));
        JsonInput.Node fields = JsonInput.object(Map.of("name", JsonInput.scalar(JsonInput.Type.STRING)));
        JsonInput input = JsonInput.of(JsonInput.choice(List.of(JsonInput.all(List.of(discriminator, fields)))));

        assertEquals(Set.of("kind", "name"), input.root().properties().keySet());
        assertFalse(input.root().canCloseObject(Set.of()));
        assertTrue(input.root().canCloseObject(Set.of("kind")));
        assertEquals(List.of(new JsonPrimitive("display")), input.at(List.of("kind")).values());

        JsonInput.View tuple = JsonInput.of(JsonInput.tuple(List.of(kind, fields))).root();
        assertFalse(tuple.canCloseArray(1));
        assertTrue(tuple.canCloseArray(2));
        assertFalse(tuple.canAddArrayItem(2));

        JsonInput inputByKind = JsonInput.of(JsonInput.choice(List.of(JsonInput.scalar(JsonInput.Type.STRING), discriminator)));
        assertFalse(inputByKind.root().canCloseObject(Set.of()));
        assertTrue(inputByKind.root().canCloseObject(Set.of("kind")));
    }

    private record External(String value) {}

    private record Holder(External external) {}

    private enum Mode {
        @SerializedName("safe-mode") SAFE,
        FORCE
    }

    private static class Parent<T> {
        private T inheritedValue;
    }

    private static final class Child extends Parent<Mode> {
        private String localName;
    }

    private static class ExternalAdapter extends TypeAdapter<External> {
        @Override
        public void write(JsonWriter out, External value) throws IOException {
            out.value(value.value());
        }

        @Override
        public External read(JsonReader in) throws IOException {
            return new External(in.nextString());
        }
    }

    private static final class ProviderAdapter extends ExternalAdapter implements JsonInput.Provider {
        @Override
        public JsonInput.Node input(JsonInput.Context context) {
            return JsonInput.scalar(JsonInput.Type.STRING, List.of(new JsonPrimitive("provided")));
        }
    }
}
