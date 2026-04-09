package org.lime.core.paper.services;

import com.google.inject.Inject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.skins.BaseSkinsCache;
import org.lime.core.common.services.skins.common.GameProfileAccess;
import org.lime.core.common.services.skins.common.VariantSkinPart;
import org.lime.core.common.services.skins.common.SkinData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@BindService
public class SkinsCache
        extends BaseSkinsCache<ServerPlayer, GameProfile> {
    @Inject MinecraftServer server;

    @Override
    protected Property renameProperty(Property property, String name) {
        return new Property(name, property.value(), property.signature());
    }
    @Override
    protected GameProfile playerGameProfile(ServerPlayer serverPlayer) {
        return serverPlayer.getGameProfile();
    }
    @Override
    protected GameProfileAccess gameProfileAccess(GameProfile gameProfile) {
        return GameProfileAccess.of(gameProfile);
    }
    @Override
    protected VariantSkinPart mainHand(ServerPlayer player) {
        return switch (player.getMainArm()) {
            case LEFT -> VariantSkinPart.LEFT_ARM;
            case RIGHT -> VariantSkinPart.RIGHT_ARM;
        };
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> skinDataProfile(GameProfile profile) {
        var textures = server.getSessionService().getTextures(profile);
        HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture> result = new HashMap<>();
        putIfNotNull(result, MinecraftProfileTexture.Type.SKIN, textures.skin());
        putIfNotNull(result, MinecraftProfileTexture.Type.CAPE, textures.cape());
        putIfNotNull(result, MinecraftProfileTexture.Type.ELYTRA, textures.elytra());
        return result;
    }
    @Override
    public void flush(ServerPlayer player) {
        CraftPlayer craftPlayer = player.getBukkitEntity();
        craftPlayer.setPlayerProfile(craftPlayer.getPlayerProfile());
    }

    private static ServerPlayer castPlayer(CraftPlayer player) {
        return player.getHandle();
    }
    private static ServerPlayer castPlayer(Player player) {
        return castPlayer((CraftPlayer)player);
    }

    public Optional<SkinData> handHeadSkin(Player player) {
        return handHeadSkin(castPlayer(player));
    }
    public Optional<SkinData> handHeadSkin(Player player, float handPercent) {
        return handHeadSkin(castPlayer(player), handPercent);
    }
    public boolean apply(Player player, @Nullable SkinData skin) {
        return apply(castPlayer(player), skin);
    }
    public boolean apply(Player player, @Nullable SkinData skin, @Nullable String cacheKey) {
        return apply(castPlayer(player), skin, cacheKey);
    }
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> skinData(Player player) {
        return skinData(castPlayer(player));
    }
    public Optional<Property> skinDataProperty(Player player) {
        return skinDataProperty(castPlayer(player));
    }
    public void flush(Player player) {
        flush(castPlayer(player));
    }

    public Optional<SkinData> handHeadSkin(CraftPlayer player) {
        return handHeadSkin(castPlayer(player));
    }
    public Optional<SkinData> handHeadSkin(CraftPlayer player, float handPercent) {
        return handHeadSkin(castPlayer(player), handPercent);
    }
    public boolean apply(CraftPlayer player, @Nullable SkinData skin) {
        return apply(castPlayer(player), skin);
    }
    public boolean apply(CraftPlayer player, @Nullable SkinData skin, @Nullable String cacheKey) {
        return apply(castPlayer(player), skin, cacheKey);
    }
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> skinData(CraftPlayer player) {
        return skinData(castPlayer(player));
    }
    public Optional<Property> skinDataProperty(CraftPlayer player) {
        return skinDataProperty(castPlayer(player));
    }
    public void flush(CraftPlayer player) {
        flush(castPlayer(player));
    }
}
