package org.lime.core.fabric.brigadier.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import org.lime.core.fabric.brigadier.CustomArgumentType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RequiredArgumentBuilder.class)
public abstract class RequiredArgumentBuilderMixin<S, T> {
    @Shadow @Final private ArgumentType<T> type;

    @Shadow public abstract RequiredArgumentBuilder<S, T> suggests(SuggestionProvider<S> provider);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void afterConstructor(String name, ArgumentType<T> type, CallbackInfo ci) {
        if (this.type instanceof CustomArgumentType<?, ?> customArgumentType)
            this.suggests(customArgumentType::listSuggestions);
    }
}
