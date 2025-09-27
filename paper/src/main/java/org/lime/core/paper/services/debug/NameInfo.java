package org.lime.core.paper.services.debug;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Math;

public record NameInfo(
        String name,
        float scale,
        Vec3 delta) {
    public static final float DEFAULT_SCALE = 1;

    public Location renderLocation(World world, AABB aabb) {
        return new Location(world,
                Math.lerp(aabb.minX, aabb.maxX, delta.x),
                Math.lerp(aabb.minY, aabb.maxY, delta.y),
                Math.lerp(aabb.minZ, aabb.maxZ, delta.z));
    }

    public static NameInfo upper(String name, float scale) {
        return new NameInfo(name, scale, new Vec3(0.5, 1, 0.5));
    }
    public static NameInfo center(String name, float scale) {
        return new NameInfo(name, scale, new Vec3(0.5, 0.5, 0.5));
    }
    public static NameInfo floor(String name, float scale) {
        return new NameInfo(name, scale, new Vec3(0.5, 0, 0.5));
    }

    public static NameInfo upper(String name) {
        return upper(name, DEFAULT_SCALE);
    }
    public static NameInfo center(String name) {
        return center(name, DEFAULT_SCALE);
    }
    public static NameInfo floor(String name) {
        return floor(name, DEFAULT_SCALE);
    }
}
