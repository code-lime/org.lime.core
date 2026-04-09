package org.lime.core.common.services.skins.common;

import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3i;
import org.joml.Vector3ic;

public enum SkinPart {
    HEAD(
            vec2(0, 0),
            vec2(32, 0),
            vec3(8, 8, 8)),
    BODY(
            vec2(16, 16),
            vec2(16, 32),
            vec3(8, 12, 4)),
    RIGHT_ARM_CLASSIC(
            vec2(40, 16),
            vec2(40, 32),
            vec3(4, 12, 4)),
    RIGHT_ARM_SLIM(
            vec2(40, 16),
            vec2(40, 32),
            vec3(3, 12, 4)),
    LEFT_ARM_CLASSIC(
            vec2(32, 48),
            vec2(48, 48),
            vec3(4, 12, 4)),
    LEFT_ARM_SLIM(
            vec2(32, 48),
            vec2(48, 48),
            vec3(3, 12, 4)),
    RIGHT_LEG(
            vec2(0, 16),
            vec2(0, 32),
            vec3(4, 12, 4)),
    LEFT_LEG(
            vec2(16, 48),
            vec2(0, 48),
            vec3(4, 12, 4)),
    ;

    public static final int TOTAL_WIDTH = 64;
    public static final int TOTAL_HEIGHT = 64;

    public final Vector2ic defaultPoint;
    public final Vector2ic overlayPoint;
    public final Vector3ic size;

    SkinPart(Vector2ic defaultPoint,
             Vector2ic overlayPoint,
             Vector3ic size) {
        this.defaultPoint = defaultPoint;
        this.overlayPoint = overlayPoint;
        this.size = size;
    }

    public Vector2ic point(SkinLayer layer) {
        return switch (layer) {
            case DEFAULT -> defaultPoint;
            case OVERLAY -> overlayPoint;
        };
    }

    private static Vector2ic vec2(int x, int y) {
        return new Vector2i(x, y);
    }
    private static Vector3ic vec3(int x, int y, int z) {
        return new Vector3i(x, y, z);
    }
}
