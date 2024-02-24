package org.lime;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.values.IV8Value;
import com.caoccao.javet.values.V8Value;
import com.caoccao.javet.values.reference.*;
import com.google.gson.JsonElement;
import org.bukkit.scheduler.BukkitTask;
import org.lime.plugin.ICore;
import org.lime.plugin.CoreElement;
import org.lime.system.execute.Action1;
import org.lime.system.json;
import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

public class JavaScript implements ICore {
    private static V8Runtime runtime;

    public static CoreElement create() { return new JavaScript()._create(); }
    private CoreElement _create() {
        return CoreElement.create(JavaScript.class)
                .withInstance(this)
                .withInit(this::init)
                .withUninit(this::uninit)
                .addText("global.js", v -> v.withInvoke(this::config));
    };

    private core base_core = null;
    @Override public void core(core base_core) { this.base_core = base_core; }
    @Override public core core() { return base_core; }

    public static abstract class InstanceJS {
        public final ConcurrentLinkedQueue<BukkitTask> inits = new ConcurrentLinkedQueue<>();
    }

    public final ConcurrentHashMap<String, InstanceJS> instances = new ConcurrentHashMap<>();

    private static final String FUNCTION_PREFIX = "FUNCTION ";
    private static final int FUNCTION_PREFIX_LENGTH = FUNCTION_PREFIX.length();
    private V8Value invokeFunction(V8ValueGlobalObject global, String methodPath) throws Exception {
        return invokeFunction(global, methodPath, null);
    }
    private V8Value invokeFunction(V8ValueGlobalObject global, String methodPath, @Nullable Object[] args) throws Exception {
        if (!methodPath.contains(".")) {
            try (V8ValueFunction function = global.get(methodPath)) {
                return args == null ? function.call(global) : function.call(global, args);
            }
        }

        V8ValueObject partObject = global;
        for (String part : methodPath.split(Pattern.quote("."))) {
            try (V8ValueObject ignored = partObject) {
                partObject = partObject.get(part);
            }
        }

        if (partObject instanceof V8ValueFunction function)
            return args == null ? function.call(global) : function.call(global, args);
        else
            throw new IllegalArgumentException("JS path " + methodPath + " not found or not function");
    }
    private V8Value eval(String script) throws Exception {
        return eval(script, null);
    }
    private V8Value eval(String script, @Nullable Map<String, Object> values) throws Exception {
        V8ValueGlobalObject global = runtime.getGlobalObject();

        if (!script.startsWith(FUNCTION_PREFIX)) {
            if (values == null || values.isEmpty()) return runtime.getExecutor(script).execute();
            try (V8ValueObject args = runtime.createV8ValueObject()) {
                List<String> lines = new ArrayList<>();
                for (var kv : values.entrySet()) {
                    args.set(kv.getKey(), kv.getValue());
                    lines.add("let " + kv.getKey() + " = args." + kv.getKey() + ";");
                }
                lines.add("return " + script + ";");

                String raw = String.join("\n", lines);
                try (V8ValueFunction func = runtime.getExecutor(raw).compileV8ValueFunction(new String[] { "args" })) {
                    return func.call(global, args);
                }
            }
        }
        script = script.substring(FUNCTION_PREFIX_LENGTH).replace(" ", "");

        int startFunc = script.indexOf('(');
        if (startFunc == -1) return global.<V8ValueFunction>get(script).call(global);

        int endFunc = script.indexOf(')');

        String methodName = script.substring(0, startFunc);
        String methodArgsLine = script.substring(startFunc + 1, endFunc);

        if (methodArgsLine.length() == 0) return invokeFunction(global, methodName);

        String[] methodArgs = script.substring(startFunc + 1, endFunc).split(",");
        int argsLength = methodArgs.length;
        Object[] args = new Object[argsLength];
        List<IV8Value> closeables = new ArrayList<>();
        try {
            for (int i = 0; i < argsLength; i++) {
                String arg = methodArgs[i].trim();
                if (values != null && values.containsKey(arg)) {
                    args[i] = values.get(arg);
                } else {
                    IV8Value value = global.get(arg);
                    args[i] = value;
                    closeables.add(value);
                }
            }
            return invokeFunction(global, methodName, args);
        } finally {
            closeables.forEach(v -> {
                try { v.close(); } catch (Exception ignored) { }
            });
        }
    }

    private void init() {
        instances.values().forEach(v -> v.inits.removeIf(BukkitTask::isCancelled));
    }
    private void uninit() {
        if (runtime != null) {
            try {
                runtime.close(true);
            } catch (Exception e) {
                base_core._logStackTrace(e);
            }
        }
    }

    private void errorDebug(String key, Exception exception) {
        if (exception instanceof JavetException javetException) {
            base_core._logOP("JS ERROR: " + key);
            javetException.getParameters().forEach((k,v) -> base_core._logOP(" - " + k + ": " + v));
            base_core._logStackTrace(exception);
        } else {
            base_core._logOP("JS ERROR: " + key);
            base_core._logStackTrace(exception);
        }
    }

