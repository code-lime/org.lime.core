package org.lime.core.fabric.brigadier.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import org.lime.core.fabric.brigadier.CustomArgumentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ArgumentTypeInfos.class)
public abstract class ArgumentTypeInfosMixin {
    @ModifyVariable(
            method = "unpack",
            at = @At("HEAD"),
            index = 1)
    private static ArgumentType<?> modify(ArgumentType<?> argumentType) {
        return argumentType instanceof CustomArgumentType<?,?> customArgumentType
                ? customArgumentType.mappedArgument().nativeType()
                : argumentType;
    }
}

