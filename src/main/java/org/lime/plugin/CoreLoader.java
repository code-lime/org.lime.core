package org.lime.plugin;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.lime.LibraryClassLoader;
import org.lime.LimeCore;
import org.lime.modules.PluginImporter;
import org.lime.system.execute.*;
import patch.Patcher;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CoreLoader extends JavaPlugin implements ITimer, ICombineJson, IConfig, IFile, ILogger {
    public abstract Stream<CoreElement<?>> elements();
    public abstract Stream<LibraryClassLoader> libraries();
    public abstract <T> CoreElementLoaded<T> add(CoreElement<T> command);
    public abstract void add(CoreCommand<?> command);
    public abstract void add(String cmd, Func1<CoreCommand<?>, CoreCommand<?>> builder);
    public abstract void library(File... jars);
    public abstract void library(String... jars);
    protected abstract void rawLibrary(LibraryClassLoader loader);

    protected abstract Map<String, CoreCommand<?>> commands();
    protected abstract Optional<IUpdateConfig> config();

    @Override public void onEnable() {
        if (!(this instanceof LimeCore _core)) {
            throw new IllegalArgumentException("Not support '"+this.getClass()+"'! Supported only `"+ LimeCore.class+"`");
        }

        if (LimeCore.instance == null) {
            try {
                Patcher.patch(this.getLogger()::warning);

                LimeCore.instance = _core;
                init_core();
            } catch (Exception e) {
                _logStackTrace(e);
                throw e;
            }
            return;
        }

        try { getClass().getDeclaredField("_plugin").set(null, this); } catch (Exception ignored) { }

        File dir = _getConfigFile("");
        if (!dir.exists()) dir.mkdir();

        try {
            init();
            config().ifPresent(IUpdateConfig::updateConfigSync);

            _logOP("[............] Start load elements...");
            invokeList(elements().sorted(Comparator.comparingDouble(v -> v.sortType.getValue())).collect(Collectors.toList()), (item, prefix) -> {
                _logOP(prefix + " Load " + item.name + "...");
                if (item.instance != null) {
                    if (item.instance instanceof Listener) Bukkit.getPluginManager().registerEvents((Listener)item.instance, this);
                }
                if (item.init != null) item.init.invoke(_core);
                item.config.forEach(data -> data.read(_core, false));
                item.commands.forEach(_cmd -> add(_cmd.invoke()));
                PluginManager manager = Bukkit.getPluginManager();
                item.permissions.forEach(_perm -> {
                    manager.removePermission(_perm);
                    manager.addPermission(_perm);
                });
            });
            _logOP("[100%] Loaded!");

            _logOP("[............] Start load commands...");
            invokeList(commands(), (name, cmd, prefix) -> {
                _logOP(prefix + " Load command " + name + "...");
                Bukkit.getCommandMap().register(this.getName(), cmd.build(this));
            });
            _logOP("[100%] Loaded!");

            _repeatTicks(this::invokableTick, 1);
        } catch (Exception e) {
            _logStackTrace(e);
            onErrorInit();
        }
    }

    private void init_core() {
        //rawLibrary(coreLoader);
        PluginImporter.register(this);
        add("update.data", cmd -> cmd
                .withCheck(ServerOperator::isOp)
                .withTab((sender, args) -> {
                    switch (args.length) {
                        case 1: return Arrays.stream(Bukkit.getPluginManager().getPlugins()).filter(plugin -> plugin instanceof LimeCore).map(Plugin::getName).collect(Collectors.toList());
                        default: {
                            Plugin plugin = Bukkit.getPluginManager().getPlugin(args[0]);
                            if (!(plugin instanceof LimeCore corePlugin)) break;
                            return corePlugin.elements().flatMap(v -> v.config.stream().flatMap(_v -> _v.getFiles().stream())).collect(Collectors.toList());
                        }
                    }
                    return Collections.emptyList();
                })
                .withExecutor((sender, args) -> {
                    if (args.length < 2) return false;
                    Plugin plugin = Bukkit.getPluginManager().getPlugin(args[0]);
                    if (!(plugin instanceof LimeCore _corePlugin)) return false;
                    Collection<String> files = Arrays.stream(args).skip(1).collect(Collectors.toList());
                    Set<String> _files = new HashSet<>();
                    files.forEach(file -> _corePlugin.elements().forEach(element -> element.config.forEach(data -> {
                        if (!data.getFiles().contains(file)) return;
                        _files.addAll(data.getFiles());
                    })));
                    _corePlugin._logOP("Update files: " + String.join(" & ", _files));
                    try {
                        ((CoreLoader)_corePlugin).config().ifPresent(config -> config.updateConfigAsync(_files, () -> {
                            try {
                                files.forEach(file -> _corePlugin.elements().forEach(element -> element.config.forEach(data -> {
                                    if (!data.getFiles().contains(file)) return;
                                    data.read(_corePlugin, true);
                                })));
                            } catch (Exception e) {
                                _corePlugin._logStackTrace(e);
                            }
                        }));
                    } catch (Exception e) {
                        _corePlugin._logStackTrace(e);
                        return true;
                    }
                    _corePlugin._logOP("Updated!");
                    return true;
                })
        );

        commands().forEach((command, cmd) -> Bukkit.getCommandMap().register(this.getName(), cmd.build(this)));
        //_repeat(_system::tryClearCompare, 60);
    }
    protected abstract void init();
    protected abstract void invokableTick();
    protected void onErrorInit() {}

    @Override public void onDisable() {
        elements().forEach(item -> { if (item.uninit != null) item.uninit.invoke(); });
        libraries().forEach(lib -> lib.unload(true));
    }

    private static <T>void invokeList(Collection<T> list, Action2<T, String> invoke) {
        int size = list.size();
        int i = 0;
        for (T item : list) {
            invoke.invoke(item, "[" + StringUtils.leftPad(String.valueOf(i*100 / size), 3, '*').replace("*", "...") + "%]");
            i++;
        }
    }
    private static <K, V>void invokeList(Map<K, V> list, Action3<K, V, String> invoke) {
        invokeList(list.entrySet(), (v,pref) -> invoke.invoke(v.getKey(), v.getValue(), pref));
    }
}
