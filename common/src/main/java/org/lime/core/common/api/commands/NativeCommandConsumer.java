package org.lime.core.common.api.commands;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.commands.brigadier.arguments.BaseMappedArgument;
import org.lime.core.common.utils.system.execute.Action1;

import java.util.ArrayList;
import java.util.List;

public interface NativeCommandConsumer<Sender, Builder extends NativeCommandConsumer.Register<Sender>>
        extends CommandConsumer<Builder> {
    interface Register<Sender> {
        void register(LiteralArgumentBuilder<Sender> node, String command, List<String> aliases, @Nullable String description);
    }
    interface Factory<Sender, Builder extends Register<Sender>> {
        Class<Builder> builderClass();

        Message tooltip(Component component);
        <T, N> ArgumentType<T> argument(BaseMappedArgument<T, N> mappedArgument);

        default NativeCommandConsumer<Sender, Builder> of(
                List<String> aliases,
                @Nullable String description,
                Action1<LiteralArgumentBuilder<Sender>> configure) {
            return new NativeCommandConsumer<>() {
                @Override
                public void apply(Builder builder) {
                    LiteralArgumentBuilder<Sender> root = null;
                    List<String> otherAliases = new ArrayList<>();
                    String rootAlias = null;
                    for (String alias : aliases) {
                        if (root == null) {
                            rootAlias = alias;
                            root = LiteralArgumentBuilder.literal(alias);
                        } else {
                            otherAliases.add(alias);
                        }
                    }
                    if (root == null || rootAlias == null)
                        return;
                    configure.invoke(root);
                    builder.register(root, rootAlias, otherAliases, description);
                }
                @Override
                public Class<Builder> builderClass() {
                    return Factory.this.builderClass();
                }
            };
        }
        default NativeCommandConsumer<Sender, Builder> of(
                String alias,
                @Nullable String description,
                Action1<LiteralArgumentBuilder<Sender>> configure) {
            return of(List.of(alias), description, configure);
        }
        default NativeCommandConsumer<Sender, Builder> of(
                List<String> aliases,
                Action1<LiteralArgumentBuilder<Sender>> configure) {
            return of(aliases, null, configure);
        }
        default NativeCommandConsumer<Sender, Builder> of(
                String alias,
                Action1<LiteralArgumentBuilder<Sender>> configure) {
            return of(List.of(alias), configure);
        }
    }
}
