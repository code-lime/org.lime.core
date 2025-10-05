package org.lime.core.paper.utils.adapters;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Provider;
import net.kyori.adventure.text.Component;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.lime.core.common.utils.adapters.JsonTypeAdapter;
import org.lime.core.common.utils.execute.Func1;
import org.lime.core.common.utils.json.builder.Json;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationSerializableTypeAdapter
        extends JsonTypeAdapter<ConfigurationSerializable, JsonObject> {
    private record Converter<T extends ConfigurationSerializable>(
            Class<T> tClass,
            Func1<T, JsonObject> save,
            Func1<JsonObject, T> load,
            boolean shadow) {
        public JsonObject saveCast(ConfigurationSerializable value) {
            return save.invoke(tClass.cast(value));
        }
        public ConfigurationSerializable loadCast(JsonObject value) {
            return load.invoke(value);
        }
        public <J extends ConfigurationSerializable>Optional<Converter<J>> tryShadowOf(Class<J> shadowClass) {
            if (shadowClass.isAssignableFrom(tClass)) {
                Bukkit.getServer().sendMessage(Component.text("Create shadow converter for: " + shadowClass));
                return Optional.of(new Converter<>(shadowClass, v -> save.invoke(tClass.cast(v)), v -> shadowClass.cast(load.invoke(v)), true));
            }
            return Optional.empty();
        }

        public static <T extends ConfigurationSerializable>Converter<T> defaultConverter(Class<T> tClass) {
            Bukkit.getServer().sendMessage(Component.text("Create converter for: " + tClass));
            return new Converter<>(tClass,
                    v -> Json.by(v.serialize()).build().getAsJsonObject(),
                    v -> tClass.cast(ConfigurationSerialization.deserializeObject(Json.toObject(v), tClass)), false);
        }
    }
    private final ConcurrentHashMap<
            Class<? extends ConfigurationSerializable>,
            Converter<?>> converters = new ConcurrentHashMap<>();

    public ConfigurationSerializableTypeAdapter(Gson gson) {
        super(gson, JsonObject.class);
    }
    public ConfigurationSerializableTypeAdapter(Provider<Gson> gson) {
        super(gson, JsonObject.class);
    }

    public <T extends ConfigurationSerializable>ConfigurationSerializableTypeAdapter addByJson(
            Class<T> tClass,
            Func1<T, JsonObject> save,
            Func1<JsonObject, T> load) {
        converters.put(tClass, new Converter<>(tClass, save, load, false));
        converters.values().removeIf(Converter::shadow);
        return this;
    }
    public <T extends ConfigurationSerializable>ConfigurationSerializableTypeAdapter addByMap(
            Class<T> tClass,
            Func1<T, Map<String, Object>> save,
            Func1<Map<String, Object>, T> load) {
        return addByJson(tClass,
                v -> Json.by(save.invoke(v)).build().getAsJsonObject(),
                v -> load.invoke(Json.toObject(v)));
    }

    @SuppressWarnings("unchecked")
    private <T extends ConfigurationSerializable>Converter<T> accessConverter(Class<T> tClass) {
        return (Converter<T>)converters.computeIfAbsent(tClass, ignored -> converters.values()
                .stream()
                .flatMap(j -> j.tryShadowOf(tClass).stream())
                .findFirst()
                .orElseGet(() -> Converter.defaultConverter(tClass)));
    }

    @Override
    public JsonObject write(ConfigurationSerializable value) {
        var alias = ConfigurationSerialization.getAlias(value.getClass());
        var data = accessConverter(ConfigurationSerialization.getClassByAlias(alias))
                .saveCast(value);
        if (!data.has(ConfigurationSerialization.SERIALIZED_TYPE_KEY))
            data.addProperty(ConfigurationSerialization.SERIALIZED_TYPE_KEY, alias);
        return data;
    }
    @Override
    public ConfigurationSerializable read(JsonObject value) {
        Class<? extends ConfigurationSerializable> clazz = Optional.ofNullable(value.get(ConfigurationSerialization.SERIALIZED_TYPE_KEY))
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .map(ConfigurationSerialization::getClassByAlias)
                .orElseThrow(() -> new IllegalArgumentException("Data doesn't contain type key ('" + ConfigurationSerialization.SERIALIZED_TYPE_KEY + "')"));
        return accessConverter(clazz).loadCast(value);
    }
}
