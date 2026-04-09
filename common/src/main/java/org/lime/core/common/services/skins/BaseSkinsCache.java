package org.lime.core.common.services.skins;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Vector2i;
import org.lime.core.common.api.*;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.services.skins.common.*;
import org.lime.core.common.services.skins.common.Rectangle;
import org.lime.core.common.utils.Lazy;
import org.lime.core.common.utils.Lock;
import org.lime.core.common.utils.execute.ActionEx2;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public abstract class BaseSkinsCache<ServerPlayer, GameProfile>
        implements Service {
    public static class Cache {
        public final HashMap<String, Data> skins = new HashMap<>();

        public static class Data {
            public String value = "";
            public String signature = "";

            public static Data create(String value, String signature) {
                Data data = new Data();
                data.value = value;
                data.signature = signature;
                return data;
            }
        }
    }

    @Inject
    @RequireConfig(path = "skins-cache")
    ConfigAccess<Cache> cache;

    @Inject NativeCommandConsumer.Factory<?, ?> consumerFactory;
    @Inject SkinUtility skins;

    private final Lazy<ArgumentType<SkinData>> SKIN_ARGUMENT = Lazy.of(() -> consumerFactory.argument(new BaseMappedArgument<SkinData, String>() {
        @Override
        public ArgumentType<String> nativeType() {
            return StringArgumentType.string();
        }
        @Override
        public @NotNull SkinData convert(String value) throws CommandSyntaxException {
            try {
                return get(value);
            } catch (Exception e) {
                throw new SimpleCommandExceptionType(new LiteralMessage(e.getMessage())).create();
            }
        }
    }));

    private final Lock lock = Lock.create();

    protected static <K, V> void putIfNotNull(Map<K, V> map, K key, V value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    public ArgumentType<SkinData> skinArgument() {
        return SKIN_ARGUMENT.value();
    }

    public SkinData get(String url) {
        return get(url, SkinVariant.AUTO);
    }
    public SkinData get(String url, SkinVariant variant) {
        if (url.startsWith("raw#")) {
            var args = url.split("#", 3);
            return args.length == 3
                    ? new SkinData(args[1], args[2])
                    : SkinData.NONE;
        }
        return lock.invoke(() -> {
            var cache = this.cache.value();
            String identity = variant.name() + "$" + url;

            var skin = cache.skins.get(identity);
            if (skin != null)
                return new SkinData(skin.value, skin.signature);

            var loaded = skins.upload(url, variant);
            skin = Cache.Data.create(loaded.value(), loaded.signature());
            cache.skins.put(identity, skin);
            this.cache.save(cache);
            return loaded;
        });
    }

    public SkinData get(byte[] png, SkinVariant variant) {
        return get(getUrl(png), variant);
    }
    public SkinData get(BufferedImage image, SkinVariant variant) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return get(out.toByteArray(), variant);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getUrl(byte[] png) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    }

    private static final String OLD_TEXTURES = "old_textures";
    private static final String OLD_TEXTURES_PREFIX = "old_";
    private static final String OLD_TEXTURES_SUFFIX = "_textures";

    protected abstract Property renameProperty(Property property, String name);
    protected abstract GameProfile playerGameProfile(ServerPlayer player);
    protected abstract GameProfileAccess gameProfileAccess(GameProfile profile);
    protected abstract VariantSkinPart mainHand(ServerPlayer player);

    public Optional<SkinData> handHeadSkin(ServerPlayer player) {
        return handHeadSkin(player, 0.5f);
    }
    public Optional<SkinData> handHeadSkin(ServerPlayer player, float handPercent) {
        var playerSkinData = skinData(player);
        var skin = playerSkinData.get(MinecraftProfileTexture.Type.SKIN);
        var variant = getVariant(skin);
        var hand = mainHand(player);

        return Optional.ofNullable(skin)
                .map(MinecraftProfileTexture::getUrl)
                .map(url -> lock.invoke(() -> {
                    var cache = this.cache.value();
                    String identity = "hand_head#" + variant + "#" + hand + "#" + url;

                    var handSkinData = cache.skins.get(identity);
                    if (handSkinData != null)
                        return new SkinData(handSkinData.value, handSkinData.signature);

                    var loaded = createHandHeadSkin(url, hand, variant, handPercent);
                    handSkinData = Cache.Data.create(loaded.value(), loaded.signature());
                    cache.skins.put(identity, handSkinData);
                    this.cache.save(cache);
                    return loaded;
                }));
    }

    public boolean apply(ServerPlayer player, @Nullable SkinData skin) {
        return apply(player, skin, null);
    }
    public boolean apply(ServerPlayer player, @Nullable SkinData skin, @Nullable String cacheKey) {
        if (!applyProfile(playerGameProfile(player), skin, cacheKey))
            return false;
        flush(player);
        return true;
    }

    public boolean applyProfile(GameProfile profile, @Nullable SkinData skin) {
        return applyProfile(profile, skin, null);
    }
    public boolean applyProfile(GameProfile profile, @Nullable SkinData skin, @Nullable String cacheKey) {
        String key = Optional.ofNullable(cacheKey)
                .map(v -> OLD_TEXTURES_PREFIX + v + OLD_TEXTURES_SUFFIX)
                .orElse(OLD_TEXTURES);

        if (skin == null)
            return rollback(profile, key);

        gameProfileAccess(profile)
                .modify(properties -> {
                    Optional.of(properties.get(key))
                            .stream()
                            .flatMap(Collection::stream)
                            .findFirst()
                            .or(() -> Optional.of(properties.get("textures"))
                                    .stream()
                                    .flatMap(Collection::stream)
                                    .findFirst())
                            .ifPresent(property -> {
                                properties.removeAll(key);
                                if (!OLD_TEXTURES.equals(key) && !properties.containsKey(OLD_TEXTURES))
                                    properties.put(OLD_TEXTURES, renameProperty(property, OLD_TEXTURES));

                                properties.put(key, renameProperty(property, key));
                            });
                    properties.removeAll("textures");
                    properties.put("textures", new Property("textures", skin.value(), skin.signature()));
                });

        return true;
    }

    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> skinData(ServerPlayer player) {
        return skinDataProfile(playerGameProfile(player));
    }
    public abstract Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> skinDataProfile(GameProfile profile);

    public Optional<Property> skinDataProperty(ServerPlayer player) {
        return skinDataPropertyProfile(playerGameProfile(player));
    }
    public Optional<Property> skinDataPropertyProfile(GameProfile profile) {
        return Optional.ofNullable(Iterables.getFirst(gameProfileAccess(profile).properties("textures"), null));
    }

    public SkinData createSkin(ActionEx2<BufferedImage, Graphics2D> factory, SkinVariant variant) {
        BufferedImage img = new BufferedImage(SkinPart.TOTAL_WIDTH, SkinPart.TOTAL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setBackground(new Color(0,0,0,0));
        try {
            g.setComposite(AlphaComposite.SrcOver);
            factory.invoke(img, g);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        } finally {
            g.dispose();
        }
        return get(img, variant);
    }
    public SkinVariant getVariant(@Nullable MinecraftProfileTexture texture) {
        return Optional.ofNullable(texture)
                .map(v -> v.getMetadata("model"))
                .map(SkinVariant::parse)
                .orElse(SkinVariant.CLASSIC);
    }

    private boolean rollback(GameProfile profile, String key) {
        return gameProfileAccess(profile)
                .modifyMap(properties -> Optional.of(properties.get(key))
                        .stream()
                        .flatMap(Collection::stream)
                        .findFirst()
                        .map(property -> {
                            properties.removeAll("textures");
                            properties.removeAll(key);
                            if (OLD_TEXTURES.equals(key))
                                properties.keySet().removeIf(v -> v.startsWith(OLD_TEXTURES_PREFIX) && v.endsWith(OLD_TEXTURES_SUFFIX));
                            properties.put("textures", renameProperty(property, "textures"));
                            return true;
                        })
                        .orElse(false));
    }

    public abstract void flush(ServerPlayer player);

    private SkinData createHandHeadSkin(
            String url,
            VariantSkinPart hand,
            SkinVariant variant,
            float handPercent) {
        var handPart = hand.face(variant);
        try {
            var skinImage = ImageIO.read(URI.create(url).toURL());
            return createSkin((img, g) -> {
                for (var face : CubeFace.values()) {
                    for (var layer : SkinLayer.values()) {
                        var fromRect = face.computeRectangle(handPart, layer);
                        var toRect = face.computeRectangle(SkinPart.HEAD, layer);
                        if (face.direction().axis() != CubeAxis.Y) {
                            var size = fromRect.size();
                            var halfSizeY = Math.round(size.y() * handPercent);
                            fromRect = fromRect.offset(new Vector2i(0, halfSizeY), new Vector2i(0, -halfSizeY));
                        }
                        Rectangle.copyAreaScaled(g, skinImage, fromRect, toRect);
                    }
                }
            }, SkinVariant.CLASSIC);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
