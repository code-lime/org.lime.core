package org.lime.core.common;

public enum Artifact {
    FABRIC(false),
    PAPER(false),
    VELOCITY(true);

    public final String key;
    public final boolean proxy;

    Artifact(boolean proxy) {
        this.proxy = proxy;
        this.key = name().toLowerCase();
    }
}
