package org.lime.core.fabric;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import org.lime.core.common.api.BaseBrigadier;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.fabric.brigadier.CustomArgumentType;

public interface FabricBrigadier
        extends FabricServer, BaseBrigadier {
    @Override
    default Message brigadierTooltip(Component component) {
        return FabricServerAudiences.of(server()).toNative(component);
    }
    @Override
    default <T, N>ArgumentType<T> brigadierArgument(BaseMappedArgument<T, N> mappedArgument) {
        return new CustomArgumentType<>(mappedArgument);
    }
}
