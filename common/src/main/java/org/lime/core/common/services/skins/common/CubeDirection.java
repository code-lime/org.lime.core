package org.lime.core.common.services.skins.common;

import org.joml.Vector3ic;

public enum CubeDirection {
    DOWN(CubeAxisDirection.NEGATIVE, CubeAxis.Y),
    UP(CubeAxisDirection.POSITIVE, CubeAxis.Y),
    NORTH(CubeAxisDirection.NEGATIVE, CubeAxis.Z),
    SOUTH(CubeAxisDirection.POSITIVE, CubeAxis.Z),
    WEST(CubeAxisDirection.NEGATIVE, CubeAxis.X),
    EAST(CubeAxisDirection.POSITIVE, CubeAxis.X),
    ;

    private final Vector3ic offset;

    private final CubeAxisDirection direction;
    private final CubeAxis axis;

    CubeDirection(CubeAxisDirection direction, CubeAxis axis) {
        this.direction = direction;
        this.axis = axis;

        this.offset = axis.step(direction);
    }

    public Vector3ic offset() {
        return offset;
    }
    public CubeAxisDirection direction() {
        return direction;
    }
    public CubeAxis axis() {
        return axis;
    }
}
