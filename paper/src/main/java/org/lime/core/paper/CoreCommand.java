package org.lime.core.paper;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.api.commands.BaseCoreCommand;
import org.lime.core.common.api.commands.BaseCoreCommandRegister;
import org.lime.core.common.api.commands.CommandAction;
import org.lime.core.common.reflection.Reflection;
import org.lime.core.common.system.Lazy;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class CoreCommand<T extends CommandSender, Self>
        extends BaseCoreCommand<T, CommandSourceStack, CoreCommand<T, Self>> {
    CoreCommand(String cmd, Class<T> sender) {
        super(cmd, sender);
    }

    @Override
    public CommandAction<T, CommandSourceStack, Boolean> operator() {
        return (sender,_,_) -> sender.isOp();
    }

    public static class Register
            extends CoreCommand<CommandSender, Register>
            implements BaseCoreCommandRegister<CoreInstancePlugin.CoreInstance> {
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
            if (nativeCommand != null) {
                LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.literal(cmd);
                nativeCommand.invoke(root);
                LiteralCommandNode<CommandSourceStack> rootNode = root.build();
                plugin.getLifecycleManager()
                        .registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands
                                .registrar()
                                .register(rootNode, this.description));
            } else {
                Bukkit.getCommandMap().register(plugin.getName(), build(plugin));
            }
        }

        private record SimpleCommandSourceStack(
                Lazy<Location> location,
                CommandSender sender,
                @Nullable Entity executor)
                implements CommandSourceStack {
            @Override
            public @NotNull Location getLocation() {
                return location.value();
            }
            @Override
            public @NotNull CommandSender getSender() {
                return sender;
            }
            @Override
            public @Nullable Entity getExecutor() {
                return executor;
            }

            @Override
            public CommandSourceStack withLocation(Location location) {
                return new SimpleCommandSourceStack(Lazy.of(location), sender, executor);
            }
            @Override
            public CommandSourceStack withExecutor(Entity executor) {
                return new SimpleCommandSourceStack(location, sender, executor);
            }

            public static SimpleCommandSourceStack of(CommandSender sender) {
                return new SimpleCommandSourceStack(Lazy.of(() -> switch (sender) {
                    case BlockCommandSender blockCommandSender -> blockCommandSender.getBlock().getLocation();
                    case Entity entity -> entity.getLocation();
                    default -> new Location(Bukkit.getWorlds().getFirst(), 0, 0, 0);
                }), sender, sender instanceof Entity executor ? executor : null);
            }
        }

        public Command build(Plugin plugin) {
            CommandAction<CommandSender, CommandSourceStack, Boolean> check = this.check == null ? (_, _, _) -> true : this.check;
            CommandAction<CommandSender, CommandSourceStack, Boolean> execute = combine(check, executor);

            try {
                PluginCommand command = Reflection.access(PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class)).newInstance(cmd, plugin);
                command.setExecutor((sender1, _, _, args) -> execute.action(sender1, SimpleCommandSourceStack.of(sender1), args));
                if (tab != null) command.setTabCompleter((sender, _, _, args) -> {
                    CommandSourceStack stack = SimpleCommandSourceStack.of(sender);
                    if (args.length == 0 || !check.action(sender, stack, args)) return Collections.emptyList();
                    String filter = args[args.length - 1].toLowerCase();
                    Collection<String> tabs = tab.action(sender, stack, args);
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
