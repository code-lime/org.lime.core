package org.lime.core.common.api.commands;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Range;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.api.commands.brigadier.arguments.RepeatableArgumentBuilder;
import org.lime.core.common.api.commands.brigadier.exceptions.SyntaxPredicate;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;
import org.lime.core.common.utils.execute.Func1;

import java.util.*;
import java.util.function.Predicate;

public interface NativeCommandConsumer<Sender, Register extends NativeCommandConsumer.NativeRegister<Sender>>
        extends CommandConsumer<Register> {
    interface NativeRegister<Sender>
            extends BaseRegister {
        record Command<Sender>(
                List<String> aliases,
                Action1<LiteralArgumentBuilder<Sender>> configure){}

        List<Command<Sender>> commands();

        default void append(List<String> aliases, Action1<LiteralArgumentBuilder<Sender>> configure) {
            commands().add(new Command<>(aliases, configure));
        }

        Disposable registerSingle(String alias, Action1<LiteralArgumentBuilder<Sender>> configure);
        @Override
        default Disposable apply() {
            Map<String, List<Command<Sender>>> commands = new HashMap<>();
            commands()
                    .forEach(command -> command.aliases
                            .forEach(alias -> commands
                                    .computeIfAbsent(alias, v -> new ArrayList<>())
                                    .add(command)));
            Disposable.Composite composite = Disposable.composite();
            commands.forEach((alias, list) -> composite.add(registerSingle(alias, v -> list
                    .forEach(item -> item.configure.invoke(v)))));
            return composite;
        }
    }
    interface Factory<Sender, Register extends NativeRegister<Sender>> {
        Class<Register> builderClass();
        Class<Sender> senderClass();
        CommandNode<Sender> root();

        Audience audience(Sender sender);

        Message tooltip(Component component);
        <T, N> ArgumentType<T> argument(BaseMappedArgument<T, N> mappedArgument);

        Predicate<Sender> operator();
        default LiteralArgumentBuilder<Sender> literal(String literal) {
            return LiteralArgumentBuilder.literal(literal);
        }
        default <T> RequiredArgumentBuilder<Sender, T> argument(String key, ArgumentType<T> argumentType) {
            return RequiredArgumentBuilder.argument(key, argumentType);
        }
        default <T, N> RequiredArgumentBuilder<Sender, T> argument(String key, BaseMappedArgument<T, N> mappedArgument) {
            return argument(key, argument(mappedArgument));
        }
        default <T> RepeatableArgumentBuilder<Sender, T> repeatable(String key, ArgumentType<T> argumentType) {
            return RepeatableArgumentBuilder.repeatable(this, key, argumentType);
        }
        default <T, N> RepeatableArgumentBuilder<Sender, T> repeatable(String key, BaseMappedArgument<T, N> mappedArgument) {
            return repeatable(key, argument(mappedArgument));
        }
        default <T> RepeatableArgumentBuilder<Sender, T> repeatable(String key, @Range(from = 1, to = RepeatableArgumentBuilder.LIMIT_MAX_COUNT) int maxCount, ArgumentType<T> argumentType) {
            return RepeatableArgumentBuilder.repeatable(this, key, maxCount, argumentType);
        }
        default <T, N> RepeatableArgumentBuilder<Sender, T> repeatable(String key, @Range(from = 1, to = RepeatableArgumentBuilder.LIMIT_MAX_COUNT) int maxCount, BaseMappedArgument<T, N> mappedArgument) {
            return repeatable(key, maxCount, argument(mappedArgument));
        }

        default <J extends ArgumentBuilder<Sender, J>>J condition(J builder, CommandNode<Sender> node, SyntaxPredicate<CommandContext<Sender>> accept) {
            return builder
                    .fork(node, ctx -> accept.test(ctx) ? Collections.singleton(ctx.getSource()) : Collections.emptyList())
                    .executes(ctx -> {
                        if (accept.test(ctx)) {
                            audience(ctx.getSource()).sendMessage(Component.translatable("commands.execute.conditional.pass"));
                            return 1;
                        } else {
                            throw new SimpleCommandExceptionType(tooltip(Component.translatable("commands.execute.conditional.fail"))).create();
                        }
                    });
        }
        default <J extends ArgumentBuilder<Sender, J>>J conditionRoot(J builder, SyntaxPredicate<CommandContext<Sender>> accept) {
            return condition(builder, root(), accept);
        }

        default NativeCommandConsumer<Sender, Register> of(
                List<String> aliases,
                Action1<LiteralArgumentBuilder<Sender>> configure) {
            return new NativeCommandConsumer<>() {
                @Override
                public void apply(Register register) {
                    if (aliases.isEmpty())
                        return;
                    register.append(aliases, configure);
                }
                @Override
                public Class<Register> registerClass() {
                    return Factory.this.builderClass();
                }
            };
        }
        default NativeCommandConsumer<Sender, Register> of(
                String alias,
                Action1<LiteralArgumentBuilder<Sender>> configure) {
            return of(List.of(alias), configure);
        }
    }
}
