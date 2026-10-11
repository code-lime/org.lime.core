package org.lime.core.paper.services;

import com.mojang.authlib.*;
import com.mojang.authlib.minecraft.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SkinsCacheTest {
    @Test
    void readsAvailableTexturesFromTheInjectedProfileFunction() {
        var profile = new GameProfile(UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"), "Ada");
        var skin = new MinecraftProfileTexture("https://textures.minecraft.net/texture/skin", Map.of("model", "slim"));
        var cape = new MinecraftProfileTexture("https://textures.minecraft.net/texture/cape", Map.of());
        var elytra = new MinecraftProfileTexture("https://textures.minecraft.net/texture/elytra", Map.of());
        var cache = new SkinsCache();
        cache.getTextures = value -> {
            assertSame(profile, value);
            return new MinecraftProfileTextures(skin, cape, elytra, SignatureState.SIGNED);
        };

        assertEquals(Map.of(MinecraftProfileTexture.Type.SKIN, skin,
                MinecraftProfileTexture.Type.CAPE, cape, MinecraftProfileTexture.Type.ELYTRA, elytra), cache.skinDataProfile(profile));

        cache.getTextures = value -> new MinecraftProfileTextures(skin, null, null, SignatureState.SIGNED);
        assertEquals(Map.of(MinecraftProfileTexture.Type.SKIN, skin), cache.skinDataProfile(profile));
        cache.getTextures = value -> MinecraftProfileTextures.EMPTY;
        assertTrue(cache.skinDataProfile(profile).isEmpty());
    }

    @Test
    void propagatesTextureLookupFailure() {
        var failure = new IllegalStateException("Session service unavailable");
        var cache = new SkinsCache();
        cache.getTextures = profile -> {
            throw failure;
        };

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> cache.skinDataProfile(new GameProfile(UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"), "Ada"))));
    }
}
