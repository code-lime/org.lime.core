package org.lime.core.common.services.skins.common;

import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Func1;

import java.util.UUID;

public interface GameProfileAccess {
    UUID id();
    String name();

    Iterable<Property> properties(String key);
    void modify(Action1<Multimap<String, Property>> properties);
    <T>T modifyMap(Func1<Multimap<String, Property>, T> properties);

    static GameProfileAccess of(GameProfile profile) {
        return new GameProfileAccess() {
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
                return profile.getProperties().get(key);
            }
            @Override
            public void modify(Action1<Multimap<String, Property>> properties) {
                properties.invoke(profile.getProperties());
            }
            @Override
            public <T> T modifyMap(Func1<Multimap<String, Property>, T> properties) {
                return properties.invoke(profile.getProperties());
            }
        };
    }
}
