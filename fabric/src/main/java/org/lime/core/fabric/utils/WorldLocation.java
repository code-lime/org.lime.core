package org.lime.core.fabric.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record WorldLocation(
        ResourceKey<Level> levelKey,
        Vec3 position,
        Vec2 rotation) {
    public float yaw() {
        return rotation.y;
    }
    public float pitch() {
        return rotation.x;
    }

    public BlockPos blockPos() {
        return BlockPos.containing(position);
    }

    public ServerLevel level(MinecraftServer server) {
        return server.getLevel(levelKey);
    }

    public static WorldLocation of(Entity entity) {
        return of(entity.level(), entity.position(), entity.getRotationVector());
    }
    public static WorldLocation of(Level level, Vec3 position) {
        return of(level, position, Vec2.ZERO);
    }
    public static WorldLocation of(Level level, Vec3 position, Vec2 rotation) {
        return new WorldLocation(level.dimension(), position, rotation);
    }
}
