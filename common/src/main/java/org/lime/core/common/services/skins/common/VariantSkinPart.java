package org.lime.core.common.services.skins.common;

import org.lime.core.common.utils.execute.Func1;

public enum VariantSkinPart {
    HEAD(SkinPart.HEAD),
    BODY(SkinPart.BODY),
    RIGHT_ARM(SkinPart.RIGHT_ARM_CLASSIC, SkinPart.RIGHT_ARM_SLIM),
    LEFT_ARM(SkinPart.LEFT_ARM_CLASSIC, SkinPart.LEFT_ARM_SLIM),
    RIGHT_LEG(SkinPart.RIGHT_LEG),
    LEFT_LEG(SkinPart.LEFT_LEG),
    ;

    private final Func1<SkinVariant, SkinPart> face;

    VariantSkinPart(SkinPart face) {
        this.face = v -> face;
    }
    VariantSkinPart(SkinPart classic, SkinPart slim) {
        this.face = type -> switch (type) {
            case SLIM -> slim;
            case CLASSIC -> classic;
            default -> throw new IllegalArgumentException("Skin variant " + type + " not supported");
        };
    }

    public SkinPart face(SkinVariant type) {
        return face.invoke(type);
    }
}
