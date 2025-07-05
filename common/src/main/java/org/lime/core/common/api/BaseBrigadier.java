package org.lime.core.common.api;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import net.kyori.adventure.text.Component;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;

public interface BaseBrigadier {
    Message brigadierTooltip(Component component);
    <T, N> ArgumentType<T> brigadierArgument(BaseMappedArgument<T, N> mappedArgument);
}
