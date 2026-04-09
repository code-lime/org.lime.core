package org.lime.core.velocity.services;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.velocitypowered.api.proxy.Player;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.skins.BaseSkinsCache;
import org.lime.core.common.services.skins.common.GameProfileAccess;
import org.lime.core.common.services.skins.common.VariantSkinPart;
import org.lime.core.velocity.services.common.PlayerGameProfile;

import java.net.Proxy;
import java.util.Map;

@BindService
public class SkinsCache
        extends BaseSkinsCache<Player, PlayerGameProfile> {
    private final YggdrasilAuthenticationService yggdrasilAuthenticationService = new YggdrasilAuthenticationService(Proxy.NO_PROXY);
    private final MinecraftSessionService sessionService = yggdrasilAuthenticationService.createMinecraftSessionService();

    @Override
    protected Property renameProperty(Property property, String name) {
        return new Property("textures", property.getValue(), property.getSignature());
    }
    @Override
    protected PlayerGameProfile playerGameProfile(Player serverPlayer) {
        return new PlayerGameProfile(serverPlayer);
    }
    @Override
    protected GameProfileAccess gameProfileAccess(PlayerGameProfile playerGameProfile) {
        return playerGameProfile;
    }
    @Override
    protected VariantSkinPart mainHand(Player player) {
        return switch (player.getPlayerSettings().getMainHand()) {
            case LEFT -> VariantSkinPart.LEFT_ARM;
            case RIGHT -> VariantSkinPart.RIGHT_ARM;
        };
    }
    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> skinDataProfile(PlayerGameProfile profile) {
        return sessionService.getTextures(profile.mojangProfile(), true);
    }
    @Override
    public void flush(Player player) {}
}
