package org.lime;

import com.google.common.collect.ImmutableList;

import com.google.common.collect.Streams;
import com.google.gson.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.java.PluginClassLoader;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.lime.invokable.IInvokable;
import org.lime.json.JsonObjectOptional;
import org.lime.timings.lib.MCTiming;
import org.lime.timings.lib.TimerTimings;
import org.lime.timings.lib.TimingManager;

import java.io.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class core extends JavaPlugin {
    @SuppressWarnings("all")
    public final static class element {
        public enum SortType {
            First(Double.NEGATIVE_INFINITY),
            Default(0),
            Last(Double.POSITIVE_INFINITY);

            private final double value;
            SortType(double value) { this.value = value; }
            public double getValue() { return value; }
        }
        public final Class<?> tClass;
        public final String name;
        public final Object instance;
        public final system.Action1<core> init;
        public final system.Action0 uninit;
        public final SortType sortType;
        public final boolean disable;

        public static class data<T> {
            private final Type type;
            private enum Type {
                Text(false),
                Json(false),
                None(false),
                Init(false);

                public final boolean init;
                Type(boolean init) { this.init = init; }
            }
            public final String name;
            public final String parent;
            public final String file;
            public final data<?> if_empty;
            public final system.Action1<T> invoke;
            public final system.Func0<T> def;

            public final system.Func1<String, T> read;
            public final system.Func1<T, String> write;

            private data(Type type, String file, String name, String parent, system.Action1<T> invoke, system.Func0<T> def, system.Func1<String, T> read, system.Func1<T, String> write, data<?> if_empty) {
                this.type = type;
                this.file = file;
                this.name = name;
                this.parent = parent;
                this.invoke = invoke;
                this.def = def;
                this.if_empty = if_empty;

                this.read = read;
                this.write = write;
            }

            private static <T>data<T> create(Type type, system.Func1<String, T> read, system.Func1<T, String> write) {
                return new data<>(type, null, null, null, null, null, read, write, null);
            }
            @SuppressWarnings("unchecked")
            public static <T extends JsonElement>data<T> json() { return create(Type.Json, text -> (T)system.json.parse(text), system::toFormat); }
            public static data<String> text() { return create(Type.Text, v -> v, v -> v); }
            private static data<Object> none() { return create(Type.None, null, null); }
            private static data<Object> init() { return create(Type.Init, null, null); }

            public data<T> withInvoke(system.Action1<T> invoke) {
                return new data<>(type, file, name, parent, invoke, def, read, write, if_empty);
            }
            public data<T> withDefault(system.Func0<T> def) {
                return new data<>(type, file, name, parent, invoke, def, read, write, if_empty);
            }
            public data<T> withDefault(T def) {
                return withDefault(() -> def);
            }
            public data<T> withParent(String parent) {
                return new data<>(type, file, name, parent, invoke, def, read, write, if_empty);
            }


            public data<T> orText(String file, system.Func1<data<String>, data<String>> builder) {
                return orFile(file, file, builder.invoke(data.text()));
            }
            public <T2 extends JsonElement>data<T> orConfig(String config, system.Func1<data<T2>, data<T2>> builder) {
                return orFile(config + ".json", config, builder.invoke(data.json()));
            }
            public data<T> orFile(String file, String name, data<?> data) {
                return new data<>(type, this.file, this.name, parent, invoke, def, read, write, data.withFile(file).withName(name));
            }

            private data<T> withFile(String file) {
                return new data<>(type, file, name, parent, invoke, def, read, write, if_empty);
            }
            private data<T> withName(String name) {
                return new data<>(type, file, name, parent, invoke, def, read, write, if_empty);
            }

            private void invokeRead(String text) {
                invoke.invoke(read.invoke(text));
            }
            private String getDefault() {
                return def == null ? "" : write.invoke(def.invoke());
            }
            private List<String> getFiles() {
                List<String> list = new ArrayList<>();
                list.add(name == null ? file : name);
                if (if_empty != null) list.addAll(if_empty.getFiles());
                return list;
            }

            private void read(core plugin, boolean update) {
                if (read == null || write == null) {
                    if (update || type.init) invoke.invoke(null);
                    return;
                }
                String[] split = file.split("\\.");
                String _file = split[0];
                String ext = "." + split[1];

                boolean isExist = plugin._existConfig(_file, ext);

                if (!isExist && if_empty != null) {
                    if_empty.read(plugin, update);
                    return;
                }

                if (parent == null) {
                    if (!isExist) plugin._writeAllConfig(_file, ext, getDefault());
                    invokeRead(plugin._readAllConfig(_file, ext));
                } else {
                    if (!isExist) plugin._writeAllConfig(_file, ext, "{}");
                    JsonObject base = system.json.parse(plugin._readAllConfig(_file, ext)).getAsJsonObject();
                    JsonElement data = base.has(parent) ? base.get(parent) : null;
                    if (type == Type.Json) {
                        if (data == null) {
                            base.add(parent, data = system.json.parse(getDefault()));
                            plugin._writeAllConfig(_file, ext, system.toFormat(base));
                        }
                        invokeRead(data.toString());
                    } else {
                        if (data == null) {
                            base.add(parent, data = new JsonPrimitive(getDefault()));
                            plugin._writeAllConfig(_file, ext, system.toFormat(base));
                        }
                        invokeRead(data.getAsString());
                    }
                }
            }
        }

        public final List<system.Func0<command>> commands;
        public final List<data<?>> config;
        public final List<Permission> permissions;

        private element(Class<?> tClass, String name, Object instance, system.Action1<core> init, system.Action0 uninit, List<data<?>> config, List<system.Func0<command>> commands, List<Permission> permissions, SortType sortType, boolean disable) {
            this.tClass = tClass;
            this.name = name;
            this.instance = instance;
            this.init = init;
            this.uninit = uninit;
            this.config = config;
            this.commands = commands;
            this.permissions = permissions;
            this.sortType = sortType;
            this.disable = disable;
        }

        public static element create(Class<?> tClass) {
            return new element(tClass, tClass.getSimpleName(), null, null, null, ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), SortType.Default, false);
        }
        public element withInstance() {
            try { return withInstance(tClass.getDeclaredConstructor().newInstance()); } catch (Exception e) { throw new IllegalArgumentException(e); }
        }
        public element withInstance(Object instance) {
            return new element(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable);
        }
        public element withInit(system.Action0 init) {
            return withInit(v -> init.invoke());
        }
        public element withInit(system.Action1<core> init) {
            return new element(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable);
        }
        public element addPermission(String permission) {
            return addPermission(new Permission(permission));
        }
        public element addPermission(Permission permission) {
            return addPermissions(permission);
        }
        public element addPermissions(String... permissions) {
            return addPermissions(Arrays.stream(permissions).map(Permission::new).toArray(Permission[]::new));
        }
        public element addPermissions(Permission... permissions) {
            return new element(tClass, name, instance, init, uninit, config, commands, ImmutableList.<Permission>builder().addAll(this.permissions).add(permissions).build(), sortType, disable);
        }
        public element addCommand(system.Func0<command> command) {
            return addCommands(command);
        }
        public element addCommand(String cmd, system.Func1<command, command> command) {
            return addCommands(() -> command.invoke(core.command.create(cmd)));
        }
        public element addCommands(system.Func0<command>... commands) {
            return new element(tClass, name, instance, init, uninit, config, ImmutableList.<system.Func0<command>>builder().addAll(this.commands).add(commands).build(), permissions, sortType, disable);
        }
        public element withUninit(system.Action0 uninit) {
            return new element(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable);
        }
        public element sortType(SortType sortType) {
            return new element(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable);
        }
        public element disable() {
            return new element(tClass, name, instance, init, uninit, config, commands, permissions, sortType, true);
        }

        public element addText(String file, system.Func1<data<String>, data<String>> builder) {
            return addFile(file, file, builder.invoke(data.text()));
        }
        public <T extends JsonElement>element addConfig(String config, system.Func1<data<T>, data<T>> builder) {
            return addFile(config + ".json", config, builder.invoke(data.json()));
        }
        public element addFile(String file, String name, data<?> data) {
            return new element(tClass, this.name, instance, init, uninit, ImmutableList.<element.data<?>>builder().addAll(this.config).add(data.withFile(file).withName(name)).build(), commands, permissions, sortType, disable);
        }
        public element addEmpty(String key, system.Action0 callback) {
            return addFile("", key, data.none().withInvoke(v -> callback.invoke()));
        }
        public element addEmptyInit(String key, system.Action0 callback) {
            return addFile("", key, data.init().withInvoke(v -> callback.invoke()));
        }
    }
    public final static class command {
        public final String cmd;
        public final String description;
        public final String usage;
        public final CommandExecutor executor;
        public final CommandExecutor check;
        public final TabCompleter tab;

        private command(String cmd, CommandExecutor executor, CommandExecutor check, TabCompleter tab, String description, String usage) {
            this.cmd = cmd;
            this.description = description;
            this.usage = usage;
            this.executor = executor;
            this.check = check;
            this.tab = tab;
        }

        public static command create(String cmd) {
            return new command(cmd, null, null, null, null, null);
        }

        public command withTab(TabCompleter tab) {
            return new command(cmd, executor, check, tab, description, usage);
        }
        public command withTab(system.Func2<CommandSender, String[], Collection<String>> tab) {
            return withTab((v0,v1,v2,v3) -> new ArrayList<>(tab.invoke(v0, v3)));
        }
        public command withTab(system.Func1<CommandSender, Collection<String>> tab) {
            return withTab((v0,v1,v2,v3) -> new ArrayList<>(tab.invoke(v0)));
        }
        public command withTab(system.Func0<Collection<String>> tab) {
            return withTab((v0,v1,v2,v3) -> new ArrayList<>(tab.invoke()));
        }
        public command withTab(Collection<String> tab) {
            return withTab((v0,v1,v2,v3) -> new ArrayList<>(tab));
        }
        public command withTab(String... tab) {
            return withTab(Arrays.asList(tab));
        }

        public command withExecutor(CommandExecutor executor) {
            return new command(cmd, executor, check, tab, description, usage);
        }
        public command withExecutor(system.Func2<CommandSender, String[], Boolean> executor) {
            return withExecutor((v0,v1,v2,v3) -> executor.invoke(v0, v3));
        }
        public command withExecutor(system.Func1<CommandSender, Boolean> executor) {
            return withExecutor((v0,v1,v2,v3) -> executor.invoke(v0));
        }
        public command withExecutor(system.Func0<Boolean> executor) {
            return withExecutor((v0,v1,v2,v3) -> executor.invoke());
        }

        public command withCheck(CommandExecutor check) {
            return new command(cmd, executor, check, tab, description, usage);
        }
        public command withCheck(system.Func2<CommandSender, String[], Boolean> check) {
            return withCheck((v0,v1,v2,v3) -> check.invoke(v0, v3));
        }
        public command withCheck(system.Func1<CommandSender, Boolean> check) {
            return withCheck((v0,v1,v2,v3) -> check.invoke(v0));
        }
        public command withCheck(system.Func0<Boolean> check) {
            return withCheck((v0,v1,v2,v3) -> check.invoke());
        }

        private static CommandExecutor combine(CommandExecutor executor1, CommandExecutor executor2) {
            /*if (executor1 == null) return executor2 == null ? (v0,v1,v2,v3) -> true : executor2;
            if (executor2 == null) return executor1;
            return (v0,v1,v2,v3) -> executor1.onCommand(v0,v1,v2,v3) && executor2.onCommand(v0,v1,v2,v3);*/
            return executor1 == null
                    ? (executor2 == null
                            ? (v0,v1,v2,v3) -> true
                            : executor2
                    )
                    : (executor2 == null
                            ? executor1
                            : (v0,v1,v2,v3) -> executor1.onCommand(v0,v1,v2,v3) && executor2.onCommand(v0,v1,v2,v3)
                    );
        }

        public command addCheck(CommandExecutor check) {
            return new command(cmd, executor, combine(this.check, check), tab, description, usage);
        }
        public command addCheck(system.Func2<CommandSender, String[], Boolean> check) {
            return addCheck((v0,v1,v2,v3) -> check.invoke(v0, v3));
        }
        public command addCheck(system.Func1<CommandSender, Boolean> check) {
            return addCheck((v0,v1,v2,v3) -> check.invoke(v0));
        }
        public command addCheck(system.Func0<Boolean> check) {
            return addCheck((v0,v1,v2,v3) -> check.invoke());
        }

        public command addCheck(String... permissions) {
            return addCheck((s,v1,v2,v3) -> {
                for (String perm : permissions) {
                    if (s.hasPermission(perm))
                        return true;
                }
                return false;
            });
        }

        public command withDescription(String description) {
            return new command(cmd, executor, check, tab, description, usage);
        }
        public command withUsage(String usage) {
            return new command(cmd, executor, check, tab, description, usage);
        }

        private static <T extends AccessibleObject>T setAccessible(T obj) {
            obj.setAccessible(true);
            return obj;
        }

        private Command build(Plugin plugin) {
            try {
                PluginCommand command = setAccessible(PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class)).newInstance(cmd, plugin);
                CommandExecutor _check = check == null ? (v0,v1,v2,v3) -> true : check;
                if (executor != null) command.setExecutor(combine(_check, executor));
                if (tab != null) command.setTabCompleter((sender, cmd, alias, args) -> {
                    if (args.length == 0 || !_check.onCommand(sender, cmd, alias, args)) return Collections.emptyList();
                    String filter = args[args.length - 1].toLowerCase();
                    List<String> tabs = tab.onTabComplete(sender, cmd, alias, args);
                    return tabs == null ? Collections.emptyList() : tabs.stream().filter(curr -> curr.toLowerCase().contains(filter)).collect(Collectors.toList());
                });
                if (description != null) command.setDescription(description);
                if (usage != null) command.setUsage(usage);
                Bukkit.getCommandMap().register(plugin.getDescription().getName(), command);
                return command;
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static core instance;
    public String getLogPrefix() {
        return "LIME:" + this.getName().toUpperCase();
    }
    public String getConfigFile() { return "plugins/" + this.getName().toLowerCase() + "/"; }
    private final List<system.Toast2<String, element>> elements = new ArrayList<>();
    private final HashMap<String, command> commands = new HashMap<>();
    private final List<LibraryClassLoader> libraries = new ArrayList<>();
    private final ConcurrentLinkedQueue<IInvokable> tickCalls = new ConcurrentLinkedQueue<>();

    public interface loadedElement {
        void cancel();
        Optional<element> element();
        String name();
        Class<?> type();

        static loadedElement disabled(element element) {
            return new loadedElement() {
                @Override public void cancel() { }
                @Override public Optional<element> element() { return Optional.empty(); }
                @Override public String name() { return element.name; }
                @Override public Class<?> type() { return element.tClass; }
            };
        }
    }
    public loadedElement add(element element) {
        if (element.disable) return loadedElement.disabled(element);
        system.Toast2<String, element> item = system.toast(element.name, element);
        elements.add(item);
        if (element.instance instanceof ICore icore) icore.core(this);
        return new loadedElement() {
            @Override public void cancel() {
                if (!elements.remove(item)) return;
                if (item.val1.uninit != null) item.val1.uninit.invoke();
            }
            @Override public Optional<element> element() {
                return Optional.of(element);
            }
            @Override public String name() { return element.name; }
            @Override public Class<?> type() { return element.tClass; }
        };
    }

    public Collection<String> getJarClassesNames() {
        if (this.getClassLoader() instanceof PluginClassLoader loader) {
            try (JarFile jar = new JarFile(this.getFile())) {
                return Streams.stream(jar.entries().asIterator())
                        .map(JarEntry::getName)
                        .filter(v -> v.endsWith(".class"))
                        .map(v -> v.substring(0, v.length() - 6).replace('/', '.'))
                        .toList();
            }
            catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return Collections.emptyList();
    }
    public List<loadedElement> addOther() {
        List<loadedElement> other = new ArrayList<>();
        if (this.getClassLoader() instanceof PluginClassLoader loader) {
            try (JarFile jar = new JarFile(this.getFile())) {
                jar.entries().asIterator().forEachRemaining(entry -> {
                    String class_name = entry.getName();
                    if (!class_name.endsWith(".class")) return;
                    class_name = class_name.substring(0, class_name.length() - 6).replace('/', '.');
                    try {
                        Class<?> tClass = loader.loadClass(class_name);
                        Method method = tClass.getDeclaredMethod("create");
                        if (!Modifier.isStatic(method.getModifiers())) return;
                        if (method.getReturnType() != element.class) return;
                        if (elements.stream().anyMatch(v -> v.val1.tClass == tClass)) return;
                        other.add(add((element)method.invoke(null)));
                    } catch (Throwable ignored) { }
                });
            }
            catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return other;
    }

    public void library(File... jars) {
        LibraryClassLoader loader = new LibraryClassLoader(this, Arrays.asList(jars));
        loader.load();
        libraries.add(loader);
        _logOP("Library "+Arrays.stream(jars).map(v -> v.getAbsolutePath()).collect(Collectors.joining(" & "))+" loaded!");
    }
    public void library(String... jars) {
        library(Arrays.stream(jars).map(this::_getConfigFile).toArray(File[]::new));
    }
    public void add(command command) {
        commands.put(command.cmd, command);
    }
    public void add(String cmd, system.Func1<command, command> builder) {
        add(builder.invoke(command.create(cmd)));
    }

    protected void init() {}

    private static <T>void invokeList(Collection<T> list, system.Action2<T, String> invoke) {
        int size = list.size();
        int i = 0;
        for (T item : list) {
            invoke.invoke(item, "[" + StringUtils.leftPad(String.valueOf(i*100 / size), 3, '*').replace("*", "...") + "%]");
            i++;
        }
    }
    private static <K, V>void invokeList(Map<K, V> list, system.Action3<K, V, String> invoke) {
        invokeList(list.entrySet(), (v,pref) -> invoke.invoke(v.getKey(), v.getValue(), pref));
    }

    public interface ICore {
        void core(core base_core);
        core core();

        public static class Abstract implements ICore {
            public core base_core;
            @Override public void core(core base_core) { this.base_core = base_core; }
            @Override public core core() { return this.base_core; }
        }
    }
    public interface IUpdateConfig {
        default void updateConfigSync() {}
        default void updateConfigAsync(Collection<String> files, system.Action0 updated) {}
    }

    private Optional<IUpdateConfig> config() {
        return Stream.concat(Stream.of(this), elements.stream().map(v -> v.val1.instance))
                .map(v -> v instanceof IUpdateConfig config ? config : null)
                .filter(v -> v != null)
                .findFirst();
    }

    private void init_core() {
        add("update.data", cmd -> cmd.withCheck(ServerOperator::isOp).withTab((sender, args) -> {
            switch (args.length) {
                case 1: return Arrays.stream(Bukkit.getPluginManager().getPlugins()).filter(plugin -> plugin instanceof core).map(Plugin::getName).collect(Collectors.toList());
                default: {
                    Plugin plugin = Bukkit.getPluginManager().getPlugin(args[0]);
                    if (!(plugin instanceof core)) break;
                    return ((core)plugin).elements.stream().flatMap(v -> v.val1.config.stream().flatMap(_v -> _v.getFiles().stream())).collect(Collectors.toList());
                }
            }
            return Collections.emptyList();
        }).withExecutor((sender, args) -> {
            if (args.length < 2) return false;
            Plugin plugin = Bukkit.getPluginManager().getPlugin(args[0]);
            if (!(plugin instanceof core)) return false;
            core _core = (core)plugin;
            List<system.Toast2<String, element>> elements = _core.elements;
            Collection<String> files = Arrays.stream(args).skip(1).collect(Collectors.toList());
            Set<String> _files = new HashSet<>();
            files.forEach(file -> elements.forEach(element -> element.val1.config.forEach(data -> {
                if (!data.getFiles().contains(file)) return;
                _files.addAll(data.getFiles());
            })));
            _core._logOP("Update files: " + String.join(" & ", _files));
            try {
                _core.config().ifPresent(config -> config.updateConfigAsync(_files, () -> {
                    try {
                        files.forEach(file -> elements.forEach(element -> element.val1.config.forEach(data -> {
                            if (!data.getFiles().contains(file)) return;
                            data.read(_core, true);
                        })));
                    } catch (Exception e) {
                        _core._logStackTrace(e);
                    }
                }));
            } catch (Exception e) {
                _core._logStackTrace(e);
                return true;
            }
            _core._logOP("Updated!");
            return true;
        }));
        add("update.class", cmd -> cmd.withCheck(ServerOperator::isOp).withTab((sender, args) -> {
            if (args.length == 1) return Arrays.stream(Bukkit.getPluginManager().getPlugins()).map(Plugin::getName).collect(Collectors.toList());
            Plugin plugin = Bukkit.getPluginManager().getPlugin(args[0]);
            if (plugin == null) return Collections.emptyList();
            List<PluginClassLoader> loaders = reflection.getField(JavaPluginLoader.class, "loaders", plugin.getPluginLoader());
            PluginClassLoader loader = loaders.stream().filter(v -> plugin.equals(v.getPlugin())).findFirst().orElse(null);
            Map<String, Class<?>> classes = reflection.getField(PluginClassLoader.class, "classes", loader);
            return new ArrayList<>(classes.keySet());
        }).withExecutor((sender, args) -> {
            if (args.length < 2) return false;
            Plugin plugin = Bukkit.getPluginManager().getPlugin(args[0]);
            if (plugin == null) return true;
            List<PluginClassLoader> loaders = reflection.getField(JavaPluginLoader.class, "loaders", plugin.getPluginLoader());
            PluginClassLoader loader = loaders.stream().filter(v -> plugin.equals(v.getPlugin())).findFirst().orElse(null);
            Map<String, Class<?>> classes = reflection.getField(PluginClassLoader.class, "classes", loader);
            String className = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
            Class<?> classT = classes.getOrDefault(className, null);
            if (classT == null) return true;
            Class<?> classNew;
            try {
                classNew = new ClassLoader() {
                    @Override public Class<?> loadClass(String name) throws ClassNotFoundException {
                        if (name != null) return getParent().loadClass(name);
                        try {
                            InputStream is = loader.getResourceAsStream(className.replace(".", "/") + ".class");
                            byte[] buf = new byte[10000];
                            int len = is.read(buf);
                            return defineClass(name, buf, 0, len);
                        } catch (IOException e) {
                            throw new ClassNotFoundException("", e);
                        }
                    }
                }.loadClass(null);
            } catch (Exception e) {
                core.instance._logStackTrace(e);
                return true;
            }
            core.instance._logOP("Updated class: " + classNew.getName());
            /*if (args.length < 2) return false;
            Plugin plugin = Bukkit.getPluginManager().getPlugin(args[0]);
            if (!(plugin instanceof core)) return false;
            core _core = (core)plugin;
            List<system.Toast2<String, element>> elements = _core.elements;
            Collection<String> files = Arrays.stream(args).skip(1).collect(Collectors.toList());
            Set<String> _files = new HashSet<>();
            files.forEach(file -> elements.forEach(element -> element.val1.config.forEach(data -> {
                if (!data.getKey().equals(file)) return;
                _files.add(data.file);
            })));
            _core._LogOP("Update files: " + String.join(" & ", _files));
            try {
                _core.updateConfigAsync(_files, () -> {
                    files.forEach(file -> elements.forEach(element -> element.val1.config.forEach(data -> {
                        if (!data.getKey().equals(file)) return;
                        data.read(_core, true);
                    })));
                });
            } catch (Exception e) {
                _core._LogStackTrace(e);
                return true;
                //throw new IllegalArgumentException(e);
            }
            _core._LogOP("Updated!");*/
            return true;
        }));
        commands.forEach((command, cmd) -> Bukkit.getCommandMap().register(this.getName(), cmd.build(this)));
        _repeat(system::tryClearCompare, 60);
        org.lime.reflection.init();
    }

    private TimingManager timingManager;
    @Override public void onEnable() {
        timingManager = TimingManager.of(this);
        if (instance == null) {
            instance = this;
            init_core();
            return;
        }

        try { getClass().getDeclaredField("_plugin").set(null, this); } catch (Exception ignored) { }

        File dir = _getConfigFile("");
        if (!dir.exists()) dir.mkdir();

        try {
            init();
            config().ifPresent(core.IUpdateConfig::updateConfigSync);

            _logOP("[............] Start load elements...");
            invokeList(elements.stream().sorted(Comparator.comparingDouble(v -> v.val1.sortType.getValue())).collect(Collectors.toList()), (item, prefix) -> {
                _logOP(prefix + " Load " + item.val0 + "...");
                if (item.val1.instance != null) {
                    if (item.val1.instance instanceof Listener) Bukkit.getPluginManager().registerEvents((Listener)item.val1.instance, this);
                }
                if (item.val1.init != null) item.val1.init.invoke(this);
                item.val1.config.forEach(data -> data.read(this, false));
                item.val1.commands.forEach(_cmd -> {
                    command cmd = _cmd.invoke();
                    commands.put(cmd.cmd, cmd);
                });
                PluginManager manager = Bukkit.getPluginManager();
                item.val1.permissions.forEach(_perm -> {
                    manager.removePermission(_perm);
                    manager.addPermission(_perm);
                });
            });
            _logOP("[100%] Loaded!");

            _logOP("[............] Start load commands...");
            invokeList(commands, (name, cmd, prefix) -> {
                _logOP(prefix + " Load command " + name + "...");
                Bukkit.getCommandMap().register(this.getName(), cmd.build(this));
            });
            _logOP("[100%] Loaded!");

            _repeatTicks(this::invokableTick, 1);
        } catch (Exception e) {
            _logStackTrace(e);
        }
    }
    public void invokableTick() {
        tickCalls.removeIf(v -> {
            try { return v.tryRemoveInvoke(); }
            catch (Throwable e) { _logStackTrace(e); }
            return true;
        });
    }
    @Override public void onDisable() {
        elements.forEach(item -> { if (item.val1.uninit != null) item.val1.uninit.invoke(); });
        libraries.forEach(lib -> lib.unload(true));
    }

    public static class ITimers {
        public enum TimerType {
            StaticCore,
            TimerBuilder
        }
        public interface IRunnable extends Runnable { }
        public static synchronized BukkitTask runTaskLater(IRunnable callback, core plugin, long delay, TimerType type) {
            if (!plugin.isEnabled()) {
                plugin._logOP("Can't run timer. Plugin is disable");
                return null;
            }
            return TimerTimings.of(Bukkit.getScheduler().runTaskLater(plugin, callback, delay), type);
        }
        public static synchronized BukkitTask runTaskTimer(IRunnable callback, core plugin, long wait, long delay, TimerType type) {
            if (!plugin.isEnabled()) {
                plugin._logOP("Can't run timer. Plugin is disable");
                return null;
            }
            return TimerTimings.of(Bukkit.getScheduler().runTaskTimer(plugin, callback, wait, delay), type);
        }
        public static synchronized BukkitTask runTaskLaterAsynchronously(IRunnable callback, core plugin, long delay, TimerType type) {
            if (!plugin.isEnabled()) {
                plugin._logOP("Can't run timer. Plugin is disable");
                return null;
            }
            return TimerTimings.of(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, callback, delay), type);
        }
        public static synchronized BukkitTask runTaskTimerAsynchronously(IRunnable callback, core plugin, long wait, long delay, TimerType type) {
            if (!plugin.isEnabled()) {
                plugin._logOP("Can't run timer. Plugin is disable");
                return null;
            }
            return TimerTimings.of(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, callback, wait, delay), type);
        }
    }

    public void _logToFile(String key, String text) {
        final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        String time = formatter.format(calendar.getTime());

        if (text.contains("{time}"))
        {
            final DateFormat time_formatter = new SimpleDateFormat("HH:mm:ss");
            calendar.setTimeInMillis(System.currentTimeMillis());
            text = text.replace("{time}", time_formatter.format(calendar.getTime()));
        }

        try {
            String path = "logs/lime/" + key + "/";
            File logs = new File(path);
            if (!logs.exists()) logs.mkdirs();

            File myObj = new File(path + key + "-" + time + ".log");
            if (!myObj.exists()) myObj.createNewFile();

            FileWriter myWriter = new FileWriter(myObj, true);
            myWriter.write(text + "\r\n");
            myWriter.close();
        } catch (FileNotFoundException e) {
            _log("An error occurred.");
            _log(e.getMessage());
        } catch (Exception e)
        {
            _log(e.getMessage());
        }
    }
    public void _log(String log) {
        Bukkit.getLogger().warning("["+getLogPrefix()+"] " + log);
    }
    public void _logAdmin(String log) {
        _logToFile("log_admin", "[{time}] " + log);
        _log(log);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("["+getLogPrefix()+"] " + log));
    }
    public void _logConsole(String log) {
        _logToFile("log_admin", "[{time}] " + log);
        _log(log);
    }
    public void _logOP(String log) {
        _logToFile("log_admin", "[{time}] " + log);
        _log(log);
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.isOp()) return;
            p.sendMessage(Component.text("["+getLogPrefix()+"] ").color(NamedTextColor.YELLOW).append(Component.text(log).color(NamedTextColor.WHITE)));
        });
    }
    public void _logOP(Component log) {
        String legacy_log = LegacyComponentSerializer.legacySection().serialize(log);
        _logToFile("log_admin", "[{time}] " + legacy_log);
        _log(legacy_log);
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.isOp()) return;
            p.sendMessage(Component.text("["+getLogPrefix()+"] ").color(NamedTextColor.YELLOW).append(Component.empty().append(log).color(NamedTextColor.WHITE)));
        });
    }
    public void _logWithoutPrefix(String log) {
        Bukkit.getLogger().warning(log);
    }

    public static class TimerBuilder {
        private final core plugin;
        private final ITimers.IRunnable callback;
        private final ITimers.IRunnable next;
        private final long wait;
        private final long loop;
        private final boolean async;

        private TimerBuilder(core plugin, ITimers.IRunnable callback, ITimers.IRunnable next, long wait, long loop, boolean async) {
            this.plugin = plugin;
            this.callback = callback;
            this.next = next;
            this.wait = Math.max(wait, 1);
            this.loop = Math.max(loop, 1);
            this.async = async;
        }
        public static TimerBuilder create(core plugin) {
            return new TimerBuilder(plugin, null, null, 1, 1, false);
        }

        public TimerBuilder withCallback(ITimers.IRunnable callback) {
            return new TimerBuilder(plugin, callback, next, wait, loop, async);
        }
        public TimerBuilder withCallback(system.Action1<Double> callback) {
            system.Toast1<Long> buff = system.toast(System.currentTimeMillis());
            return withCallback(() -> {
                long now = System.currentTimeMillis();
                callback.invoke((now - buff.val0) / (loop * 50.0));
                buff.val0 = now;
            });
        }
        public TimerBuilder withCallbackTicks(system.Action1<Long> callback) {
            system.Toast1<Long> buff = system.toast(System.currentTimeMillis());
            return withCallback(() -> {
                long now = System.currentTimeMillis();
                callback.invoke(now - buff.val0);
                buff.val0 = now;
            });
        }
        public TimerBuilder withNext(ITimers.IRunnable next) {
            return new TimerBuilder(plugin, callback, next, wait, loop, async);
        }
        public TimerBuilder withWait(double wait) {
            return withWaitTicks((long)(wait * 20));
        }
        public TimerBuilder withLoop(double loop) {
            return withLoopTicks((long)(loop * 20));
        }
        public TimerBuilder withWaitTicks(long wait) {
            return new TimerBuilder(plugin, callback, next, wait, loop, async);
        }
        public TimerBuilder withLoopTicks(long loop) {
            return new TimerBuilder(plugin, callback, next, wait, loop, async);
        }
        public TimerBuilder withAsync(boolean async) {
            return new TimerBuilder(plugin, callback, next, wait, loop, async);
        }

        public TimerBuilder setAsync() {
            return this.withAsync(true);
        }
        public TimerBuilder setSync() {
            return this.withAsync(false);
        }

        private enum type {
            sync_once(v -> !v.async && v.loop == -1, v -> ITimers.runTaskLater(v.callback, v.plugin, v.wait, ITimers.TimerType.TimerBuilder)),
            async_once(v -> v.async && v.loop == -1, v -> ITimers.runTaskLaterAsynchronously(v.callback, v.plugin, v.wait, ITimers.TimerType.TimerBuilder)),
            sync_repeat(v -> !v.async && v.loop != -1, v -> ITimers.runTaskTimer(v.callback, v.plugin, v.wait, v.loop, ITimers.TimerType.TimerBuilder)),
            async_repeat(v -> v.async && v.loop != -1, v -> ITimers.runTaskTimerAsynchronously(v.callback, v.plugin, v.wait, v.loop, ITimers.TimerType.TimerBuilder));

            public static BukkitTask run(TimerBuilder timer) {
                if (!timer.plugin.isEnabled()) {
                    timer.plugin._logOP("Can't run timer. Plugin is disable");
                    return null;
                }
                for (type t : type.values()) {
                    if (t.isDo.invoke(timer))
                        return t.run.invoke(timer);
                }
                throw new IllegalArgumentException("timer type error");
            }

            private final system.Func1<TimerBuilder, Boolean> isDo;
            private final system.Func1<TimerBuilder, BukkitTask> run;
            type(system.Func1<TimerBuilder, Boolean> isDo, system.Func1<TimerBuilder, BukkitTask> run) {
                this.isDo = isDo;
                this.run = run;
            }
        }

        public BukkitTask run() {
            return type.run(this);
        }
    }

    public TimerBuilder _timer() {
        return TimerBuilder.create(this);
    }
    public MCTiming _timing(String name) {
        return timingManager.of(name);
    }
    public void _timing(String name, system.Action0 callback) {
        try (MCTiming ignored = _timing(name)) {
            callback.invoke();
        }
    }

    public BukkitTask _nextTick(ITimers.IRunnable callback) {
        return ITimers.runTaskLater(callback, this, 0, ITimers.TimerType.StaticCore);
    }
    public BukkitTask _onceNoCheck(ITimers.IRunnable callback, double sec) {
        return ITimers.runTaskLater(callback, this, (long)(sec * 20), ITimers.TimerType.StaticCore);
    }
    public BukkitTask _once(ITimers.IRunnable callback, double sec) {
        return ITimers.runTaskLater(callback, this, (long)(sec * 20), ITimers.TimerType.StaticCore);
    }
    public BukkitTask _onceTicks(ITimers.IRunnable callback, long ticks) {
        return ITimers.runTaskLater(callback, this, ticks, ITimers.TimerType.StaticCore);
    }
    public BukkitTask _repeat(ITimers.IRunnable callback, double sec) {
        return ITimers.runTaskTimer(callback, this, (long)(sec * 20), (long)(sec * 20), ITimers.TimerType.StaticCore);
    }
    public BukkitTask _repeatTicks(ITimers.IRunnable callback, long ticks) {
        return ITimers.runTaskTimer(callback, this, ticks, ticks, ITimers.TimerType.StaticCore);
    }
    public BukkitTask _repeat(ITimers.IRunnable callback, double wait, double sec) {
        return ITimers.runTaskTimer(callback, this, (long)(wait * 20), (long)(sec * 20), ITimers.TimerType.StaticCore);
    }
    public BukkitTask _repeatTicks(ITimers.IRunnable callback, long wait, long ticks) {
        return ITimers.runTaskTimer(callback, this, wait, ticks, ITimers.TimerType.StaticCore);
    }
    public <T>void _repeat(T[] array, system.Action1<T> callback_part, system.Action0 callback_end, double sec, int inOneStep) {
        _ionce(array, array.length, 0, callback_part, callback_end, sec, inOneStep);
    }
    private <T>void _ionce(T[] array, int length, int index, system.Action1<T> callback, system.Action0 callback_end, double sec, int inOneStep) {
        if (index >= length) {
            callback_end.invoke();
            return;
        }
        _once(() -> {
            int maxIndex = index + inOneStep;
            _ionce(array, length, maxIndex, callback, callback_end, sec, inOneStep);
            maxIndex = Math.min(maxIndex, length);
            for (int i = index; i < maxIndex; i++)
                callback.invoke(array[i]);
        }, sec);
    }
    public BukkitTask _invokeAsync(system.Action0 async, ITimers.IRunnable nextSync) {
        if (!isEnabled()) {
            _logOP("Can't run timer. Plugin is disable");
            return null;
        }
        BukkitScheduler scheduler = Bukkit.getScheduler();
        return scheduler.runTaskAsynchronously(this, () -> {
            async.invoke();
            if (nextSync == null) return;
            scheduler.scheduleSyncDelayedTask(this, nextSync);
        });
    }
    public <T>BukkitTask _invokeAsync(system.Func0<T> async, system.Action1<T> nextSync) {
        if (!isEnabled()) {
            _logOP("Can't run timer. Plugin is disable");
            return null;
        }
        BukkitScheduler scheduler = Bukkit.getScheduler();
        return scheduler.runTaskAsynchronously(this, () -> {
            T obj = async.invoke();
            if (nextSync == null) return;
            scheduler.scheduleSyncDelayedTask(this, () -> nextSync.invoke(obj));
        });
    }
    public void _invokeSync(ITimers.IRunnable sync) {
        if (!isEnabled()) {
            _logOP("Can't run timer. Plugin is disable");
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, sync);
    }
    public void _invokable(IInvokable invokable) {
        tickCalls.add(invokable);
    }

    public JsonElement _combineJson(JsonElement first, JsonElement second, boolean array_join) {
        if (first.isJsonArray()) {
            JsonArray _first = first.getAsJsonArray();
            JsonArray _second = second.getAsJsonArray();
            if (array_join) _second.forEach(_first::add);
            else {
                int length = Math.min(_first.size(), _second.size());
                for (int i = 0; i < length; i++) _first.set(i, _combineJson(_first.get(i), _second.get(i), array_join));
            }
        }
        else if (first.isJsonObject()) {
            JsonObject _first = first.getAsJsonObject();
            second.getAsJsonObject().entrySet().forEach(kv -> {
                String key = kv.getKey();
                if (_first.has(key)) _first.add(key, _combineJson(_first.get(key), kv.getValue(), array_join));
                else _first.add(key, kv.getValue());
            });
        }
        return first;
    }
    public JsonElement _combineJson(JsonElement first, JsonElement second) {
        return _combineJson(first, second, true);
    }
    public JsonObject _combineParent(JsonObject json) {
        return _combineParent(json, false, true);
    }
    public JsonObject _combineParent(JsonObject json, boolean category, boolean array_join) {
        HashMap<String, JsonObject> parentObjects = new HashMap<>();
        JsonObject _new = new JsonObject();
        List<system.Toast2<String, String>> replaceList = new ArrayList<>();
        HashMap<String, JsonElement> replaceJsonList = new HashMap<>();
        if (json.has("DEFAULT_REPLACE")) {
            json.get("DEFAULT_REPLACE").getAsJsonObject().entrySet().forEach(kv -> {
                if (kv.getValue().isJsonArray()) replaceList.add(system.toast(kv.getKey(), Streams.stream(kv.getValue().getAsJsonArray().iterator()).map(JsonElement::getAsString).collect(Collectors.joining("\\n"))));
                else replaceList.add(system.toast(kv.getKey(), kv.getValue().getAsString()));
            });
            json.remove("DEFAULT_REPLACE");
        }
        if (json.has("DEFAULT_REPLACE_JSON")) {
            json.get("DEFAULT_REPLACE_JSON").getAsJsonObject().entrySet().forEach(kv -> {
                replaceJsonList.put(kv.getKey(), system.DeepCopy(kv.getValue()));
            });
            json.remove("DEFAULT_REPLACE_JSON");
        }
        if (json.has("DEFAULT_REPLACE_FILE")) {
            json.get("DEFAULT_REPLACE_FILE").getAsJsonObject().entrySet().forEach(kv -> {
                String file = kv.getValue().getAsString();
                List<String> dat = new ArrayList<>(Arrays.asList(file.split("\\.")));
                if (dat.size() == 1) dat.add("json");
                replaceList.add(system.toast(kv.getKey(), _readAllConfig(dat.get(0), "." + dat.get(1))));
            });
            json.remove("DEFAULT_REPLACE_FILE");
        }
        json = system.EditStringToObject(json, text -> {
            for (system.Toast2<String, String> replace : replaceList)
                text = text.replaceAll(
                        Pattern.quote("{" + replace.val0 + "}"),
                        Matcher.quoteReplacement(replace.val1));
            JsonElement replaceJson = replaceJsonList.getOrDefault(text, null);
            return replaceJson == null ? new JsonPrimitive(text) : replaceJson;
        });
        json = _executeJS(json);
        this._writeAllConfig("tmp.parent", system.toFormat(json));
        json.entrySet().forEach(kv -> {
            if (category) {
                kv.getValue().getAsJsonObject().entrySet().forEach(_kv -> {
                    JsonObject _obj = _combineParentItem(parentObjects, _kv.getKey(), _kv.getValue().getAsJsonObject(), array_join);
                    if (_obj == null) return;
                    _new.add(_kv.getKey(), _obj);
                });
            } else {
                JsonObject _obj = _combineParentItem(parentObjects, kv.getKey(), kv.getValue().getAsJsonObject(), array_join);
                if (_obj == null) return;
                _new.add(kv.getKey(), _obj);
            }
        });
        return _new;
    }
    private JsonObject _combineParentItem(HashMap<String, JsonObject> parentObjects, String key, JsonObject item, boolean array_join) {
        String parent = item.has("parent") ? item.get("parent").getAsString() : null;
        if (parent != null)
        {
            JsonObject _parent = parentObjects.getOrDefault(parent, null);
            if (_parent == null)
            {
                _logOP("[ERROR] PARENT '" + parent + "' NOT FOUNDED!");
                return null;
            }
            item = _combineJson(item, _parent, array_join).getAsJsonObject();
        }
        item.remove("parent");
        parentObjects.put(key, item);
        if (key.startsWith("_")) return null;
        return item;
    }
    
    protected JavaScript js() {
        throw new IllegalAccessError("JavaScript module not overrided");
    }
    private Optional<JsonElement> _executePartOfJS(JavaScript js, JsonElement element) {
        return _executePartOfJS(js, element, Collections.emptyMap());
    }
    private Optional<JsonElement> _executePartOfJS(JavaScript js, JsonElement element, Map<String, Object> setup) {
        if (element.isJsonObject()) {
            JsonObjectOptional json = JsonObjectOptional.of(element.getAsJsonObject());
            String code = json.getAsString("code").orElseThrow();
            HashMap<String, Object> data = new HashMap<>(setup);
            json.getAsJsonObject("args")
                .ifPresent(args -> args
                    .forEach((key, value) -> data.put(key, value.createObject())));
            return js.getJsJson(code, data);
        }
        return js.getJsJson(element.getAsString());
    }
    private JsonObject _executeJS(JsonObject json) {
        if (json.has("GENERATE_JS_APPEND")) {
            JavaScript js = js();
            system.Toast1<JsonObject> append = system.toast(new JsonObject());
            json.getAsJsonArray("GENERATE_JS_APPEND").forEach(_js -> {
                if (_js.isJsonPrimitive()) 
                    _executePartOfJS(js, _js)
                        .ifPresent(result -> append.val0 = _combineJson(append.val0, result, false).getAsJsonObject());
                else if (_js.isJsonArray()) 
                    _js.getAsJsonArray().forEach(__js -> {
                        _executePartOfJS(js, _js)
                            .ifPresent(result -> append.val0 = _combineJson(append.val0, result, false).getAsJsonObject());
                    });
                else if (_js.isJsonObject()) 
                    _js.getAsJsonObject().entrySet().forEach(_kv -> {
                        if (_kv.getKey().startsWith("!"))
                            _executePartOfJS(js, _kv.getValue(), Collections.singletonMap("key", _kv.getKey().substring(1)))
                                .ifPresent(result -> append.val0 = _combineJson(append.val0, result, false).getAsJsonObject());
                        else 
                            _executePartOfJS(js, _kv.getValue(), Collections.singletonMap("key", _kv.getKey()))
                                .ifPresent(result -> append.val0.add(_kv.getKey(), append.val0.has(_kv.getKey())
                                        ? _combineJson(append.val0, append.val0.get(_kv.getKey()), false)
                                        : result));
                    });
            });
            json = _combineJson(system.DeepCopy(json), append.val0, false).getAsJsonObject();
            json.remove("GENERATE_JS_APPEND");
        }
        json.entrySet().forEach(kv -> {
            if (!kv.getValue().isJsonObject()) return;
            kv.setValue(_executeJS(kv.getValue().getAsJsonObject()));
        });
        return json;
    }
    
    public boolean _existFile(String path) {
        File myObj = new File(path);
        return (myObj.isFile() && myObj.exists());
    }
    public String _readAllText(String path) {
        try {
            return Files.readString(Paths.get(path)).replace("\r", "");
            //return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8).replace("\r", "");
        } catch (IOException e) {
            _log("An error occurred.");
            _log(e.getMessage());
            return null;
        }
    }
    public String _readAllText(File file) {
        return _readAllText(file.getAbsolutePath());
    }
    public void _writeAllText(String path, String text) {
        try {
            Files.writeString(Paths.get(path), text);
        } catch (IOException e) {
            _log("An error occurred.");
            _log(e.getMessage());
        } catch (Exception e) {
            _log(e.getMessage());
        }
    }
    public void _deleteText(String path) {
        try {
            File myObj = new File(path);
            myObj.delete();
        } catch (Exception e) {
            _log(e.getMessage());
        }
    }

    public File _getConfigFile(String file) {
        return new File(getConfigFile() + file);
    }

    public boolean _existConfig(String config)
    {
        return _existConfig(config, ".json");
    }
    public String _readAllConfig(String config) {
        return _readAllConfig(config, ".json");
    }
    public void _writeAllConfig(String config, String text) {
        _writeAllConfig(config, ".json", text);
    }
    public void _deleteConfig(String config)
    {
        _deleteConfig(config, ".json");
    }

    public boolean _existConfig(String config, String ext) {
        return _existFile(getConfigFile() + config + ext);
    }
    public String _readAllConfig(String config, String ext)
    {
        return _readAllText(getConfigFile() + config + ext);
    }
    public void _writeAllConfig(String config, String ext, String text) {
        _writeAllText(getConfigFile() + config + ext, text);
    }
    public void _deleteConfig(String config, String ext)
    {
        _deleteText(getConfigFile() + config + ext);
    }

    public void _logStackTrace(Throwable exception) {
        Throwable target = exception;
        while (target != null) {
            _logOP(ChatColor.RED + target.getMessage());
            StackTraceElement[] stackTraceElements = target.getStackTrace();
            int length = stackTraceElements.length;
            for (int i = 0; i < length; i++)
            {
                if (i > 10) _logConsole(stackTraceElements[i].toString());
                else _logOP(stackTraceElements[i].toString());
            }
            target = target.getCause();
        }
    }
    public void _logStackTrace() {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace())
            _logOP(stackTraceElement.toString());
    }

    @SuppressWarnings("unused")
    private static class _example extends core {
        public static _example _plugin;

        @Override public String getLogPrefix() { return "LIME:EXAMPLE"; }
        @Override public String getConfigFile() { return "plugins/example/"; }
        @Override protected void init() { }

        //<editor-fold desc="CORE INIT">
        public static void logToFile(String key, String text) { _plugin._logToFile(key, text);}
        public static void log(String log) { _plugin._log(log);}
        public static void logAdmin(String log) { _plugin._logAdmin(log);}
        public static void logConsole(String log) { _plugin._logConsole(log);}
        public static void logOP(String log) { _plugin._logOP(log); }
        public static void logOP(Component log) { _plugin._logOP(log); }
        public static void logWithoutPrefix(String log) { _plugin._logWithoutPrefix(log);}
        public static void logStackTrace(Throwable exception) { _plugin._logStackTrace(exception); }
        public static void logStackTrace() { _plugin._logStackTrace(); }

        public static TimerBuilder timer() { return _plugin._timer(); }
        public static BukkitTask nextTick(ITimers.IRunnable callback) { return _plugin._nextTick(callback); }
        public static BukkitTask onceNoCheck(ITimers.IRunnable callback, double sec) { return _plugin._onceNoCheck(callback, sec); }
        public static BukkitTask once(ITimers.IRunnable callback, double sec) { return _plugin._once(callback, sec); }
        public static BukkitTask onceTicks(ITimers.IRunnable callback, long ticks) { return _plugin._onceTicks(callback, ticks); }
        public static BukkitTask repeat(ITimers.IRunnable callback, double sec) { return _plugin._repeat(callback, sec); }
        public static BukkitTask repeatTicks(ITimers.IRunnable callback, long ticks) { return _plugin._repeatTicks(callback, ticks); }
        public static BukkitTask repeat(ITimers.IRunnable callback, double wait, double sec) { return _plugin._repeat(callback, wait, sec); }
        public static BukkitTask repeatTicks(ITimers.IRunnable callback, long wait, long ticks) { return _plugin._repeatTicks(callback, wait, ticks); }
        public static <T>void repeat(T[] array, system.Action1<T> callback_part, system.Action0 callback_end, double sec, int inOneStep) { _plugin._repeat(array, callback_part, callback_end, sec, inOneStep); }
        public static BukkitTask invokeAsync(system.Action0 async, ITimers.IRunnable nextSync) { return _plugin._invokeAsync(async, nextSync); }
        public static <T>BukkitTask invokeAsync(system.Func0<T> async, system.Action1<T> nextSync) { return _plugin._invokeAsync(async, nextSync); }
        public static void invokeSync(ITimers.IRunnable sync) { _plugin._invokeSync(sync); }
        public static void invokable(IInvokable invokable) { _plugin._invokable(invokable); }

        public static JsonElement combineJson(JsonElement first, JsonElement second, boolean array_join) { return _plugin._combineJson(first, second, array_join); }
        public static JsonElement combineJson(JsonElement first, JsonElement second) { return _plugin._combineJson(first, second); }
        public static JsonObject combineParent(JsonObject json) { return _plugin._combineParent(json); }
        public static JsonObject combineParent(JsonObject json, boolean category, boolean array_join) { return _plugin._combineParent(json, category, array_join); }

        public static boolean existFile(String path) { return _plugin._existFile(path); }
        public static String readAllText(String path) { return _plugin._readAllText(path); }
        public static String readAllText(File file) { return _plugin._readAllText(file); }
        public static void writeAllText(String path, String text) { _plugin._writeAllText(path, text); }
        public static void deleteText(String path) { _plugin._deleteText(path); }
        public static File getConfigFile(String file) { return _plugin._getConfigFile(file); }
        public static boolean existConfig(String config) { return _plugin._existConfig(config); }
        public static String readAllConfig(String config) { return _plugin._readAllConfig(config); }
        public static void writeAllConfig(String config, String text) { _plugin._writeAllConfig(config, text); }
        public static void deleteConfig(String config) { _plugin._deleteConfig(config); }
        public static boolean existConfig(String config, String ext) { return _plugin._existConfig(config, ext); }
        public static String readAllConfig(String config, String ext) { return _plugin._readAllConfig(config, ext); }
        public static void writeAllConfig(String config, String ext, String text) { _plugin._writeAllConfig(config, ext, text); }
        public static void deleteConfig(String config, String ext) { _plugin._deleteConfig(config, ext); }
        public static MCTiming timing(String name) { return _plugin._timing(name); }
        public static void timing(String name, system.Action0 callback) { _plugin._timing(name, callback); }
        //</editor-fold>
    }
}