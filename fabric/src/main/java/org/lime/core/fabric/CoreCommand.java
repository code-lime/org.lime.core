package org.lime.core.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.kyori.adventure.platform.fabric.impl.AdventureCommandSourceStackInternal;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.lime.core.common.api.commands.BaseCoreCommand;
import org.lime.core.common.api.commands.BaseCoreCommandRegister;
import org.lime.core.common.api.commands.CommandAction;

import java.util.Collection;

public abstract class CoreCommand<T extends CommandSource, Self>
        extends BaseCoreCommand<T, CommandSourceStack, CoreCommand<T, Self>> {
    CoreCommand(String cmd, Class<T> sender) {
        super(cmd, sender);
    }

    @Override
    public CommandAction<T, CommandSourceStack, Boolean> operator() {
        return (_,source,_) -> source.hasPermission(3);
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
            instance.server()
                    .getCommands()
                    .getDispatcher()
                    .register(build());
        }

        LiteralArgumentBuilder<CommandSourceStack> build() {
            CommandAction<CommandSource, CommandSourceStack, Boolean> check = this.check == null ? (_, _, _) -> true : this.check;
            CommandAction<CommandSource, CommandSourceStack, Boolean> execute = combine(check, executor);

            return Commands.literal(cmd)
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                            .suggests((context, suggestionsBuilder) -> {
                                String[] args = StringUtils.split(suggestionsBuilder.getRemaining());
                                if (suggestionsBuilder.getRemaining().endsWith(" "))
                                    args = ArrayUtils.add(args, "");

                                if (args.length == 0)
                                    args = new String[]{""};

                                final SuggestionsBuilder offsetSuggestionsBuilder = suggestionsBuilder.createOffset(suggestionsBuilder.getInput().lastIndexOf(' ') + 1);

                                CommandSourceStack source = context.getSource();
                                CommandSource src = ((AdventureCommandSourceStackInternal)source).adventure$source();
                                if (tab == null || this.sender.isInstance(src))
                                    return offsetSuggestionsBuilder.buildFuture();
                                if (!check.action(src, source, args))
                                    return offsetSuggestionsBuilder.buildFuture();
                                Collection<String> tabs = tab.action(src, source, args);
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
                                CommandSourceStack source = stack.getSource();
                                CommandSource src = ((AdventureCommandSourceStackInternal)source).adventure$source();
                                if (this.sender.isInstance(src) && !execute.action(src, source, StringUtils.split(stack.getArgument("args", String.class), ' ')) && usage != null)
                                    source.sendFailure(Component.literal(usage));
                                return Command.SINGLE_SUCCESS;
                            }))
                    .executes((stack) -> {
                        CommandSourceStack source = stack.getSource();
                        CommandSource src = ((AdventureCommandSourceStackInternal)source).adventure$source();
                        if (this.sender.isInstance(src) && !execute.action(src, source, new String[0]) && usage != null)
                            source.sendFailure(Component.literal(usage));
                        return Command.SINGLE_SUCCESS;
                    });
        }
    }
}
