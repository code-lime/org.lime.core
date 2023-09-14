package org.lime.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;
import org.lime.system.execute.*;

import java.lang.reflect.AccessibleObject;
import java.util.*;
import java.util.stream.Collectors;

public final class CoreCommand {
    public final String cmd;
    public final String description;
    public final String usage;
    public final CommandExecutor executor;
    public final CommandExecutor check;
    public final TabCompleter tab;

    private CoreCommand(String cmd, CommandExecutor executor, CommandExecutor check, TabCompleter tab, String description, String usage) {
        this.cmd = cmd;
        this.description = description;
        this.usage = usage;
        this.executor = executor;
        this.check = check;
        this.tab = tab;
    }

    public static CoreCommand create(String cmd) {
        return new CoreCommand(cmd, null, null, null, null, null);
    }

    public CoreCommand withTab(TabCompleter tab) {
        return new CoreCommand(cmd, executor, check, tab, description, usage);
    }

    public CoreCommand withTab(Func2<CommandSender, String[], Collection<String>> tab) {
        return withTab((v0, v1, v2, v3) -> new ArrayList<>(tab.invoke(v0, v3)));
    }

    public CoreCommand withTab(Func1<CommandSender, Collection<String>> tab) {
        return withTab((v0, v1, v2, v3) -> new ArrayList<>(tab.invoke(v0)));
    }

    public CoreCommand withTab(Func0<Collection<String>> tab) {
        return withTab((v0, v1, v2, v3) -> new ArrayList<>(tab.invoke()));
    }

    public CoreCommand withTab(Collection<String> tab) {
        return withTab((v0, v1, v2, v3) -> new ArrayList<>(tab));
    }

    public CoreCommand withTab(String... tab) {
        return withTab(Arrays.asList(tab));
    }

    public CoreCommand withExecutor(CommandExecutor executor) {
        return new CoreCommand(cmd, executor, check, tab, description, usage);
    }

    public CoreCommand withExecutor(Func2<CommandSender, String[], Boolean> executor) {
        return withExecutor((v0, v1, v2, v3) -> executor.invoke(v0, v3));
    }

    public CoreCommand withExecutor(Func1<CommandSender, Boolean> executor) {
        return withExecutor((v0, v1, v2, v3) -> executor.invoke(v0));
    }

    public CoreCommand withExecutor(Func0<Boolean> executor) {
        return withExecutor((v0, v1, v2, v3) -> executor.invoke());
    }

    public CoreCommand withCheck(CommandExecutor check) {
        return new CoreCommand(cmd, executor, check, tab, description, usage);
    }

    public CoreCommand withCheck(Func2<CommandSender, String[], Boolean> check) {
        return withCheck((v0, v1, v2, v3) -> check.invoke(v0, v3));
    }

    public CoreCommand withCheck(Func1<CommandSender, Boolean> check) {
        return withCheck((v0, v1, v2, v3) -> check.invoke(v0));
    }

    public CoreCommand withCheck(Func0<Boolean> check) {
        return withCheck((v0, v1, v2, v3) -> check.invoke());
    }

    private static CommandExecutor combine(CommandExecutor executor1, CommandExecutor executor2) {
        return executor1 == null
                ? (executor2 == null
                ? (v0, v1, v2, v3) -> true
                : executor2
        )
                : (executor2 == null
                ? executor1
                : (v0, v1, v2, v3) -> executor1.onCommand(v0, v1, v2, v3) && executor2.onCommand(v0, v1, v2, v3)
        );
    }

    public CoreCommand addCheck(CommandExecutor check) {
        return new CoreCommand(cmd, executor, combine(this.check, check), tab, description, usage);
    }

    public CoreCommand addCheck(Func2<CommandSender, String[], Boolean> check) {
        return addCheck((v0, v1, v2, v3) -> check.invoke(v0, v3));
    }

    public CoreCommand addCheck(Func1<CommandSender, Boolean> check) {
        return addCheck((v0, v1, v2, v3) -> check.invoke(v0));
    }

    public CoreCommand addCheck(Func0<Boolean> check) {
        return addCheck((v0, v1, v2, v3) -> check.invoke());
    }

    public CoreCommand addCheck(String... permissions) {
        return addCheck((s, v1, v2, v3) -> {
            for (String perm : permissions) {
                if (s.hasPermission(perm))
                    return true;
            }
            return false;
        });
    }

    public CoreCommand withDescription(String description) {
        return new CoreCommand(cmd, executor, check, tab, description, usage);
    }

    public CoreCommand withUsage(String usage) {
        return new CoreCommand(cmd, executor, check, tab, description, usage);
    }

    private static <T extends AccessibleObject> T setAccessible(T obj) {
        obj.setAccessible(true);
        return obj;
    }

    Command build(Plugin plugin) {
        try {
            PluginCommand command = setAccessible(PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class)).newInstance(cmd, plugin);
            CommandExecutor _check = check == null ? (v0, v1, v2, v3) -> true : check;
            if (executor != null) command.setExecutor(combine(_check, executor));
            if (tab != null) command.setTabCompleter((sender, cmd, alias, args) -> {
                if (args.length == 0 || !_check.onCommand(sender, cmd, alias, args)) return Collections.emptyList();
                String filter = args[args.length - 1].toLowerCase();
                List<String> tabs = tab.onTabComplete(sender, cmd, alias, args);
                return tabs == null ? Collections.emptyList() : tabs.stream().filter(curr -> curr.toLowerCase().contains(filter)).collect(Collectors.toList());
            });
            if (description != null) command.setDescription(description);
            if (usage != null) command.setUsage(usage);
            Bukkit.getCommandMap().register(plugin.getName(), command);
            return command;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
