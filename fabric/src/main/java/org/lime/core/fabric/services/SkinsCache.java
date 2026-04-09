package org.lime.core.fabric.services;

import com.google.inject.Inject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.biome.BiomeManager;
import org.lime.core.common.api.BindService;
import org.lime.core.common.services.skins.BaseSkinsCache;
import org.lime.core.common.services.skins.common.GameProfileAccess;
import org.lime.core.common.services.skins.common.VariantSkinPart;

import java.util.*;

@BindService
public class SkinsCache
        extends BaseSkinsCache<ServerPlayer, GameProfile> {
    @Inject MinecraftServer server;

    @Override
    protected Property renameProperty(Property property, String name) {
        return new Property("textures",
                //#switch PROPERTIES.versionMinecraft
                //#caseofregex 1\.21\.*
                //OF//                property.value(), property.signature()
                //#default
                property.getValue(), property.getSignature()
                //#endswitch
        );
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
        //#switch PROPERTIES.versionMinecraft
        //#caseofregex 1\.21\.*
        //OF//        var textures = server.getSessionService().getTextures(profile);
        //OF//        HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture> result = new HashMap<>();
        //OF//        putIfNotNull(result, MinecraftProfileTexture.Type.SKIN, textures.skin());
        //OF//        putIfNotNull(result, MinecraftProfileTexture.Type.CAPE, textures.cape());
        //OF//        putIfNotNull(result, MinecraftProfileTexture.Type.ELYTRA, textures.elytra());
        //OF//        return result;
        //#default
        return server.getSessionService().getTextures(profile, true);
        //#endswitch
    }
    @Override
    public void flush(ServerPlayer player) {
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.21.8
        //OF//        ServerLevel serverLevel = player.level();
        //#default
        ServerLevel serverLevel = player.serverLevel();
        //#endswitch
        PlayerList playerList = serverLevel.getServer().getPlayerList();
        ChunkMap chunkMap = serverLevel.getChunkSource().chunkMap;

        playerList.broadcastAll(new ClientboundBundlePacket(
                List.of(
                        new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())),
                        ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singleton(player))
                )
        ));

        var trackedEntity = chunkMap.entityMap.get(player.getId());
        if (trackedEntity != null) {
            var seenBy = Set.copyOf(trackedEntity.seenBy);
            for (var observerConnection : seenBy) {
                var observer = observerConnection.getPlayer();
                trackedEntity.removePlayer(observer);

                var trackedObserverEntity = chunkMap.entityMap.get(observer.getId());
                if (trackedObserverEntity != null) {
                    trackedObserverEntity.removePlayer(player);
                    trackedObserverEntity.updatePlayer(player);
                }
                trackedEntity.updatePlayer(observer);
            }
        }

        if (!player.isDeadOrDying()) {
            //#switch PROPERTIES.versionMinecraft
            //#caseofregex 1\.21\.*
            //OF//            player.connection.send(new ClientboundBundlePacket(
            //OF//                    List.of(
            //OF//                            new ClientboundRespawnPacket(player.createCommonSpawnInfo(serverLevel), ClientboundRespawnPacket.KEEP_ALL_DATA),
            //OF//                            new ClientboundGameEventPacket(ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0)
            //OF//                    )
            //OF//            ));
            //#default
            player.connection.send(new ClientboundRespawnPacket(
                    serverLevel.dimensionTypeId(),
                    serverLevel.dimension(),
                    BiomeManager.obfuscateSeed(serverLevel.getSeed()),
                    player.gameMode.getGameModeForPlayer(),
                    player.gameMode.getPreviousGameModeForPlayer(),
                    serverLevel.isDebug(),
                    serverLevel.isFlat(),
                    ClientboundRespawnPacket.KEEP_ALL_DATA,
                    player.getLastDeathLocation(),
                    player.getPortalCooldown()
            ));
            //#endswitch
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            var vehicle = player.getVehicle();
            if (vehicle != null)
                player.connection.send(new ClientboundSetPassengersPacket(vehicle));
            if (!player.getPassengers().isEmpty())
                player.connection.send(new ClientboundSetPassengersPacket(player));

            player.onUpdateAbilities();
            player.giveExperiencePoints(0);
            playerList.sendPlayerPermissionLevel(player);
            playerList.sendLevelInfo(player, serverLevel);
            playerList.sendAllPlayerInfo(player);
            //#switch PROPERTIES.versionMinecraft
            //#caseofregex 1\.21\.*
            //OF//            playerList.sendActivePlayerEffects(player);
            //#default
            for (var effect : player.getActiveEffects())
                player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect));
            //#endswitch
        }
    }
}
