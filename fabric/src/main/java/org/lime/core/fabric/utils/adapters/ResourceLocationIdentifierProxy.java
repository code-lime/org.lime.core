package org.lime.core.fabric.utils.adapters;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minecraft.resources.*;
import org.jetbrains.annotations.NotNull;

public record ResourceLocationIdentifierProxy(
        //#switch PROPERTIES.versionMinecraft
        //#caseof 1.21.11
        //OF//        Identifier handle
        //#default
        ResourceLocation handle
        //#endswitch
) implements Keyed {
    @Override
    public @NotNull String toString() {
        return handle.toString();
    }

    public static ResourceLocationIdentifierProxy identifierLocation(ResourceKey<?> resourceKey) {
        return new ResourceLocationIdentifierProxy(
                //#switch PROPERTIES.versionMinecraft
                //#caseof 1.21.11
                //OF//                resourceKey.identifier()
                //#default
                resourceKey.location()
                //#endswitch
        );
    }

    public static ResourceLocationIdentifierProxy parse(String value) {
        return new ResourceLocationIdentifierProxy(
                //#switch PROPERTIES.versionMinecraft
                //#caseof 1.21.11
                //OF//                Identifier.parse(value)
                //#default
                ResourceLocation.parse(value)
                //#endswitch
        );
    }

    @Override
    public @NotNull Key key() {
        return handle;
    }
}
