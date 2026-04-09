package org.lime.core.common.services.skins.common;

public enum SkinVariant {
    AUTO,
    SLIM,
    CLASSIC,
    ;

    public static SkinVariant parse(String name) {
        return switch (name) {
            case "slim" -> SLIM;
            default -> CLASSIC;
        };
    }
}
