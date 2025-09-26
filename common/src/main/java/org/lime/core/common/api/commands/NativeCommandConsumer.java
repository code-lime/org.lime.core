package org.lime.core.common.api.commands;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Range;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.api.commands.brigadier.arguments.RepeatableArgumentBuilder;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.execute.Action1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        Audience audience(Sender sender);

        Message tooltip(Component component);
        <T, N> ArgumentType<T> argument(BaseMappedArgument<T, N> mappedArgument);

        Predicate<Sender> operator();
        LiteralArgumentBuilder<Sender> literal(String literal);
        <T> RequiredArgumentBuilder<Sender, T> argument(String key, ArgumentType<T> argumentType);
        default <T, N> RequiredArgumentBuilder<Sender, T> argument(String key, BaseMappedArgument<T, N> mappedArgument) {
            return argument(key, argument(mappedArgument));
        }
        <T> RepeatableArgumentBuilder<Sender, T> repeatable(String key, ArgumentType<T> argumentType);
        default <T, N> RepeatableArgumentBuilder<Sender, T> repeatable(String key, BaseMappedArgument<T, N> mappedArgument) {
            return repeatable(key, argument(mappedArgument));
        }
        <T> RepeatableArgumentBuilder<Sender, T> repeatable(String key, @Range(from = 1, to = RepeatableArgumentBuilder.LIMIT_MAX_COUNT) int maxCount, ArgumentType<T> argumentType);
        default <T, N> RepeatableArgumentBuilder<Sender, T> repeatable(String key, @Range(from = 1, to = RepeatableArgumentBuilder.LIMIT_MAX_COUNT) int maxCount, BaseMappedArgument<T, N> mappedArgument) {
            return repeatable(key, maxCount, argument(mappedArgument));
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
