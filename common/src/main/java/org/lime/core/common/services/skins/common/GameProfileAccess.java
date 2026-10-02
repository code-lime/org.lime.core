package org.lime.core.common.services.skins.common;

import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.utils.Lazy;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Func1;

import java.util.UUID;

public interface GameProfileAccess {
    @SuppressWarnings("unchecked")
    Func1<GameProfile, PropertyMap> PROPERTIES = ReflectionMethod.ofMojangOptional(GameProfile.class, "getProperties")
            .orElseGet(() -> ReflectionMethod.ofMojang(GameProfile.class, "properties"))
            .lambda(Func1.class);

    UUID id();
    String name();

    Iterable<Property> properties(String key);
    void modify(Action1<Multimap<String, Property>> properties);
    <T>T modifyMap(Func1<Multimap<String, Property>, T> properties);

    static GameProfileAccess of(GameProfile profile) {
        return new GameProfileAccess() {
            private final Lazy<PropertyMap> properties = Lazy.of(() -> PROPERTIES.invoke(profile));

            @Override
            public UUID id() {
                return profile.getId();
            }
            @Override
            public String name() {
                return profile.getName();
            }
            @Override
            public Iterable<Property> properties(String key) {
                return this.properties.value().get(key);
            }
            @Override
            public void modify(Action1<Multimap<String, Property>> properties) {
                properties.invoke(this.properties.value());
            }
            @Override
            public <T> T modifyMap(Func1<Multimap<String, Property>, T> properties) {
                return properties.invoke(this.properties.value());
            }
        };
    }
}
