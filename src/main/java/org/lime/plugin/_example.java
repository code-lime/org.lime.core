package org.lime.plugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitTask;
import org.lime.LimeCore;
import org.lime.invokable.BaseInvokable;
import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Func0;

import java.io.File;

@SuppressWarnings("unused")
class _example extends LimeCore {
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
    public static BukkitTask nextTick(Timers.IRunnable callback) { return _plugin._nextTick(callback); }
    public static BukkitTask onceNoCheck(Timers.IRunnable callback, double sec) { return _plugin._onceNoCheck(callback, sec); }
    public static BukkitTask once(Timers.IRunnable callback, double sec) { return _plugin._once(callback, sec); }
    public static BukkitTask onceTicks(Timers.IRunnable callback, long ticks) { return _plugin._onceTicks(callback, ticks); }
    public static BukkitTask repeat(Timers.IRunnable callback, double sec) { return _plugin._repeat(callback, sec); }
    public static BukkitTask repeatTicks(Timers.IRunnable callback, long ticks) { return _plugin._repeatTicks(callback, ticks); }
    public static BukkitTask repeat(Timers.IRunnable callback, double wait, double sec) { return _plugin._repeat(callback, wait, sec); }
    public static BukkitTask repeatTicks(Timers.IRunnable callback, long wait, long ticks) { return _plugin._repeatTicks(callback, wait, ticks); }
    public static <T>void repeat(T[] array, Action1<T> callback_part, Action0 callback_end, double sec, int inOneStep) { _plugin._repeat(array, callback_part, callback_end, sec, inOneStep); }
    public static BukkitTask invokeAsync(Action0 async, Timers.IRunnable nextSync) { return _plugin._invokeAsync(async, nextSync); }
    public static <T>BukkitTask invokeAsync(Func0<T> async, Action1<T> nextSync) { return _plugin._invokeAsync(async, nextSync); }
    public static void invokeSync(Timers.IRunnable sync) { _plugin._invokeSync(sync); }
    public static void invokable(BaseInvokable invokable) { _plugin._invokable(invokable); }

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
    //</editor-fold>
}
