package org.lime.core.common.services.skins.common;

import com.google.common.collect.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.*;
import org.lime.core.common.reflection.*;
import org.lime.core.common.utils.execute.*;

import java.util.UUID;

public interface GameProfileAccess<T> {
    @SuppressWarnings("unchecked")
    Func1<GameProfile, PropertyMap> PROPERTIES = ReflectionMethod.ofMojangOptional(GameProfile.class, "getProperties")
            .orElseGet(() -> ReflectionMethod.ofMojang(GameProfile.class, "properties"))
            .lambda(Func1.class);
    @SuppressWarnings("unchecked")
    Func1<GameProfile, UUID> ID = ReflectionMethod.ofMojangOptional(GameProfile.class, "getId")
            .orElseGet(() -> ReflectionMethod.ofMojang(GameProfile.class, "id"))
            .lambda(Func1.class);
    @SuppressWarnings("unchecked")
    Func1<GameProfile, String> NAME = ReflectionMethod.ofMojangOptional(GameProfile.class, "getName")
            .orElseGet(() -> ReflectionMethod.ofMojang(GameProfile.class, "name"))
            .lambda(Func1.class);

    UUID id();
    String name();
    Iterable<Property> properties(String key);
    T modify(Action1<Multimap<String, Property>> properties);

    static GameProfileAccess<GameProfile> of(GameProfile profile) {
        return new GameProfileAccess<>() {
            @Override
            public UUID id() {
                return ID.invoke(profile);
            }
            @Override
            public String name() {
                return NAME.invoke(profile);
            }
            @Override
            public Iterable<Property> properties(String key) {
                return PROPERTIES.invoke(profile).get(key);
            }
            @Override
            public GameProfile modify(Action1<Multimap<String, Property>> action) {
                var properties = ArrayListMultimap.create(PROPERTIES.invoke(profile));
                action.invoke(properties);
                var constructor = Reflection.constructorOptional(GameProfile.class, UUID.class, String.class, PropertyMap.class);
                if (constructor.isPresent()) {
                    var map = ReflectionConstructor.of(PropertyMap.class, Multimap.class).newInstance(properties);
                    return ReflectionConstructor.of(constructor.get()).newInstance(id(), name(), map);
                }
                var updated = new GameProfile(id(), name());
                PROPERTIES.invoke(updated).putAll(properties);
                return updated;
            }
        };
    }
}
