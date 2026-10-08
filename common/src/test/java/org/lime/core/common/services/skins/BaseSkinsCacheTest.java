package org.lime.core.common.services.skins;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import org.junit.jupiter.api.Test;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.services.skins.common.*;
import org.lime.core.common.utils.execute.Func1;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BaseSkinsCacheTest {
    static {
        org.lime.core.common.utils.Unsafe.MAPPINGS = org.lime.core.common.services.UnsafeMappingsUtility.EMPTY;
    }
    @Test
    void replacesAndRestoresSkinWithoutMutatingPreviousProfiles() {
        var cache = new TestCache();
        var original = GameProfileAccess.of(new GameProfile(UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"), "Ada"))
                .modify(properties -> {
                    properties.put("textures", new Property("textures", "original", "signature"));
                    properties.put("other", new Property("other", "kept"));
                });
        var first = cache.applyProfile(original, new SkinData("first", "first-signature"), "hand");
        var second = cache.applyProfile(first, new SkinData("second", "second-signature"), "hand");
        assertNotSame(original, first);
        assertNotSame(first, second);
        assertEquals(GameProfileAccess.of(original).id(), GameProfileAccess.of(second).id());
        assertEquals("Ada", GameProfileAccess.of(second).name());
        assertEquals("original", value(original, "textures"));
        assertEquals("first", value(first, "textures"));
        assertEquals("second", value(second, "textures"));
        assertEquals("kept", value(second, "other"));
        assertEquals("original", value(second, "old_hand_textures"));

        var restored = cache.applyProfile(second, null, "hand");
        assertEquals("original", value(restored, "textures"));
        assertEquals("second", value(second, "textures"));
        assertEquals("kept", value(restored, "other"));
        assertNull(cache.applyProfile(original, null));

        var player = new Player(original);
        assertTrue(cache.apply(player, new SkinData("player", "signature")));
        assertEquals("player", value(player.profile, "textures"));
        assertEquals(1, player.flushes);
        assertTrue(cache.apply(player, null));
        assertEquals("original", value(player.profile, "textures"));
        assertEquals(2, player.flushes);
        assertFalse(cache.apply(player, null));
        assertEquals(2, player.flushes);
    }

    @SuppressWarnings("unchecked")
    private static final Func1<Property, String> VALUE = ReflectionMethod.ofMojangOptional(Property.class, "getValue")
            .orElseGet(() -> ReflectionMethod.ofMojang(Property.class, "value"))
            .lambda(Func1.class);
    @SuppressWarnings("unchecked")
    private static final Func1<Property, String> SIGNATURE = ReflectionMethod.ofMojangOptional(Property.class, "getSignature")
            .orElseGet(() -> ReflectionMethod.ofMojang(Property.class, "signature"))
            .lambda(Func1.class);
    private static String value(GameProfile profile, String key) {
        return VALUE.invoke(GameProfileAccess.of(profile).properties(key).iterator().next());
    }

    private static class Player {
        GameProfile profile;
        int flushes;
        Player(GameProfile profile) { this.profile = profile; }
    }
    private static class TestCache extends BaseSkinsCache<Player, GameProfile> {
        @Override protected Property renameProperty(Property property, String name) { return new Property(name, VALUE.invoke(property), SIGNATURE.invoke(property)); }
        @Override protected GameProfile playerGameProfile(Player player) { return player.profile; }
        @Override protected void playerGameProfile(Player player, GameProfile profile) { player.profile = profile; }
        @Override protected GameProfileAccess<GameProfile> gameProfileAccess(GameProfile profile) { return GameProfileAccess.of(profile); }
        @Override protected VariantSkinPart mainHand(Player player) { return VariantSkinPart.RIGHT_ARM; }
        @Override public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> skinDataProfile(GameProfile profile) { return Map.of(); }
        @Override public void flush(Player player) { player.flushes++; }
    }
}
