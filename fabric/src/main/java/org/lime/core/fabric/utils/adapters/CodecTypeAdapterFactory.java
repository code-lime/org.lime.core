package org.lime.core.fabric.utils.adapters;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
//#switch PROPERTIES.versionMinecraft
//#caseof 1.21.4;1.21.8
//#default
import net.minecraft.resources.RegistryOps;
//#endswitch
import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.reflection.ReflectionField;
import org.lime.core.common.utils.adapters.JsonTypeAdapter;
import org.lime.core.common.utils.json.builder.Json;

import java.lang.reflect.Modifier;
import java.util.Optional;

public class CodecTypeAdapterFactory
        implements TypeAdapterFactory {
    private class CodecTypeAdapter<T>
            extends JsonTypeAdapter<T, JsonObject> {
        private final Codec<T> codec;
        private final DSL.TypeReference reference;

        public CodecTypeAdapter(
                Gson gson,
                Codec<T> codec,
                @Nullable DSL.TypeReference reference) {
            super(gson, JsonObject.class);
            this.codec = codec;
            this.reference = reference;
        }

        @Override
        public JsonObject write(T value) {
            JsonElement result = codec.encodeStart(ops, value).result().orElseThrow();
            return Json.object()
                    .add("version", currentVersion)
                    .add("value", result)
                    .build();
        }
        @Override
        public T read(JsonObject value) {
            int version = Optional.ofNullable(value.get("version"))
                    .map(JsonElement::getAsInt)
                    .orElse(currentVersion);
            JsonElement data = value.get("value");
            if (reference != null)
                data = dataFixer.update(reference, new Dynamic<>(JsonOps.INSTANCE, data), version, currentVersion).getValue();

            //#switch PROPERTIES.versionMinecraft
            //#caseof 1.21.4;1.21.8
            //OF//            return codec.parse(ops, data).getOrThrow(IllegalArgumentException::new);
            //#default
            return codec.parse(ops, data).getOrThrow(false, IllegalArgumentException::new);
            //#endswitch
        }
    }

    private final DataFixer dataFixer;
    private final DynamicOps<JsonElement> ops;
    private final int currentVersion;

    public CodecTypeAdapterFactory(
            MinecraftServer server) {
        dataFixer = server.getFixerUpper();
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.21.4;1.21.8
        //OF//        ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        //#default
        ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        //#endswitch

        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.21.8
        //OF//        currentVersion = SharedConstants.getCurrentVersion().dataVersion().version();
        //#default
        currentVersion = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        //#endswitch
    }
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        var rawType = type.getRawType();
        ReflectionField<Codec<T>> codecField;
        try {
            codecField = ReflectionField.ofMojang(rawType, "CODEC");
        } catch (Exception ex) {
            return null;
        }
        if (!codecField.is(Modifier.STATIC, Modifier.FINAL))
            return null;
        if (!codecField.target().getGenericType().equals(TypeUtils.parameterize(Codec.class, type.getType())))
            return null;
        Codec<T> codec = codecField.get(null);
        return new CodecTypeAdapter<>(gson, codec, null);
    }
}
