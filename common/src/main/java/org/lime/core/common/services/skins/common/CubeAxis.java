package org.lime.core.common.services.skins.common;

import org.joml.Vector3i;
import org.joml.Vector3ic;

public enum CubeAxis {
    X(new Vector3i(1, 0, 0)),
    Y(new Vector3i(0, 1, 0)),
    Z(new Vector3i(0, 0, 1)),
    ;

    private final Vector3ic step;

    CubeAxis(Vector3ic step) {
        this.step = step;
    }

    public Vector3ic step(CubeAxisDirection direction) {
        return step.mul(direction.step(), new Vector3i());
    }
}
