package org.lime.core.fabric.utils.adapters;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minecraft.resources.*;
import org.jetbrains.annotations.NotNull;

public record ResourceLocationIdentifierProxy(
        //#switch PROPERTIES.versionMinecraft
        //#caseofregex ^1\.(20\.1|21\.([0-9]|10))$
        ResourceLocation handle
        //#default
        //OF//        Identifier handle
        //#endswitch
) implements Keyed {
    @Override
    public @NotNull String toString() {
        return handle.toString();
    }

    public static ResourceLocationIdentifierProxy identifierLocation(ResourceKey<?> resourceKey) {
        return new ResourceLocationIdentifierProxy(
                //#switch PROPERTIES.versionMinecraft
                //#caseofregex ^1\.(20\.1|21\.([0-9]|10))$
                resourceKey.location()
                //#default
                //OF//                resourceKey.identifier()
                //#endswitch
        );
    }

    public static ResourceLocationIdentifierProxy parse(String value) {
        return new ResourceLocationIdentifierProxy(
                //#switch PROPERTIES.versionMinecraft
                //#caseofregex ^1\.(20\.1|21\.([0-9]|10))$
                ResourceLocation.tryParse(value)
                //#default
                //OF//                Identifier.parse(value)
                //#endswitch
        );
    }

    @Override
    public @NotNull Key key() {
        return handle;
    }
}