    public void reinstance(boolean cancel) {
        if (cancel) instances.values().forEach(v -> v.inits.forEach(BukkitTask::cancel));
        try {
            for (var kv : instances.entrySet()) {
                runtime.getGlobalObject().set(kv.getKey(), kv.getValue());
            }
        }
        catch (Exception e) {
            errorDebug("Instances", e);
        }
    }
    public void config(String js) {
        if (runtime != null) {
            try {
                runtime.close(true);
            } catch (Exception e) {
                base_core._logStackTrace(e);
            }
        }

        LinkedHashMap<String, String> _modules = new LinkedHashMap<>();
        List<String> _js = new ArrayList<>();
        boolean _import = true;
        for (String line : js.split("\n")) {
            if (_import && line.startsWith("//import ")) {
                String module = line.substring("//import ".length());
                base_core._logOP("JS import: " + module);
                _modules.put(module, base_core._readAllConfig(module, ".js"));
            } else {
                _js.add(line);
                _import = false;
            }
        }
        instances.values().forEach(v -> v.inits.forEach(BukkitTask::cancel));

        try {
            runtime = V8Host.getNodeInstance().createV8Runtime();
            runtime.setConverter(new JavetNativeConverter());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        base_core._logOP("JavaScriptEngine: " + runtime.getClass().getName());
        String loadModule = "???";
        try {
            for (Map.Entry<String, String> kv : _modules.entrySet()) {
                loadModule = kv.getKey();
                eval(kv.getValue()).close();
            }
        }
        catch (Exception e) {
            errorDebug(loadModule + ".js", e);
        }
        try {
            eval(String.join("\n", _js)).close();

            for (var kv : instances.entrySet()) {
                base_core._logOP("Set JS instance: " + kv.getKey());
                runtime.getGlobalObject().set(kv.getKey(), kv.getValue());
            }

            eval("init()").close();
        }
        catch (Exception e) {
            errorDebug("global.js", e);
        }

        base_core._logOP("TEST: " + getJsString("a + b", Map.of("a", 1, "b", 55)));
    }

    public Optional<Boolean> isJsTrue(String js) { return isJsTrue(js, Collections.emptyMap()); }
    public Optional<Boolean> isJsTrue(String js, Map<String, Object> values) {
        try {
            try (V8Value value = eval(js, values)) {
                return Optional.of(value.asBoolean());
            }
        }
        catch (Exception e) {
            errorDebug("JS:\n" + js, e);
            return Optional.empty();
        }
    }

    public Optional<Number> getJsNumber(String js) { return getJsNumber(js, Collections.emptyMap()); }
    public Optional<Number> getJsNumber(String js, Map<String, Object> values) {
        try {
            try (V8Value value = eval(js, values)) {
                return Optional.of(value.asDouble());
            }
        }
        catch (Exception e) {
            errorDebug("JS:\n" + js, e);
            return Optional.empty();
        }
    }

    public Optional<Integer> getJsInt(String js) { return getJsInt(js, Collections.emptyMap()); }
    public Optional<Integer> getJsInt(String js, Map<String, Object> values) {
        try {
            try (V8Value value = eval(js, values)) {
                return Optional.of(value.asInt());
            }
        }
        catch (Exception e) {
            errorDebug("JS:\n" + js, e);
            return Optional.empty();
        }
    }

    public Optional<Double> getJsDouble(String js) { return getJsDouble(js, Collections.emptyMap()); }
    public Optional<Double> getJsDouble(String js, Map<String, Object> values) {
        try {
            try (V8Value value = eval(js, values)) {
                return Optional.of(value.asDouble());
            }
        }
        catch (Exception e) {
            errorDebug("JS:\n" + js, e);
            return Optional.empty();
        }
    }

    public Optional<String> getJsString(String js) { return getJsString(js, Collections.emptyMap()); }
    public Optional<String> getJsString(String js, Map<String, Object> values) {
        try {
            try (V8Value value = eval(js, values)) {
                return Optional.of(value.asString());
            }
        }
        catch (Exception e) {
            errorDebug("JS:\n" + js, e);
            return Optional.empty();
        }
    }

    public Optional<JsonElement> getJsJson(String js) { return getJsJson(js, Collections.emptyMap()); }
    public Optional<JsonElement> getJsJson(String js, Map<String, Object> values) {
        try {
            try (V8Value value = eval(js, values)) {
                return value instanceof IV8ValueObject valueObject
                        ? Optional.of(json.parse(valueObject.toJsonString()))
                        : Optional.of(json.by(value).build());
            }
        }
        catch (Exception e) {
            errorDebug("JS:\n" + js, e);
            return Optional.empty();
        }
    }

    public void getJsStringNext(String js, Action1<String> callback) {
        try {
            try (V8Value value = eval(js)) {
                if (value == null) callback.invoke(null);
                else if (value instanceof IV8ValueFunction function) function.call(runtime.getGlobalObject(), (Action1<Object>) v -> callback.invoke(v instanceof String str ? str : v.toString())).close();
                else callback.invoke(value.asString());
            }
        }
        catch (Exception e) {
            errorDebug("JS:\n" + js, e);
            callback.invoke("");
        }
    }

    public V8ValueObject createNative() {
        try {
            return runtime.createV8ValueObject();
        } catch (Exception e) {
            errorDebug("JS.NATIVE", e);
            return null;
        }
    }

    public void invoke(String js) {
        try {
            eval(js).close();
        }
        catch (Exception e) {
            errorDebug("JS:\n" + js, e);
        }
    }
    public void invoke(String js, Map<String, Object> values) {
        try {
            eval(js, values).close();
        }
        catch (Exception e) {
            errorDebug("JS:\n" + js, e);
        }
    }
    /*
    public Optional<Object> invoke(String js, Map<String, Object> values) {
        try {
            return Optional.ofNullable(eval(js, values));
        }
        catch (Exception e) {
            errorDebug("JS:\n" + js, e);
            return Optional.empty();
        }
    }*/
}





















