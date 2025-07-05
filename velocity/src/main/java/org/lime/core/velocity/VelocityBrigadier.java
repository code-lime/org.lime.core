package org.lime.core.velocity;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import net.kyori.adventure.text.Component;
import org.lime.core.common.api.BaseBrigadier;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.velocity.brigadier.CustomArgumentType;

public interface VelocityBrigadier
        extends BaseBrigadier {
    @Override
    default Message brigadierTooltip(Component component) {
        return VelocityBrigadierMessage.tooltip(component);
    }
    @Override
    default <T, N> ArgumentType<T> brigadierArgument(BaseMappedArgument<T, N> mappedArgument) {
        return new CustomArgumentType<>(mappedArgument);
    }
}
