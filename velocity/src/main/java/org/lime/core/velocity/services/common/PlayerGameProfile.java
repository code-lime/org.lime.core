package org.lime.core.velocity.services.common;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mojang.authlib.properties.Property;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.GameProfile;
import org.lime.core.common.services.skins.common.GameProfileAccess;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Func1;

import java.util.UUID;

public class PlayerGameProfile
        implements GameProfileAccess {
    public final Player player;
    private GameProfile profile;

    public PlayerGameProfile(Player player) {
        this.player = player;
        this.profile = player.getGameProfile();
    }

    @Override
    public UUID id() {
        return profile.getId();
    }
    @Override
    public String name() {
        return profile.getName();
    }
    public GameProfile profile() {
        return profile;
    }
    public com.mojang.authlib.GameProfile mojangProfile() {
        var profile = this.profile;
        var result = new com.mojang.authlib.GameProfile(id(), name());
        var props = result.getProperties();
        profile.getProperties()
                .forEach(property -> props.put(property.getName(), cast(property)));
        return result;
    }

    @Override
    public Iterable<Property> properties(String key) {
        return Iterables.transform(Iterables.filter(profile.getProperties(), v -> key.equals(v.getName())), this::cast);
    }

    private Property cast(GameProfile.Property property) {
        return new Property(property.getName(), property.getValue(), property.getSignature());
    }

    private GameProfile.Property cast(Property property) {
        return new GameProfile.Property(property.getName(), property.getValue(), property.getSignature());
    }

    @Override
    public void modify(Action1<Multimap<String, Property>> properties) {
        LinkedHashMultimap<String, Property> map = profile
                .getProperties()
                .stream()
                .collect(Multimaps.toMultimap(GameProfile.Property::getName, this::cast, LinkedHashMultimap::create));
        properties.invoke(map);
        player.setGameProfileProperties(map.values().stream().map(this::cast).toList());
        profile = player.getGameProfile();
    }

    @Override
    public <T> T modifyMap(Func1<Multimap<String, Property>, T> properties) {
        LinkedHashMultimap<String, Property> map = profile
                .getProperties()
                .stream()
                .collect(Multimaps.toMultimap(GameProfile.Property::getName, this::cast, LinkedHashMultimap::create));
        var result = properties.invoke(map);
        player.setGameProfileProperties(map.values().stream().map(this::cast).toList());
        profile = player.getGameProfile();
        return result;
    }
}
