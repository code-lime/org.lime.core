package org.lime.core.common.services.skins.common;

public enum CubeAxisDirection {
    POSITIVE(1),
    NEGATIVE(-1),
    ;

    private final int step;

    CubeAxisDirection(int step) {
        this.step = step;
    }

    public int step() {
        return step;
    }
}
