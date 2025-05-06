package org.lime.core.paper;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.lime.core.common.api.commands.BaseCoreCommand;
import org.lime.core.common.api.commands.BaseCoreCommandRegister;
import org.lime.core.common.api.commands.CommandAction;
import org.lime.core.common.reflection.Reflection;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public abstract class CoreCommand<T extends CommandSender, Self>
        extends BaseCoreCommand<T, Command, CoreCommand<T, Self>> {
    CoreCommand(String cmd, Class<T> sender) {
        super(cmd, sender);
    }

    @Override
    public CommandAction<T, Command, Boolean> operator() {
        return (sender,_,_) -> sender.isOp();
    }

    public static class Register
            extends CoreCommand<CommandSender, Register>
            implements BaseCoreCommandRegister<CoreInstancePlugin.CoreInstance, Register> {
        @Override
        protected Register self() {
            return this;
        }

        Register(String cmd) {
            super(cmd, CommandSender.class);
        }

        public static Register create(String cmd) {
            return new Register(cmd);
        }

        @Override
        public void register(CoreInstancePlugin.CoreInstance instance) {
            Plugin plugin = instance.plugin();
            Bukkit.getCommandMap().register(plugin.getName(), build(plugin));
        }

        Command build(Plugin plugin) {
            CommandAction<CommandSender, Command, Boolean> check = this.check == null ? (_, _, _) -> true : this.check;
            CommandAction<CommandSender, Command, Boolean> execute = combine(check, executor);

            try {
                PluginCommand command = Reflection.access(PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class)).newInstance(cmd, plugin);
                command.setExecutor((sender1, command1, _, args) -> execute.action(sender1, command1, args));
                if (tab != null) command.setTabCompleter((sender, cmd, _, args) -> {
                    if (args.length == 0 || !check.action(sender, cmd, args)) return Collections.emptyList();
                    String filter = args[args.length - 1].toLowerCase();
                    Collection<String> tabs = tab.action(sender, cmd, args);
                    return tabs == null
                            ? Collections.emptyList()
                            : tabs.stream().filter(curr -> curr.toLowerCase().contains(filter)).collect(Collectors.toList());
                });
                if (description != null) command.setDescription(description);
                if (usage != null) command.setUsage(usage);
                return command;
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
