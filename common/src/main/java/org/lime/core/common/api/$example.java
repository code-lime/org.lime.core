package org.lime.core.common.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.lime.core.common.BaseCoreInstance;
import org.lime.core.common.invokable.BaseInvokable;
import org.lime.core.common.api.tasks.ScheduleTask;
import org.lime.core.common.system.execute.Action0;
import org.lime.core.common.system.execute.Action1;
import org.lime.core.common.system.execute.Func0;

import java.io.File;

@SuppressWarnings("unused")
abstract class $example extends BaseCoreInstance {
    public static $example instance;

    @Override public String getLogPrefix() { return "LIME:EXAMPLE"; }
    @Override public String configFile() { return "config/example/"; }
    @Override public String name() { return "example"; }

    @Override protected void init() { }

    //<editor-fold desc="CORE INIT">
    public static void logToFile(String key, String text) { instance.$logToFile(key, text);}
    public static void log(String log) { instance.$log(log);}
    public static void logAdmin(String log) { instance.$logBroadcast(log);}
    public static void logConsole(String log) { instance.$logConsole(log);}
    public static void logOP(String log) { instance.$logOP(log); }
    public static void logOP(Component log) { instance.$logOP(log); }
    public static void logWithoutPrefix(String log) { instance.$logWithoutPrefix(log);}
    public static void logStackTrace(Throwable exception) { instance.$logStackTrace(exception); }
    public static void logStackTrace() { instance.$logStackTrace(); }

    public static TimerBuilder timer() { return instance.$timer(); }
    public static ScheduleTask nextTick(Action0 callback) { return instance.$nextTick(callback); }
    public static ScheduleTask onceNoCheck(Action0 callback, double sec) { return instance.$onceNoCheck(callback, sec); }
    public static ScheduleTask once(Action0 callback, double sec) { return instance.$once(callback, sec); }
    public static ScheduleTask onceTicks(Action0 callback, long ticks) { return instance.$onceTicks(callback, ticks); }
    public static ScheduleTask repeat(Action0 callback, double sec) { return instance.$repeat(callback, sec); }
    public static ScheduleTask repeatTicks(Action0 callback, long ticks) { return instance.$repeatTicks(callback, ticks); }
    public static ScheduleTask repeat(Action0 callback, double wait, double sec) { return instance.$repeat(callback, wait, sec); }
    public static ScheduleTask repeatTicks(Action0 callback, long wait, long ticks) { return instance.$repeatTicks(callback, wait, ticks); }
    public static <T>void repeat(T[] array, Action1<T> callback_part, Action0 callback_end, double sec, int inOneStep) { instance.$repeat(array, callback_part, callback_end, sec, inOneStep); }
    public static ScheduleTask invokeAsync(Action0 async, Action0 nextSync) { return instance.$invokeAsync(async, nextSync); }
    public static <T>ScheduleTask invokeAsync(Func0<T> async, Action1<T> nextSync) { return instance.$invokeAsync(async, nextSync); }
    public static void invokeSync(Action0 sync) { instance.$invokeSync(sync); }
    public static void invokable(BaseInvokable invokable) { instance.$invokable(invokable); }

    public static JsonElement combineJson(JsonElement first, JsonElement second, boolean array_join) { return instance.$combineJson(first, second, array_join); }
    public static JsonElement combineJson(JsonElement first, JsonElement second) { return instance.$combineJson(first, second); }
    public static JsonObject combineParent(JsonObject json) { return instance.$combineParent(json); }
    public static JsonObject combineParent(JsonObject json, boolean category, boolean array_join) { return instance.$combineParent(json, category, array_join); }

    public static boolean existFile(String path) { return instance.$existFile(path); }
    public static String readAllText(String path) { return instance.$readAllText(path); }
    public static String readAllText(File file) { return instance.$readAllText(file); }
    public static void writeAllText(String path, String text) { instance.$writeAllText(path, text); }
    public static void deleteText(String path) { instance.$deleteText(path); }
    public static File getConfigFile(String file) { return instance.$configFile(file); }
    public static boolean existConfig(String config) { return instance.$existConfig(config); }
    public static String readAllConfig(String config) { return instance.$readAllConfig(config); }
    public static void writeAllConfig(String config, String text) { instance.$writeAllConfig(config, text); }
    public static void deleteConfig(String config) { instance.$deleteConfig(config); }
    public static boolean existConfig(String config, String ext) { return instance.$existConfig(config, ext); }
    public static String readAllConfig(String config, String ext) { return instance.$readAllConfig(config, ext); }
    public static void writeAllConfig(String config, String ext, String text) { instance.$writeAllConfig(config, ext, text); }
    public static void deleteConfig(String config, String ext) { instance.$deleteConfig(config, ext); }
    //</editor-fold>
}
