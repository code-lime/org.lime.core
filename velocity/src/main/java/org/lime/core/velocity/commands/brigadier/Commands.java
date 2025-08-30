package org.lime.core.velocity.commands.brigadier;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;
import org.jetbrains.annotations.NotNull;
import org.lime.core.common.utils.Unsafe;
import org.lime.core.common.reflection.ReflectionField;
import org.lime.core.common.utils.system.execute.Action2;
import org.lime.core.common.utils.system.execute.Action3;

import java.util.concurrent.Callable;

public class Commands {
    private static <S, T, N>ArgumentCommandNode<S, N> of(
            CustomArgumentType<T, N> customArgumentType,
            RequiredArgumentBuilder<S, ?> builder) {
        final CustomArgumentCommandNode<S, T, N> result = new CustomArgumentCommandNode<>(
                builder.getName(),
                customArgumentType,
                builder.getCommand(),
                builder.getRequirement(),
                builder.getContextRequirement(),
                builder.getRedirect(),
                builder.getRedirectModifier(),
                builder.isFork(),
                builder.getSuggestionsProvider());

        for (final CommandNode<S> argument : builder.getArguments())
            result.addChild(argument);

        return result;
    }

    public static class Interceptor {
        @RuntimeType
        public Object intercept(
                @This Object instance,
                @AllArguments Object[] args,
                @SuperCall Callable<?> superCall) throws Exception {
            if (instance instanceof RequiredArgumentBuilder<?,?> requiredArgumentBuilder)
                if (requiredArgumentBuilder.getType() instanceof CustomArgumentType customArgumentType) {
                    return of(customArgumentType, requiredArgumentBuilder);
                }
            return superCall.call();
        }
    }

    private static final Class<? extends RequiredArgumentBuilder> customRequiredArgumentBuilderClass = new ByteBuddy()
            .subclass(RequiredArgumentBuilder.class)

            .method(ElementMatchers.named("build"))
            .intercept(MethodDelegation.to(new Interceptor()))

            .make()
            .load(Commands.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
            .getLoaded();

    private static final Action2<RequiredArgumentBuilder<?,?>, String> requiredArgumentNameSetter = ReflectionField
            .of(RequiredArgumentBuilder.class, "name")
            .access()
            .nonFinal()
            .setter(Action2.class);
    private static final Action2<RequiredArgumentBuilder<?,?>, ArgumentType<?>> requiredArgumentTypeSetter = ReflectionField
            .of(RequiredArgumentBuilder.class, "type")
            .access()
            .nonFinal()
            .setter(Action2.class);
    private static final Action2<ArgumentBuilder<?,?>, RootCommandNode<?>> argumentArgumentsSetter = ReflectionField
            .of(ArgumentBuilder.class, "arguments")
            .access()
            .nonFinal()
            .setter(Action2.class);

    private static final Action3<RequiredArgumentBuilder, String, ArgumentType<?>> customRequiredArgumentBuilderCtor = (builder, name, type) -> {
        requiredArgumentNameSetter.invoke(builder, name);
        requiredArgumentTypeSetter.invoke(builder, type);
        argumentArgumentsSetter.invoke(builder, new RootCommandNode<>());
        builder.requires(s -> true);
        builder.requiresWithContext((c,r) -> true);
    };

    public static LiteralArgumentBuilder<CommandSource> literal(
            @NotNull String name) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkArgument(name.indexOf(' ') == -1, "the argument name cannot contain spaces");
        return LiteralArgumentBuilder.literal(name);
    }
    public static <T> RequiredArgumentBuilder<CommandSource, ?> argument(
            @NotNull String name,
            @NotNull ArgumentType<T> argumentType) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(argumentType, "argument type");
        if (argumentType instanceof CustomArgumentType<T, ?> customArgumentType)
            return customArgument(name, customArgumentType);
        return RequiredArgumentBuilder.argument(name, argumentType);
    }
    public static <S, T, N, J extends RequiredArgumentBuilder<S, J>>RequiredArgumentBuilder<S, J> customArgument(
            @NotNull String name,
            @NotNull CustomArgumentType<T, N> argumentType) {
        RequiredArgumentBuilder<S, J> argumentBuilder = Unsafe.createInstance(customRequiredArgumentBuilderClass);
        customRequiredArgumentBuilderCtor.invoke(argumentBuilder, name, argumentType);
        return argumentBuilder;
    }
}
