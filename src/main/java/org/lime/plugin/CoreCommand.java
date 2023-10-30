package org.lime.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;
import org.lime.system.execute.*;

import java.lang.reflect.AccessibleObject;
import java.util.*;
import java.util.stream.Collectors;

public final class CoreCommand<T extends CommandSender> {
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

    public static CoreCommand<CommandSender> create(String cmd) { return new CoreCommand<>(cmd, null, null, null, null, null); }

    public CoreCommand<T> withTab(TabCompleter tab) { return new CoreCommand<>(cmd, executor, check, tab, description, usage); }
    public CoreCommand<T> withTab(Func2<T, String[], Collection<String>> tab) { return withTab((v0, v1, v2, v3) -> new ArrayList<>(tab.invoke((T)v0, v3))); }
    public CoreCommand<T> withTab(Func1<T, Collection<String>> tab) { return withTab((v0, v1, v2, v3) -> new ArrayList<>(tab.invoke((T)v0))); }
    public CoreCommand<T> withTab(Func0<Collection<String>> tab) { return withTab((v0, v1, v2, v3) -> new ArrayList<>(tab.invoke())); }
    public CoreCommand<T> withTab(Collection<String> tab) { return withTab((v0, v1, v2, v3) -> new ArrayList<>(tab)); }
    public CoreCommand<T> withTab(String... tab) { return withTab(Arrays.asList(tab)); }

    public CoreCommand<T> withExecutor(CommandExecutor executor) { return new CoreCommand<>(cmd, executor, check, tab, description, usage); }
    public CoreCommand<T> withExecutor(Func2<T, String[], Boolean> executor) { return withExecutor((v0, v1, v2, v3) -> executor.invoke((T)v0, v3)); }
    public CoreCommand<T> withExecutor(Func1<T, Boolean> executor) { return withExecutor((v0, v1, v2, v3) -> executor.invoke((T)v0)); }
    public CoreCommand<T> withExecutor(Func0<Boolean> executor) { return withExecutor((v0, v1, v2, v3) -> executor.invoke()); }

    public CoreCommand<T> withCheck(CommandExecutor check) { return new CoreCommand<>(cmd, executor, check, tab, description, usage); }
    public CoreCommand<T> withCheck(Func2<T, String[], Boolean> check) { return withCheck((v0, v1, v2, v3) -> check.invoke((T)v0, v3)); }
    public CoreCommand<T> withCheck(Func1<T, Boolean> check) { return withCheck((v0, v1, v2, v3) -> check.invoke((T)v0)); }
    public CoreCommand<T> withCheck(Func0<Boolean> check) { return withCheck((v0, v1, v2, v3) -> check.invoke()); }

    public CoreCommand<T> addCheck(CommandExecutor check) { return new CoreCommand<>(cmd, executor, combine(this.check, check), tab, description, usage); }
    public CoreCommand<T> addCheck(Func2<T, String[], Boolean> check) { return addCheck((v0, v1, v2, v3) -> check.invoke((T)v0, v3)); }
    public CoreCommand<T> addCheck(Func1<T, Boolean> check) { return addCheck((v0, v1, v2, v3) -> check.invoke((T)v0)); }
    public CoreCommand<T> addCheck(Func0<Boolean> check) { return addCheck((v0, v1, v2, v3) -> check.invoke()); }
    public CoreCommand<T> addCheck(String... permissions) {
        return addCheck((s, v1, v2, v3) -> {
            for (String perm : permissions) {
                if (s.hasPermission(perm))
                    return true;
            }
            return false;
        });
    }

    public <I extends T>CoreCommand<I> withCheckCast(Class<I> tClass) { return (CoreCommand<I>)withCheck(tClass::isInstance); }

    public CoreCommand<T> withDescription(String description) { return new CoreCommand<>(cmd, executor, check, tab, description, usage); }
    public CoreCommand<T> withUsage(String usage) { return new CoreCommand<>(cmd, executor, check, tab, description, usage); }

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
