package org.lime.core.fabric.brigadier.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import org.lime.core.fabric.brigadier.CustomArgumentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArgumentTypeInfos.class)
public abstract class ArgumentTypeInfosMixin {
    @Inject(
            method = "unpack",
            at = @At("HEAD"),
            cancellable = true
    )
    @SuppressWarnings("unchecked")
    private static <A extends ArgumentType<?>>void unpack(A argumentType, CallbackInfoReturnable<ArgumentTypeInfo.Template<A>> cir) {
        if (argumentType instanceof CustomArgumentType<?,?> customArgumentType) {
            ArgumentTypeInfo.Template template = ArgumentTypeInfos.unpack(customArgumentType.mappedArgument().nativeType());
            cir.setReturnValue(template);
        }
    }
}

