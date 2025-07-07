package org.lime.core.velocity;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.lime.core.common.api.commands.BaseCoreCommand;
import org.lime.core.common.api.commands.BaseCoreCommandRegister;
import org.lime.core.common.api.commands.CommandAction;

import java.util.Collection;

public abstract class CoreCommand<T extends CommandSource, Self extends CoreCommand<T, Self>>
        extends BaseCoreCommand<T, CommandSource, Self> {
    CoreCommand(String cmd, Class<T> sender) {
        super(cmd, sender, CommandSource.class);
    }

    @Override
    public CommandAction<T, CommandSource, Boolean> operator() {
        return (v0,source,v3) -> source.hasPermission("velocity.operator");
    }

    public static class Register
            extends CoreCommand<CommandSource, Register>
            implements BaseCoreCommandRegister<CoreInstance, Register> {
        @Override
        protected Register self() {
            return this;
        }

        Register(String cmd) {
            super(cmd, CommandSource.class);
        }

        public static Register create(String cmd) {
            return new Register(cmd);
        }

        @Override
        public void register(CoreInstance instance) {
            var commands = instance.server()
                    .getCommandManager();
            var meta = commands.metaBuilder(cmd)
                    .plugin(instance)
                    .build();
            commands.register(meta, new BrigadierCommand(build()));
        }

        public LiteralArgumentBuilder<CommandSource> build() {
            LiteralArgumentBuilder<CommandSource> root = LiteralArgumentBuilder.literal(cmd);

            if (nativeCommand != null) {
                nativeCommand.invoke(root);
                return root;
            }

            CommandAction<CommandSource, CommandSource, Boolean> check = this.check == null ? (v0, v1, v3) -> true : this.check;
            CommandAction<CommandSource, CommandSource, Boolean> execute = combine(check, executor);

            return root
                    .then(BrigadierCommand.requiredArgumentBuilder("args", StringArgumentType.greedyString())
                            .suggests((context, suggestionsBuilder) -> {
                                String[] args = StringUtils.split(suggestionsBuilder.getRemaining());
                                if (suggestionsBuilder.getRemaining().endsWith(" "))
                                    args = ArrayUtils.add(args, "");

                                if (args.length == 0)
                                    args = new String[]{""};

                                final SuggestionsBuilder offsetSuggestionsBuilder = suggestionsBuilder.createOffset(suggestionsBuilder.getInput().lastIndexOf(' ') + 1);

                                CommandSource source = context.getSource();
                                if (tab == null || !this.sender.isInstance(source))
                                    return offsetSuggestionsBuilder.buildFuture();
                                if (!check.action(source, source, args))
                                    return offsetSuggestionsBuilder.buildFuture();
                                Collection<String> tabs = tab.action(source, source, args);
                                if (tabs == null || tabs.isEmpty())
                                    return offsetSuggestionsBuilder.buildFuture();
                                String filter = args[args.length - 1].toLowerCase();
                                tabs.forEach(curr -> {
                                    if (!curr.toLowerCase().contains(filter))
                                        return;
                                    offsetSuggestionsBuilder.suggest(curr);
                                });
                                return offsetSuggestionsBuilder.buildFuture();
                            })
                            .executes((stack) -> {
                                CommandSource source = stack.getSource();
                                if (this.sender.isInstance(source) && !execute.action(source, source, StringUtils.split(stack.getArgument("args", String.class), ' ')) && usage != null)
                                    source.sendMessage(Component.text(usage));
                                return Command.SINGLE_SUCCESS;
                            }))
                    .executes((stack) -> {
                        CommandSource source = stack.getSource();
                        if (this.sender.isInstance(source) && !execute.action(source, source, new String[0]) && usage != null)
                            source.sendMessage(Component.text(usage));
                        return Command.SINGLE_SUCCESS;
                    });
        }
    }
}
