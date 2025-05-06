package org.lime.core.common.system.utils;

import com.google.gson.JsonElement;

import java.util.Optional;

public class EnumUtils {
    public static <T>T forceParseEnum(Class<T> tClass, String name) {
        for (T each : tClass.getEnumConstants())
            if (((Enum<?>)each).name().compareToIgnoreCase(name) == 0)
                return each;
        throw new IllegalArgumentException("No enum constant " + tClass.getCanonicalName() + "." + name);
    }

    public static <T extends Enum<T>>T parseEnum(Class<T> tClass, JsonElement json) {
        try { return json == null || json.isJsonNull() ? null : T.valueOf(tClass, json.getAsString()); } catch (Exception ignore) { return null; }
    }
    public static <T extends Enum<T>>T parseEnum(Class<T> tClass, JsonElement json, T def) {
        try { return json == null || json.isJsonNull() ? def : T.valueOf(tClass, json.getAsString()); } catch (Exception ignore) { return def; }
    }

    public static <T extends Enum<T>> Optional<T> tryParse(Class<T> tClass, String name) {
        try {
            for (T each : tClass.getEnumConstants())
                if (each.name().compareToIgnoreCase(name) == 0)
                    return Optional.of(each);
            return Optional.empty();
        } catch (Exception e) { return Optional.empty(); }
    }
}
