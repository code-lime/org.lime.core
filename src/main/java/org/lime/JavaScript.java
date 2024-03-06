package org.lime;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.impl.Accessor;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.bukkit.scheduler.BukkitTask;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionValues;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.io.FileSystem;
import org.lime.plugin.CoreElement;
import org.lime.plugin.ICore;
import org.lime.system.execute.Action1;
import org.lime.system.json;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;

import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class JavaScript implements ICore {
    private static class ContextMultithreading implements AutoCloseable {
        private record ContextEngine(long id, GraalJSScriptEngine engine, Context context, Bindings bindings, Map<?,?> global) { }
        private record ValueCache(Object raw, long time) {
            public ValueCache(Object raw) {
                this(raw, System.currentTimeMillis());
            }
        }
        public static class ContextFrame implements AutoCloseable {
            private final ContextEngine contextEngine;
            private final ConcurrentHashMap<String, ValueCache> globalValues;
            private ContextFrame(ContextEngine contextEngine, ConcurrentHashMap<String, ValueCache> globalValues) {
                this.contextEngine = contextEngine;
                this.globalValues = globalValues;
            }

            public GraalJSScriptEngine engine() {
                return contextEngine.engine();
            }
            public Context context() {
                return contextEngine.context();
            }
            public Map<?,?> global() {
                return contextEngine.global();
            }

            public void open() {
                Bindings bindings = contextEngine.bindings();
                Toast1<Integer> counter = Toast.of(0);
                globalValues.forEach((key, value) -> bindings.compute(key, (k, v) -> {
                    if (v == null || !Objects.equals(v, value.raw))
                    {
                        counter.val0++;
                        return value.raw;
                    }
                    return v;
                }));
            }
            @Override public void close() {
                Bindings bindings = contextEngine.bindings();
                Toast1<Integer> counter = Toast.of(0);
                bindings.forEach((key, value) -> globalValues.compute(key, (k, v) -> {
                    if (v == null || !Objects.equals(v.raw, value)) {
                        counter.val0++;
                        return new ValueCache(value);
                    }
                    return v;
                }));
            }
        }

        private static class LanguageAccessorProxy extends Accessor.LanguageSupport {
            public Accessor.LanguageSupport base;

            public static LanguageAccessorProxy create(Accessor.LanguageSupport base) {
                LanguageAccessorProxy proxy = unsafe.createInstance(LanguageAccessorProxy.class);
                proxy.base = base;
                return proxy;
            }

            @Override public void initializeLanguage(TruffleLanguage<?> impl, LanguageInfo language, Object polyglotLanguage, Object polyglotLanguageInstance) { base.initializeLanguage(impl,language, polyglotLanguage, polyglotLanguageInstance); }
            @Override public TruffleLanguage.Env createEnv(Object polyglotLanguageContext, TruffleLanguage<?> language, OutputStream stdOut, OutputStream stdErr, InputStream stdIn, Map<String, Object> config, OptionValues options, String[] applicationArguments) { return base.createEnv(polyglotLanguageContext, language, stdOut, stdErr, stdIn, config, options, applicationArguments); }
            @Override public boolean areOptionsCompatible(TruffleLanguage<?> language, OptionValues firstContextOptions, OptionValues newContextOptions) { return base.areOptionsCompatible(language, firstContextOptions, newContextOptions); }
            @Override public Object createEnvContext(TruffleLanguage.Env localEnv, List<Object> servicesCollector) { return base.createEnvContext(localEnv, servicesCollector); }
            @Override public TruffleContext createTruffleContext(Object impl, boolean creator) { return base.createTruffleContext(impl, creator); }
            @Override public void postInitEnv(TruffleLanguage.Env env) { base.postInitEnv(env); }
            @Override public Object evalInContext(Source source, Node node, MaterializedFrame frame) { return base.evalInContext(source, node, frame); }
            @Override public void dispose(TruffleLanguage.Env env) { base.dispose(env); }
            @Override public LanguageInfo getLanguageInfo(TruffleLanguage.Env env) { return base.getLanguageInfo(env); }
            @Override public LanguageInfo getLanguageInfo(TruffleLanguage<?> language) { return base.getLanguageInfo(language); }
            @Override public Object getPolyglotLanguageInstance(TruffleLanguage<?> language) { return base.getPolyglotLanguageInstance(language); }
            @Override public CallTarget parse(TruffleLanguage.Env env, Source code, Node context, String... argumentNames) { return base.parse(env, code, context, argumentNames); }
            @Override public ExecutableNode parseInline(TruffleLanguage.Env env, Source code, Node context, MaterializedFrame frame) { return base.parseInline(env, code, context, frame); }
            @Override public boolean isVisible(TruffleLanguage.Env env, Object value) { return base.isVisible(env, value); }
            @Override public Object getContext(TruffleLanguage.Env env) { return base.getContext(env); }
            @Override public Object getPolyglotLanguageContext(TruffleLanguage.Env env) { return base.getPolyglotLanguageContext(env); }
            @Override public TruffleLanguage<?> getSPI(TruffleLanguage.Env env) { return base.getSPI(env); }
            @Override public InstrumentInfo createInstrument(Object polyglotInstrument, String id, String name, String version) { return base.createInstrument(polyglotInstrument, id, name, version); }
            @Override public Object getPolyglotInstrument(InstrumentInfo info) { return base.getPolyglotInstrument(info); }
            @Override public boolean isContextInitialized(TruffleLanguage.Env env) { return base.isContextInitialized(env); }
            @Override public OptionDescriptors describeOptions(TruffleLanguage<?> language, String requiredGroup) { return base.describeOptions(language, requiredGroup); }
            @Override public void onThrowable(Node callNode, RootCallTarget root, Throwable e, Frame frame) { base.onThrowable(callNode, root, e, frame); }
            @Override public boolean isThreadAccessAllowed(TruffleLanguage.Env env, Thread current, boolean singleThread) {
                return true;//base.isThreadAccessAllowed(env, current, singleThread);
            }
            @Override public void initializeThread(TruffleLanguage.Env env, Thread current) { base.initializeThread(env, current); }
            @Override public void initializeMultiThreading(TruffleLanguage.Env env) { base.initializeMultiThreading(env); }
            @Override public void disposeThread(TruffleLanguage.Env env, Thread thread) { base.disposeThread(env, thread); }
            @Override public void finalizeContext(TruffleLanguage.Env localEnv) { base.finalizeContext(localEnv); }
            @Override public void exitContext(TruffleLanguage.Env localEnv, TruffleLanguage.ExitMode exitMode, int exitCode) { base.exitContext(localEnv, exitMode, exitCode); }
            @Override public TruffleLanguage.Env patchEnvContext(TruffleLanguage.Env env, OutputStream stdOut, OutputStream stdErr, InputStream stdIn, Map<String, Object> config, OptionValues options, String[] applicationArguments) { return base.patchEnvContext(env, stdOut, stdErr, stdIn, config, options, applicationArguments); }
            @Override public void initializeMultiContext(TruffleLanguage<?> language) { base.initializeMultiContext(language); }
            @Override public boolean isTruffleStackTrace(Throwable t) { return base.isTruffleStackTrace(t); }
            @Override public StackTraceElement[] getInternalStackTraceElements(Throwable t) { return base.getInternalStackTraceElements(t); }
            @Override public Throwable getOrCreateLazyStackTrace(Throwable t) { return base.getOrCreateLazyStackTrace(t); }
            @Override public void configureLoggers(Object polyglotContext, Map<String, Level> logLevels, Object... loggers) { base.configureLoggers(polyglotContext, logLevels, loggers); }
            @Override public Object getDefaultLoggers() { return base.getDefaultLoggers(); }
            @Override public Object createEngineLoggers(Object spi) { return base.createEngineLoggers(spi); }
            @Override public Object getLoggersSPI(Object loggerCache) { return base.getLoggersSPI(loggerCache); }
            @Override public void closeEngineLoggers(Object loggers) { base.closeEngineLoggers(loggers); }
            @Override public TruffleLogger getLogger(String id, String loggerName, Object loggers) { return base.getLogger(id, loggerName, loggers); }
            @Override public Object getLoggerCache(TruffleLogger logger) { return base.getLoggerCache(logger); }
            @Override public TruffleLanguage<?> getLanguage(TruffleLanguage.Env env) { return base.getLanguage(env); }
            @Override public Object createFileSystemContext(Object engineObject, FileSystem fileSystem) { return base.createFileSystemContext(engineObject, fileSystem); }
            @Override public String detectMimeType(TruffleFile file, Set<String> validMimeTypes) { return base.detectMimeType(file, validMimeTypes); }
            @Override public Charset detectEncoding(TruffleFile file, String mimeType) { return base.detectEncoding(file, mimeType); }
            @Override public TruffleFile getTruffleFile(String path, Object fileSystemContext) { return base.getTruffleFile(path, fileSystemContext); }
            @Override public boolean isSocketIOAllowed(Object fileSystemContext) { return base.isSocketIOAllowed(fileSystemContext); }
            @Override public TruffleFile getTruffleFile(Object context, String path) { return base.getTruffleFile(context, path); }
            @Override public TruffleFile getTruffleFile(Object context, URI uri) { return base.getTruffleFile(context, uri); }
            @Override public FileSystem getFileSystem(TruffleFile truffleFile) { return base.getFileSystem(truffleFile); }
            @Override public Path getPath(TruffleFile truffleFile) { return base.getPath(truffleFile); }
            @Override public Object getLanguageView(TruffleLanguage.Env env, Object value) { return base.getLanguageView(env, value); }
            @Override public Object getFileSystemContext(TruffleFile file) { return base.getFileSystemContext(file); }
            @Override public Object getFileSystemEngineObject(Object fileSystemContext) { return base.getFileSystemEngineObject(fileSystemContext); }
            @Override public Object getPolyglotContext(TruffleContext context) { return base.getPolyglotContext(context); }
            @Override public Object invokeContextLocalFactory(Object factory, Object contextImpl) { return base.invokeContextLocalFactory(factory, contextImpl); }
            @Override public Object invokeContextThreadLocalFactory(Object factory, Object contextImpl, Thread thread) { return base.invokeContextThreadLocalFactory(factory, contextImpl, thread); }
            @Override public Object getScope(TruffleLanguage.Env env) { return base.getScope(env); }
            @Override public boolean isSynchronousTLAction(ThreadLocalAction action) { return base.isSynchronousTLAction(action); }
            @Override public boolean isSideEffectingTLAction(ThreadLocalAction action) { return base.isSideEffectingTLAction(action); }
            @Override public boolean isRecurringTLAction(ThreadLocalAction action) { return base.isRecurringTLAction(action); }
            @Override public void performTLAction(ThreadLocalAction action, ThreadLocalAction.Access access) { base.performTLAction(action, access); }
            @Override public OptionDescriptors createOptionDescriptorsUnion(OptionDescriptors... descriptors) { return base.createOptionDescriptorsUnion(descriptors); }
        }

        static {
            try {
                Field field = reflection.get(Class.forName("com.oracle.truffle.polyglot.EngineAccessor"), "LANGUAGE");
                field = reflection.nonFinal(reflection.access(field));
                Accessor.LanguageSupport base = (Accessor.LanguageSupport)field.get(null);
                field.set(null, LanguageAccessorProxy.create(base));
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        private final ConcurrentHashMap<String, ValueCache> globalValues = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Long, ContextEngine> threads = new ConcurrentHashMap<>();

        private ContextEngine getContextEngine() {
            return threads.computeIfAbsent(Thread.currentThread().threadId(), id -> {
                try {
                    GraalJSScriptEngine engine = GraalJSScriptEngine.create(
                            Engine.newBuilder()
                            .option("engine.WarnInterpreterOnly", "false")
                            .build(),
                            Context.newBuilder()
                            .allowHostAccess(HostAccess.ALL)
                            .allowHostClassLookup(v -> true)
                            .logHandler(System.out));
                    Context context = engine.getPolyglotContext();
                    Map<?,?> global = (Map<?, ?>)engine.eval("this");
                    return new ContextEngine(id, engine, context, engine.getContext().getBindings(ScriptContext.ENGINE_SCOPE), global);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            });
        }

        public ContextFrame frame() {
            ContextFrame frame = new ContextFrame(getContextEngine(), globalValues);
            frame.open();
            return frame;
        }

        public void clear() {
            threads.entrySet().removeIf(kv -> {
                kv.getValue().context().close(true);
                return true;
            });
            globalValues.clear();
        }

        @Override public void close() {
            threads.forEach((k,v) -> v.context.close());
        }
    }

    private static ContextMultithreading context;

    public static CoreElement create() { return new JavaScript()._create(); }
    private CoreElement _create() {
        return CoreElement.create(JavaScript.class)
                .withInstance(this)
                .withInit(this::init)
                .withUninit(this::uninit)
                .addText("global.js", v -> v.withInvoke(this::config));
    }

    private core base_core = null;
    @Override public void core(core base_core) { this.base_core = base_core; }
    @Override public core core() { return base_core; }

    public static abstract class InstanceJS {
        public final ConcurrentLinkedQueue<BukkitTask> inits = new ConcurrentLinkedQueue<>();
    }

    public final ConcurrentHashMap<String, InstanceJS> instances = new ConcurrentHashMap<>();

    private static final String FUNCTION_PREFIX = "FUNCTION ";
    private static final int FUNCTION_PREFIX_LENGTH = FUNCTION_PREFIX.length();
    private Object invokeFunction(GraalJSScriptEngine engine, Map<?, ?> global, String methodPath) throws Exception {
        return invokeFunction(engine, global, methodPath, null);
    }
    private Object invokeFunction(GraalJSScriptEngine engine, Map<?, ?> global, String methodPath, @Nullable Object[] args) throws Exception {
        if (!methodPath.contains("."))
            return args == null ? engine.invokeFunction(methodPath) : engine.invokeFunction(methodPath, args);
        Map<?, ?> partElement = global;
        for (String part : methodPath.split(Pattern.quote(".")))
            partElement = (Map<?, ?>)partElement.get(part);
        Function<Object[], Object> function = (Function<Object[], Object>) partElement;
        return args == null ? function.apply(new Object[0]) : function.apply(args);
    }
    private Object eval(String script) throws Exception {
        return eval(script, null);
    }
    private Object eval(String script, @Nullable Map<String, Object> values) throws Exception {
        try (var frame = context.frame()) {
            GraalJSScriptEngine engine = frame.engine();
            var global = frame.global();

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

            //engine.getBindings(ScriptContext.ENGINE_SCOPE).forEach((k,v) -> System.out.println(k + ": " + v));

            //JSObject global = (JSObject)engine.get(GraalJSScriptEngine.NASHORN_GLOBAL);
            if (methodArgsLine.length() == 0) return invokeFunction(engine, global, methodName);

            String[] methodArgs = script.substring(startFunc + 1, endFunc).split(",");
            int argsLength = methodArgs.length;
            Object[] args = new Object[argsLength];
            for (int i = 0; i < argsLength; i++) {
                String arg = methodArgs[i].trim();
                args[i] = values != null && values.containsKey(arg)
                        ? values.get(arg)
                        : global.get(arg);
            }
            return invokeFunction(engine, global, methodName, args);
        }
    }

    public void init() {
        instances.values().forEach(v -> v.inits.removeIf(BukkitTask::isCancelled));
    }
    public void uninit() {
        context.close();
    }
    public void reinstance(boolean cancel) {
        try (var frame = context.frame()) {
            var engine = frame.engine();
            if (cancel) instances.values().forEach(v -> v.inits.forEach(BukkitTask::cancel));
            instances.forEach(engine::put);
        }
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

        if (context != null) context.clear();
        else context = new ContextMultithreading();

        try (var frame = context.frame()) {
            var engine = frame.engine();
            //engine.getContext().setBindings(new SimpleBindings(new ConcurrentHashMap<>()), ScriptContext.ENGINE_SCOPE);
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
    }

    public Optional<Boolean> isJsTrue(String js) { return isJsTrue(js, Collections.emptyMap()); }
    public Optional<Boolean> isJsTrue(String js, Map<String, Object> values) {
        try
        {
            Object value = eval(js, values);
            return Optional.of(value instanceof Boolean bool ? bool : value.equals(1));
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            return Optional.empty();
        }
    }

    public Optional<Number> getJsNumber(String js) { return getJsNumber(js, Collections.emptyMap()); }
    public Optional<Number> getJsNumber(String js, Map<String, Object> values) {
        try
        {
            return Optional.ofNullable((Number)eval(js, values));
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            return Optional.empty();
        }
    }

    public Optional<Integer> getJsInt(String js) { return getJsInt(js, Collections.emptyMap()); }
    public Optional<Integer> getJsInt(String js, Map<String, Object> values) { return getJsNumber(js, values).map(Number::intValue); }

    public Optional<Double> getJsDouble(String js) { return getJsDouble(js, Collections.emptyMap()); }
    public Optional<Double> getJsDouble(String js, Map<String, Object> values) { return getJsNumber(js, values).map(Number::doubleValue); }

    public Optional<String> getJsString(String js) { return getJsString(js, Collections.emptyMap()); }
    public Optional<String> getJsString(String js, Map<String, Object> values) {
        try
        {
            Object value = eval(js, values);
            return Optional.of(value instanceof String str ? str : value.toString());
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            return Optional.empty();
        }
    }

    public Optional<JsonElement> getJsJson(String js) { return getJsJson(js, Collections.emptyMap()); }
    public Optional<JsonElement> getJsJson(String js, Map<String, Object> values) {
        try
        {
            return Optional.of(json.by(eval(js, values)).build());
            /*
            return invoke("JSON.stringify(value)", Collections.singletonMap("value", eval(js, values)))
                .map(value -> value instanceof String str ? str : value.toString())
                .map(json::parse);
            */
        }
        catch (Exception e) {
            base_core._logOP("JS ERROR");
            base_core._logOP("JS:\n" + js);
            base_core._logStackTrace(e);
            return Optional.empty();
        }
    }

    public void getJsStringNext(String js, Action1<String> callback) {
        try
        {
            Object value = eval(js);
            if (value == null) callback.invoke(null);
            else if (value instanceof Function) ((Function<Object[], Object>)value).apply(new Object[] { (Action1<Object>) v -> callback.invoke(v instanceof String str ? str : v.toString()) });
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

    private record ArgsBinding(Map<String, Object> args, Bindings owner) implements Bindings {
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





















