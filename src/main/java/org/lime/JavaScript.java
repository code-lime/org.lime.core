package org.lime;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.vk2gpz.jsengine.JSEngine;
import org.bukkit.scheduler.BukkitTask;
import org.openjdk.nashorn.api.scripting.JSObject;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import javax.annotation.Nullable;
import javax.script.*;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JavaScript implements core.ICore {
    private NashornScriptEngine engine;
    public static core.element create() {
        return new JavaScript()._create();
    }
    private core.element _create() {
        return core.element.create(JavaScript.class)
                .withInstance(this)
                .withInit(this::init)
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
    private Object eval(String script) throws Exception {
        return eval(script, null);
    }
    private Object eval(String script, @Nullable Map<String, Object> values) throws Exception {
        if (!script.startsWith(FUNCTION_PREFIX)) {
            if (values == null || values.isEmpty()) return engine.eval(script);
            return engine.eval(script, new ArgsBinding(values, engine.getBindings(ScriptContext.ENGINE_SCOPE)));
        }
        script = script.substring(FUNCTION_PREFIX_LENGTH).replace(" ", "");

        int startFunc = script.indexOf('(');
        if (startFunc == -1) return engine.invokeFunction(script);

        int endFunc = script.indexOf(')');

        String methodName = script.substring(0, startFunc);
        String methodArgsLine = script.substring(startFunc + 1, endFunc);

        if (methodArgsLine.length() == 0) return engine.invokeFunction(methodName);

        String[] methodArgs = script.substring(startFunc + 1, endFunc).split(",");
        int argsLength = methodArgs.length;
        Object[] args = new Object[argsLength];
        JSObject global = (JSObject)engine.get(NashornScriptEngine.NASHORN_GLOBAL);
        for (int i = 0; i < argsLength; i++) {
            String arg = methodArgs[i];
            args[i] = values != null && values.containsKey(arg)
                    ? values.get(arg)
                    : global.getMember(arg);
        }
        return engine.invokeFunction(methodName, args);
    }

    public void init() {
        instances.values().forEach(v -> v.inits.removeIf(BukkitTask::isCancelled));
    }
    public void config(String js) {
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
        engine = (NashornScriptEngine)JSEngine.getEngine();
        engine.getContext().setBindings(new SimpleBindings(new ConcurrentHashMap<>()), ScriptContext.ENGINE_SCOPE);
        base_core._logOP("JavaScriptEngine: " + engine.getClass().getName());
        String loadModule = "???";
        try {
            for (Map.Entry<String, String> kv : _modules.entrySet()) {
                loadModule = kv.getKey();
                eval(kv.getValue());
            }
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR:" + loadModule + ".js");
            base_core._logStackTrace(e);
        }
        try {
            eval(String.join("\n", _js));
            instances.forEach(engine::put);
            eval("init()");
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR:global.js");
            base_core._logStackTrace(e);
        }
    }
    public Optional<Boolean> isJsTrue(String js) {
        try
        {
            Object value = eval(js);
            return Optional.of(value instanceof Boolean bool ? bool : value.equals(1));
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            return Optional.empty();
        }
    }
    public Optional<Number> getJsNumber(String js) {
        try
        {
            return Optional.ofNullable((Number)eval(js));
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            return Optional.empty();
        }
    }
    public Optional<Integer> getJsInt(String js) { return getJsNumber(js).map(Number::intValue); }
    public Optional<Double> getJsDouble(String js) { return getJsNumber(js).map(Number::doubleValue); }
    public Optional<String> getJsString(String js) {
        try
        {
            Object value = eval(js);
            return Optional.of(value instanceof String str ? str : value.toString());
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            return Optional.empty();
        }
    }
    public Optional<JsonElement> getJsJson(String js) {
        try
        {
            return invoke("JSON.stringify(value)", Collections.singletonMap("value", eval(js)))
                .map(value -> value instanceof String str ? str : value.toString())
                .map(value -> system.json.parse(value));
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            return Optional.empty();
        }
    }
    public Optional<JsonElement> getJsJson(String js, Map<String, Object> values) {
        try
        {
            return invoke("JSON.stringify(value)", Collections.singletonMap("value", eval(js, values)))
                .map(value -> value instanceof String str ? str : value.toString())
                .map(value -> system.json.parse(value));
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            return Optional.empty();
        }
    }

    public void getJsStringNext(String js, system.Action1<String> callback) {
        try
        {
            Object value = eval(js);
            if (value == null) callback.invoke(null);
            if (value instanceof ScriptObjectMirror som && som.isFunction()) som.call(null, (system.Action1<Object>)v -> callback.invoke(v instanceof String str ? str : v.toString()));
            else callback.invoke(value instanceof String str ? str : value.toString());
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            callback.invoke("");
        }
    }

    public void invoke(String js) {
        try
        {
            eval(js);
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
        }
    }
    public Optional<Object> invoke(String js, Map<String, Object> values) {
        try {
            return Optional.ofNullable(eval(js, values));
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            return Optional.empty();
        }
    }

    private static class ArgsBinding implements Bindings {
        private final Map<String, Object> args;
        private final Bindings owner;

        public ArgsBinding(Map<String, Object> args, Bindings owner) {
            this.args = args;
            this.owner = owner;
        }

        @Override public int size() { return args.size() + owner.size(); }
        @Override public boolean isEmpty() { return args.isEmpty() && owner.isEmpty(); }
        @Override public boolean containsValue(Object value) { return args.containsValue(value) || owner.containsValue(value); }
        @Override public void clear() { owner.clear(); }

        private static <T>Set<T> combile(Set<T> args, Set<T> owner) {
            return new Set<T>() {
                @Override public int size() { return args.size() + owner.size(); }
                @Override public boolean isEmpty() { return args.isEmpty() && owner.isEmpty(); }
                @Override public boolean contains(Object value) { return args.contains(value) || owner.contains(value); }
                @Override public Iterator<T> iterator() { return Streams.concat(args.stream(), owner.stream()).iterator(); }
                @Override public Object[] toArray() { return Streams.concat(args.stream(), owner.stream()).toArray(); }
                @SuppressWarnings("all") @Override public <_T> _T[] toArray(_T[] a) {
                    Class<_T> type = (Class<_T>)a.getClass().getComponentType();
                    return Streams.concat(args.stream(), owner.stream()).toArray(length -> (_T[])Array.newInstance(type, length));
                }
                @Override public boolean add(T e) { throw new IllegalAccessError(); }
                @Override public boolean remove(Object o) { throw new IllegalAccessError(); }
                @Override public boolean containsAll(Collection<?> c) { return args.containsAll(c) && owner.containsAll(c); }
                @Override public boolean addAll(Collection<? extends T> c) { return owner.addAll(c); }
                @Override public boolean retainAll(Collection<?> c) { return owner.retainAll(c); }
                @Override public boolean removeAll(Collection<?> c) { return owner.removeAll(c); }
                @Override public void clear() { owner.clear(); }
            };
        }
        private static <T>Collection<T> combile(Collection<T> args, Collection<T> owner) {
            return new Collection<T>() {
                @Override public int size() { return args.size() + owner.size(); }
                @Override public boolean isEmpty() { return args.isEmpty() && owner.isEmpty(); }
                @Override public boolean contains(Object value) { return args.contains(value) || owner.contains(value); }
                @Override public Iterator<T> iterator() { return Streams.concat(args.stream(), owner.stream()).iterator(); }
                @Override public Object[] toArray() { return Streams.concat(args.stream(), owner.stream()).toArray(); }
                @SuppressWarnings("all") @Override public <_T> _T[] toArray(_T[] a) {
                    Class<_T> type = (Class<_T>)a.getClass().getComponentType();
                    return Streams.concat(args.stream(), owner.stream()).toArray(length -> (_T[])Array.newInstance(type, length));
                }
                @Override public boolean add(T e) { throw new IllegalAccessError(); }
                @Override public boolean remove(Object o) { throw new IllegalAccessError(); }
                @Override public boolean containsAll(Collection<?> c) { return args.containsAll(c) && owner.containsAll(c); }
                @Override public boolean addAll(Collection<? extends T> c) { return owner.addAll(c); }
                @Override public boolean retainAll(Collection<?> c) { return owner.retainAll(c); }
                @Override public boolean removeAll(Collection<?> c) { return owner.removeAll(c); }
                @Override public void clear() { owner.clear(); }
            };
        }

        @Override public Set<String> keySet() { return combile(args.keySet(), owner.keySet()); }
        @Override public Collection<Object> values() { return combile(args.values(), owner.values()); }
        @Override public Set<Entry<String, Object>> entrySet() { return combile(args.entrySet(), owner.entrySet()); }
        @Override public Object put(String name, Object value) { return owner.put(name, value); }
        @Override public void putAll(Map<? extends String, ? extends Object> toMerge) { owner.putAll(toMerge); }
        @Override public boolean containsKey(Object key) { return args.containsKey(key) || owner.containsKey(key); }
        @Override public Object get(Object key) { return args.getOrDefault(key, owner.get(key)); }
        @Override public Object remove(Object key) { return owner.remove(key); }
    }
}





















