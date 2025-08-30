package org.lime.core.fabric.commands.brigadier.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import org.lime.core.fabric.commands.brigadier.CustomArgumentType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArgumentCommandNode.class)
public abstract class ArgumentCommandNodeMixin<S, T> {
    @Shadow @Final private String name;
    @Shadow @Final private ArgumentType<T> type;

    @Inject(
            method = "parse",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void parse(StringReader reader, CommandContextBuilder<S> contextBuilder, CallbackInfo ci)
            throws CommandSyntaxException {
        if (!(this.type instanceof CustomArgumentType<?,?> customArgumentType))
            return;
        final int start = reader.getCursor();
        final var result = customArgumentType.parse(reader, contextBuilder.getSource());
        final ParsedArgument<S, ?> parsed = new ParsedArgument<>(start, reader.getCursor(), result);
        contextBuilder.withArgument(this.name, parsed);
        contextBuilder.withNode((ArgumentCommandNode<S, T>)(Object)this, parsed.getRange());
        ci.cancel();
    }
}
