package org.lime;

import com.google.common.collect.Streams;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("unchecked")
public class system
{
    private final static Random rnd = new Random();

    public static class Lock {
        private final ReentrantLock _lock;
        public Lock() { _lock = new ReentrantLock(); }
        public static Lock create() { return new Lock(); }
        public system.Action0 lock() {
            _lock.lock();
            return _lock::unlock;
        }
        public void invoke(system.Action0 invoke) {
            try (system.Action0 a = lock()) {
                invoke.invoke();
            }
        }
        public <T>T invoke(system.Func0<T> invoke) {
            try (system.Action0 a = lock()) {
                return invoke.invoke();
            }
        }
    }

    public static <T, TAny, TRet>void waitAllAnyAsyns(Collection<system.Toast2<T, TAny>> list, system.Action2<T, system.Action1<TRet>> func, system.Action1<List<system.Toast3<T, TAny, TRet>>> callback) {
        var locked = system.toast(0).lock();
        List<system.Toast3<T, TAny, TRet>> ret = new ArrayList<>();
        for (system.Toast2<T, TAny> item : list) {
            locked.edit0(v -> v + 1);
            int index = ret.size();
            ret.add(system.toast(item.val0, item.val1, null));
            func.invoke(item.val0, v -> {
                ret.get(index).val2 = v;
                if (locked.edit0(j -> j - 1) > 0) return;
                callback.invoke(ret);
            });
        }
        if (ret.size() == 0) callback.invoke(ret);
    }

    private static final ConcurrentHashMap<String, Pattern> patterns = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<system.Toast2<String, String>, Boolean> comparers = new ConcurrentHashMap<>();
    public static void tryClearCompare() {
        if (comparers.size() > 100000)
            comparers.clear();
    }
    public static boolean compareRegex(String input, String regex) {
        return comparers.compute(system.toast(input, regex), (k,v) -> {
            if (v != null) return v;
            Pattern pattern = patterns.getOrDefault(regex, null);
            if (pattern == null) patterns.put(regex, pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            return pattern.matcher(input).matches();
        });
    }
    public static <T>T getOrDefault(List<T> list, int index, T def) {
        return index < list.size() ? list.get(index) : def;
    }

    public static <T>void parseAdd(JsonObject json, String key, Collection<T> list, Collection<T> data, system.Func1<T, String> name) {
        parseList(json, key, addByList(list, data, name));
    }
    public static <T>List<T> parseGet(JsonObject json, String key, Collection<T> list, system.Func1<T, String> name) {
        List<T> data = new ArrayList<>();
        parseAdd(json, key, data, list, name);
        return data;
    }
    public static void parseList(JsonObject json, String key, system.Action2<JsonArray, Boolean> callback) {
        if (json.has(key)) callback.invoke(json.get(key).getAsJsonArray(), true);
        else if (json.has("!" + key)) callback.invoke(json.get("!" + key).getAsJsonArray(), false);
    }
    private static Stream<String> getStringsInArrayDeep(JsonArray array) {
        return Streams.stream(array.iterator()).flatMap(v -> v.isJsonObject()
                ? v.getAsJsonObject()
                    .entrySet()
                    .stream()
                    .map(Map.Entry::getValue)
                    .map(JsonElement::getAsJsonArray)
                    .flatMap(system::getStringsInArrayDeep)
                : v.isJsonArray()
                    ? getStringsInArrayDeep(v.getAsJsonArray())
                    : Stream.of(v.getAsString()));
    }
    public static <T>system.Action2<JsonArray, Boolean> addByList(Collection<T> list, Collection<T> data, system.Func1<T, String> name) {
        return (json, add_to_empty) -> {
            if (add_to_empty) {
                HashMap<T, Boolean> _list = new HashMap<>();
                getStringsInArrayDeep(json).forEach(item -> {
                    for (T material : data) {
                        if (compareRegex(name.invoke(material), item)) {
                            _list.put(material, true);
                        }
                    }
                });
                list.addAll(_list.keySet());
            } else {
                List<T> _list = new ArrayList<>(data);
                _list.removeIf(material -> {
                    for (String item : getStringsInArrayDeep(json).toList()) {
                        if (compareRegex(name.invoke(material), item))
                            return true;
                    }
                    return false;
                });
                list.addAll(_list);
            }
        };
    }

    @SuppressWarnings("deprecation")
    public static class json {
        private static final JsonParser parser = new JsonParser();
        public static JsonElement parse(String json) { return parser.parse(json); }
        public static JsonElement parse(Reader json) { return parser.parse(json); }
        public static JsonElement parse(JsonReader json) { return parser.parse(json); }

        public static abstract class builder<T extends JsonElement> {
            private static List<Object> toList(Object array) {
                int length = Array.getLength(array);
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < length; i++) list.add(Array.get(array, i));
                return list;
            }
            public static builder<?> byObject(Object value) {
                if (value == null) return element.create();
                else if (value instanceof builder<?> dat) return dat;
                else if (value instanceof String dat) return element.create(dat);
                else if (value instanceof Number dat) return element.create(dat);
                else if (value instanceof Boolean dat) return element.create(dat);
                else if (value instanceof Character dat) return element.create(dat);
                else if (value instanceof java.lang.Enum<?> dat) return element.create(dat.name());
                else if (value instanceof JsonElement dat) return element.create(dat);
                else if (value instanceof Map<?, ?> dat) return object().add(dat, Object::toString, v -> v);
                else if (value instanceof Iterable<?> dat) return array().add(dat, v -> v);
                else if (value.getClass().isArray()) return byObject(toList(value));
                else return element.create(value.toString());
            }

            public static class element extends builder<JsonElement> {
                private final JsonElement json;

                private element(JsonElement json) { this.json = json; }

                private element() { this(JsonNull.INSTANCE); }
                private element(String value) { this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value)); }
                private element(Number value) { this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value)); }
                private element(Boolean value) { this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value)); }
                private element(Character value) { this(value == null ? JsonNull.INSTANCE : new JsonPrimitive(value)); }

                public static element create() { return new element(); }

                public static element create(JsonElement value) { return new element(value); }
                public static element create(builder<?> value) { return new element(value.build()); }
                public static element create(String value) { return new element(value); }
                public static element create(Number value) { return new element(value); }
                public static element create(Boolean value) { return new element(value); }
                public static element create(Character value) { return new element(value); }

                @Override public JsonElement build() { return json; }
            }
            public static class object extends builder<JsonObject> {
                private final JsonObject json;
                private object(JsonObject json) { this.json = json; }
                private object() { this(new JsonObject()); }

                public object addObject(String key, system.Func1<object, object> value) {
                    return add(key, value.invoke(new object()));
                }
                public object addArray(String key, system.Func1<array, array> value) {
                    return add(key, value.invoke(new array()));
                }

                public <TValue>object add(Map<String, TValue> map) {
                    map.forEach(this::add);
                    return this;
                }
                public <TKey, TValue>object add(Map<TKey, TValue> map, system.Func1<TKey, String> key, system.Func1<TValue, Object> value) {
                    map.forEach((k,v) -> add(key.invoke(k), value.invoke(v)));
                    return this;
                }
                public <T>object add(Iterable<T> list, system.Func1<T, String> key, system.Func1<T, Object> value) {
                    list.forEach(i -> add(key.invoke(i), value.invoke(i)));
                    return this;
                }
                public <T>object add(Iterator<T> list, system.Func1<T, String> key, system.Func1<T, Object> value) {
                    list.forEachRemaining(i -> add(key.invoke(i), value.invoke(i)));
                    return this;
                }

                public object addNull(String key) { return add(key, byObject(null)); }
                public object add(String key, JsonElement value) {
                    json.add(key, value);
                    return this;
                }
                public object add(String key, builder<?> value) {
                    return value == null ? addNull(key) : add(key, value.build());
                }
                public object add(String key, Object value) {
                    return add(key, byObject(value));
                }
                public object add(JsonObject json) {
                    json.entrySet().forEach(kv -> this.add(kv.getKey(), kv.getValue()));
                    return this;
                }
                public object add(object json) {
                    return add(json.build());
                }

                @Override public JsonObject build() { return json; }
            }
            public static class array extends builder<JsonArray> {
                private final JsonArray json;
                private array(JsonArray json) { this.json = json; }
                private array() { this(new JsonArray()); }

                public <T>array add(Iterable<T> items) {
                    items.forEach(this::add);
                    return this;
                }
                public <In, T>array add(Iterable<In> items, system.Func1<In, T> format) {
                    items.forEach(item -> add(format.invoke(item)));
                    return this;
                }
                public <T>array add(Iterator<T> items) {
                    items.forEachRemaining(this::add);
                    return this;
                }
                public <In, T>array add(Iterator<In> items, system.Func1<In, T> format) {
                    items.forEachRemaining(item -> add(format.invoke(item)));
                    return this;
                }
                public <In, T>array add(In[] items, system.Func1<In, T> format) {
                    for (In item : items) add(format.invoke(item));
                    return this;
                }
                public array addObject(system.Func1<object, object> value) {
                    return add(value.invoke(new object()));
                }
                public array addArray(system.Func1<array, array> value) {
                    return add(value.invoke(new array()));
                }

                public array addNull() { return add(byObject(null)); }
                public array add(JsonElement value) {
                    json.add(value);
                    return this;
                }
                public array add(builder<?> value) {
                    return add(value.build());
                }
                public array add(Object value) {
                    return add(byObject(value));
                }

                @Override public JsonArray build() { return json; }
            }

            public static class getter extends builder<JsonElement> {
                private final List<system.Toast2<String, system.Func1<JsonElement, JsonElement>>> list = new ArrayList<>();
                private final JsonElement base;
                private getter(JsonElement base) { this.base = base; }

                public getter of(int index) {
                    list.add(system.toast("["+index+"]", json -> json != null && json.isJsonArray() ? json.getAsJsonArray().get(index) : null));
                    return this;
                }
                public getter last() {
                    list.add(system.toast("[last]", json -> {
                        if (!json.isJsonArray()) return null;
                        JsonArray arr = json.getAsJsonArray();
                        int length = arr.size();
                        return length == 0 ? null : arr.get(length - 1);
                    }));
                    return this;
                }
                public getter of(String key) {
                    list.add(system.toast("."+key, json -> json != null && json.isJsonObject() ? json.getAsJsonObject().get(key) : null));
                    return this;
                }

                @Override public JsonElement build() {
                    JsonElement json = base;
                    for (system.Toast2<String, system.Func1<JsonElement, JsonElement>> item : list) json = item.val1.invoke(json);
                    return json;
                }

                public JsonArray array() {
                    JsonElement json = build();
                    return json == null || !json.isJsonArray() ? null : json.getAsJsonArray();
                }
                public JsonObject object() {
                    JsonElement json = build();
                    return json == null || !json.isJsonObject() ? null : json.getAsJsonObject();
                }
                public JsonPrimitive primitive() {
                    JsonElement json = build();
                    return json == null || !json.isJsonPrimitive() ? null : json.getAsJsonPrimitive();
                }
                public <T>T other(system.Func1<JsonElement, T> parse, T def) {
                    return otherFunc(parse, () -> def);
                }
                public <T>T other(system.Func1<JsonElement, T> parse) {
                    return otherFunc(parse, () -> {
                        throw new IllegalArgumentException("Path 'JSON."+list.stream().map(v -> v.val0).collect(Collectors.joining())+"' not founded!");
                    });
                }
                public <T>T otherFunc(system.Func1<JsonElement, T> parse, system.Func0<T> def) {
                    JsonElement json = build();
                    if (json == null) return def.invoke();
                    try { return parse.invoke(json); }
                    catch (Exception e) { return def.invoke(); }
                }
            }

            public abstract T build();
        }

        public static builder.object object() { return new builder.object(); }
        public static builder.array array() { return new builder.array(); }

        public static builder<?> by(Object obj) { return builder.byObject(obj); }

        public static builder.object of(JsonObject json) { return new builder.object(json); }
        public static builder.array of(JsonArray json) { return new builder.array(json); }

        public static JsonObject object(system.Func1<builder.object, builder.object> func) { return func.invoke(object()).build(); }
        public static JsonArray array(system.Func1<builder.array, builder.array> func) { return func.invoke(array()).build(); }

        public static builder.getter getter(JsonElement json) { return new builder.getter(json); }
    }
    public static class map {
        public static class builder<TKey, TValue> {
            private final HashMap<TKey, TValue> map;

            private builder() { this(false); }
            private builder(boolean linked) { this(linked ? new LinkedHashMap<>() : new HashMap<>()); }
            private builder(HashMap<TKey, TValue> map) { this.map = map; }

            public builder<TKey, TValue> add(TKey key, TValue value) { this.map.put(key, value); return this; }
            public builder<TKey, TValue> add(Map.Entry<TKey, TValue> entry) { return add(entry.getKey(), entry.getValue()); }
            public builder<TKey, TValue> add(system.Toast2<TKey, TValue> entry) { return add(entry.val0, entry.val1); }
            public builder<TKey, TValue> add(Map<TKey, TValue> map) { this.map.putAll(map); return this; }
            public builder<TKey, TValue> add(Iterable<system.Toast2<TKey, TValue>> map) { map.forEach(this::add); return this; }
            public builder<TKey, TValue> add(Iterable<TKey> keys, TValue value) { keys.forEach(key -> add(key, value)); return this; }
            public builder<TKey, TValue> add(Iterator<system.Toast2<TKey, TValue>> map) { map.forEachRemaining(this::add); return this; }
            public builder<TKey, TValue> add(Iterator<TKey> keys, TValue value) { keys.forEachRemaining(key -> add(key, value)); return this; }

            public <T>builder<TKey, TValue> add(Iterable<T> list, system.Func1<T, TKey> key, system.Func1<T, TValue> value) {
                list.forEach(item -> add(key.invoke(item), value.invoke(item)));
                return this;
            }
            public <T>builder<TKey, TValue> add(Iterator<T> list, system.Func1<T, TKey> key, system.Func1<T, TValue> value) {
                list.forEachRemaining(item -> add(key.invoke(item), value.invoke(item)));
                return this;
            }
            public <TTKey, TTValue>builder<TKey, TValue> add(Map<TTKey, TTValue> map, system.Func1<TTKey, TKey> key, system.Func1<TTValue, TValue> value) {
                map.forEach((k,v) -> add(key.invoke(k), value.invoke(v)));
                return this;
            }

            public HashMap<TKey, TValue> build() { return map; }

            public builder<TKey, TValue> copy() {
                return new builder<TKey, TValue>().add(map);
            }
        }

        public static <TKey, TValue>builder<TKey, TValue> of(Class<TKey> tKey, Class<TValue> tValue) { return new builder<>(); }
        public static <TKey, TValue>builder<TKey, TValue> of() { return new builder<>(); }
        public static <TKey, TValue>builder<TKey, TValue> of(TKey key, TValue value) { return map.<TKey, TValue>of().add(key, value); }

        public static <TKey, TValue>builder<TKey, TValue> of(Class<TKey> tKey, Class<TValue> tValue, boolean linked) { return new builder<>(linked); }
        public static <TKey, TValue>builder<TKey, TValue> of(boolean linked) { return new builder<>(linked); }
        public static <TKey, TValue>builder<TKey, TValue> of(TKey key, TValue value, boolean linked) { return map.<TKey, TValue>of(linked).add(key, value); }

        public static <TKey, TValue>builder<TKey, TValue> of(HashMap<TKey, TValue> value) { return new builder<>(value); }
    }
    public static class list {
        public static class builder<T> {
            private final List<T> list = new ArrayList<>();

            @SuppressWarnings("all")
            public builder<T> add(T... items) { Collections.addAll(list, items); return this; }
            public builder<T> add(Iterable<T> items) { items.forEach(list::add); return this; }
            public <In>builder<T> add(Iterable<In> items, system.Func1<In, T> func) { items.forEach(v -> list.add(func.invoke(v))); return this; }

            public List<T> build() { return list; }
            public builder<T> copy() { return new builder<T>().add(list); }
        }

        public static <T>builder<T> of(Class<T> tClass) { return new builder<>(); }
        public static <T>builder<T> of() { return new builder<>(); }
    }

    public static int rand(int min, int max) {
        int _min = Math.min(min, max);
        int _max = Math.max(min, max);
        return rnd.nextInt((_max - _min) + 1) + _min;
    }
    public static int rand(system.Toast2<Integer, Integer> minmax) {
        return rand(minmax.val0, minmax.val1);
    }
    public static double rand(double min, double max) {
        double _min = Math.min(min, max);
        double _max = Math.max(min, max);
        return _min + (_max - _min) * rnd.nextDouble();
    }
    public static <T> T rand(Collection<T> array) {
        Object[] arr = array.toArray();
        return (T) arr[rand(0, arr.length - 1)];
    }
    public static boolean rand_is(double value) {
        return value <= 0
                ? false
                : value >= 1
                    ? true
                    : rnd.nextDouble() <= value;
    }
    public static boolean rand() { return rand_is(0.5); }
    public static <T>T rand(T... array) {
        return array[rand(0, array.length - 1)];
    }
    public static <T>void randomize(List<T> list) { Collections.shuffle(list, rnd); }

    public static <T>Iterable<T> iterable(Stream<T> stream) {
        return stream::iterator;
    }

    public static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public interface IJson<T extends JsonElement> {
        abstract class ILoad<T extends JsonElement> {
            protected ILoad(T json) { }
            public static <T extends JsonElement, I extends ILoad<T>>I parse(system.Func1<T, I> parse, T json) {
                return parse.invoke(json);
            }
            public static <T extends JsonElement, I extends ILoad<T>>List<I> parse(system.Func1<T, I> parse, JsonArray json) {
                List<I> list = new ArrayList<>();
                json.forEach(item -> list.add(parse.invoke((T)item)));
                return list;
            }
            public static <T extends JsonElement, I extends ILoad<T>>HashMap<String, I> parse(system.Func1<T, I> parse, JsonObject json) {
                return parse(parse, json, v -> v);
            }
            public static <TKey, T extends JsonElement, I extends ILoad<T>>HashMap<TKey, I> parse(system.Func1<T, I> parse, JsonObject json, system.Func1<String, TKey> key) {
                HashMap<TKey, I> list = new HashMap<>();
                json.entrySet().forEach(kv -> list.put(key.invoke(kv.getKey()), parse.invoke((T)kv.getValue())));
                return list;
            }
        }

        T toJson();
        static <TValue extends IJson<?>>JsonObject toJson(Map<String, TValue> map) {
            return toJson(map, v -> v);
        }
        static <TKey, TValue extends IJson<?>>JsonObject toJson(Map<TKey, TValue> map, system.Func1<TKey, String> key) {
            JsonObject json = new JsonObject();
            map.forEach((k,v) -> json.add(key.invoke(k), v.toJson()));
            return json;
        }
        static <TValue extends IJson<?>>JsonArray toJson(List<TValue> list) {
            JsonArray json = new JsonArray();
            list.forEach(v -> json.add(v.toJson()));
            return json;
        }
    }

    @SuppressWarnings("all")
    public interface cancel extends Action0 { default void cancel() { invoke(); } }

    private static <T>T invoke(Method method, Object instance, Object[] args) {
        try { return (T)method.invoke(instance, args); }
        catch (Exception e) { throw new IllegalArgumentException(e); }
    }

    public interface ICallable {
        Object call(Object[] args);

        default Object createObjectProxy(Class<?> tClass, String method) { return createObjectProxy(tClass, method, this); }
        default <T>T createProxy(Class<T> tClass, String method) { return createProxy(tClass, method, this); }

        static <T>T createProxy(Class<T> tClass, String method, ICallable executor) {
            return (T)createObjectProxy(tClass, method, executor);
        }
        static Object createObjectProxy(Class<?> tClass, String method, ICallable executor) {
            return java.lang.reflect.Proxy.newProxyInstance(
                    tClass.getClassLoader(),
                    new java.lang.Class[] { tClass },
                    (proxy, method1, args) -> {
                        String method_name = method1.getName();
                        if (!method_name.equals(method)) return false;
                        return executor.call(args);
                    });
        }
    }

    public interface Action0 extends core.ITimers.IRunnable, AutoCloseable, ICallable {
        void invoke();
        @Override default void run() { invoke(); }
        @Override default void close() { invoke(); }
        @Override default Object call(Object[] args) { invoke(); return null; }
        static Action0 of(Method method) { return () -> system.invoke(method, null, new Object[0]); }
    }
    public interface Action1<T0> extends Consumer<T0>, ICallable {
        void invoke(T0 arg0);
        @Override default void accept(T0 t0) {
            invoke(t0);
        }
        @Override default Object call(Object[] args) { invoke((T0)args[0]); return null; }
        default Action1<T0> andThen(Action1<? super T0> after) { return (T0 t) -> { invoke(t); after.invoke(t); }; }
        static <T0>Action1<T0> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    t0 -> system.invoke(method, null, new Object[] { t0 }) :
                    t0 -> system.invoke(method, t0, new Object[] {  });
        }
    }
    public interface Func0<TResult> extends ICallable, Supplier<TResult> {
        TResult invoke();
        @Override default Object call(Object[] args) { return invoke(); }
        @Override default TResult get() { return invoke(); }
    }
    public interface Func1<T0, TResult> extends Function<T0, TResult>, ICallable {
        TResult invoke(T0 arg0);
        @Override default TResult apply(T0 t0) { return invoke(t0); }
        @Override default Object call(Object[] args) { return invoke((T0)args[0]); }
        default Func1<T0, TResult> and(Func1<T0, TResult> after, Func2<TResult, TResult, TResult> combine) {
            return (T0 t) -> combine.invoke(invoke(t), after.invoke(t));
        }
    }

    public static Action0 action(Action0 action) { return action; }
    public static <T0>Action1<T0> action(Action1<T0> action) { return action; }
    public static ActionEx0 actionEx(ActionEx0 action) { return action; }
    public static <T0>ActionEx1<T0> actionEx(ActionEx1<T0> action) { return action; }

    public interface ActionEx0 {
        void invoke() throws Throwable;
        default Action0 throwable() {
            return () -> { try { this.invoke(); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
    }
    public interface ActionEx1<T0> {
        void invoke(T0 arg0) throws Throwable;
        default Action1<T0> throwable() {
            return (arg0) -> { try { this.invoke(arg0); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
    }
    public interface FuncEx0<TResult> {
        TResult invoke() throws Throwable;
        default Func0<TResult> throwable() {
            return () -> { try { return this.invoke(); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func0<Optional<TResult>> optional() {
            return () -> { try { return Optional.ofNullable(this.invoke()); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public interface FuncEx1<T0, TResult> {
        TResult invoke(T0 arg0) throws Throwable;
        default Func1<T0, TResult> throwable() {
            return (arg0) -> { try { return this.invoke(arg0); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func1<T0, Optional<TResult>> optional() {
            return (arg0) -> { try { return Optional.ofNullable(this.invoke(arg0)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }

    public static <TResult>Func0<TResult> func(Func0<TResult> action) { return action; }
    public static <T0, TResult>Func1<T0, TResult> func(Func1<T0, TResult> action) { return action; }
    public static <TResult>FuncEx0<TResult> funcEx(FuncEx0<TResult> action) { return action; }
    public static <T0, TResult>FuncEx1<T0, TResult> funcEx(FuncEx1<T0, TResult> action) { return action; }

    //<editor-fold desc="Actions & Funcs">
    //<generator name="system-invoke.js">
    public interface Action2<T0, T1> extends Action1<system.Toast2<T0,T1>> {
        void invoke(T0 arg0, T1 arg1);
        @Override default void invoke(system.Toast2<T0,T1> arg0) { invoke(arg0.val0, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1]); return null; }
        static <T0, T1>Action2<T0, T1> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1) -> system.invoke(method, null, new Object[] { val0, val1 }) :
                    (val0, val1) -> system.invoke(method, val0, new Object[] { val1 });
        }
    }
    public static <T0, T1>Action2<T0, T1> action(Action2<T0, T1> action) { return action; }
    public interface Func2<T0, T1, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1]); }
        static <T0, T1, TResult>Func2<T0, T1, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1) -> system.invoke(method, null, new Object[] { val0, val1 }) :
                    (val0, val1) -> system.invoke(method, val0, new Object[] { val1 });
        }
    }
    public static <T0, T1, TResult>Func2<T0, T1, TResult> func(Func2<T0, T1, TResult> func) { return func; }
    public interface ActionEx2<T0, T1> {
        void invoke(T0 arg0, T1 arg1) throws Throwable;
        default Action2<T0, T1> throwable() {
            return (val0, val1) -> { try { this.invoke(val0, val1); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func2<T0, T1, Boolean> optional() {
            return (val0, val1) -> { try { this.invoke(val0, val1); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1>ActionEx2<T0, T1> actionEx(ActionEx2<T0, T1> action) { return action; }
    public interface FuncEx2<T0, T1, TResult> {
        TResult invoke(T0 arg0, T1 arg1) throws Throwable;
        default Func2<T0, T1, TResult> throwable() {
            return (val0, val1) -> { try { return this.invoke(val0, val1); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func2<T0, T1, Optional<TResult>> optional() {
            return (val0, val1) -> { try { return Optional.ofNullable(this.invoke(val0, val1)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, TResult>FuncEx2<T0, T1, TResult> funcEx(FuncEx2<T0, T1, TResult> func) { return func; }
    
    public interface Action3<T0, T1, T2> extends Action1<system.Toast2<system.Toast2<T0,T1>,T2>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2);
        @Override default void invoke(system.Toast2<system.Toast2<T0,T1>,T2> arg0) { invoke(arg0.val0.val0, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2]); return null; }
        static <T0, T1, T2>Action3<T0, T1, T2> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2) -> system.invoke(method, null, new Object[] { val0, val1, val2 }) :
                    (val0, val1, val2) -> system.invoke(method, val0, new Object[] { val1, val2 });
        }
    }
    public static <T0, T1, T2>Action3<T0, T1, T2> action(Action3<T0, T1, T2> action) { return action; }
    public interface Func3<T0, T1, T2, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2]); }
        static <T0, T1, T2, TResult>Func3<T0, T1, T2, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2) -> system.invoke(method, null, new Object[] { val0, val1, val2 }) :
                    (val0, val1, val2) -> system.invoke(method, val0, new Object[] { val1, val2 });
        }
    }
    public static <T0, T1, T2, TResult>Func3<T0, T1, T2, TResult> func(Func3<T0, T1, T2, TResult> func) { return func; }
    public interface ActionEx3<T0, T1, T2> {
        void invoke(T0 arg0, T1 arg1, T2 arg2) throws Throwable;
        default Action3<T0, T1, T2> throwable() {
            return (val0, val1, val2) -> { try { this.invoke(val0, val1, val2); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func3<T0, T1, T2, Boolean> optional() {
            return (val0, val1, val2) -> { try { this.invoke(val0, val1, val2); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2>ActionEx3<T0, T1, T2> actionEx(ActionEx3<T0, T1, T2> action) { return action; }
    public interface FuncEx3<T0, T1, T2, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2) throws Throwable;
        default Func3<T0, T1, T2, TResult> throwable() {
            return (val0, val1, val2) -> { try { return this.invoke(val0, val1, val2); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func3<T0, T1, T2, Optional<TResult>> optional() {
            return (val0, val1, val2) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, TResult>FuncEx3<T0, T1, T2, TResult> funcEx(FuncEx3<T0, T1, T2, TResult> func) { return func; }
    
    public interface Action4<T0, T1, T2, T3> extends Action1<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3);
        @Override default void invoke(system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3> arg0) { invoke(arg0.val0.val0.val0, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3]); return null; }
        static <T0, T1, T2, T3>Action4<T0, T1, T2, T3> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3 }) :
                    (val0, val1, val2, val3) -> system.invoke(method, val0, new Object[] { val1, val2, val3 });
        }
    }
    public static <T0, T1, T2, T3>Action4<T0, T1, T2, T3> action(Action4<T0, T1, T2, T3> action) { return action; }
    public interface Func4<T0, T1, T2, T3, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3]); }
        static <T0, T1, T2, T3, TResult>Func4<T0, T1, T2, T3, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3 }) :
                    (val0, val1, val2, val3) -> system.invoke(method, val0, new Object[] { val1, val2, val3 });
        }
    }
    public static <T0, T1, T2, T3, TResult>Func4<T0, T1, T2, T3, TResult> func(Func4<T0, T1, T2, T3, TResult> func) { return func; }
    public interface ActionEx4<T0, T1, T2, T3> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3) throws Throwable;
        default Action4<T0, T1, T2, T3> throwable() {
            return (val0, val1, val2, val3) -> { try { this.invoke(val0, val1, val2, val3); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func4<T0, T1, T2, T3, Boolean> optional() {
            return (val0, val1, val2, val3) -> { try { this.invoke(val0, val1, val2, val3); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3>ActionEx4<T0, T1, T2, T3> actionEx(ActionEx4<T0, T1, T2, T3> action) { return action; }
    public interface FuncEx4<T0, T1, T2, T3, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3) throws Throwable;
        default Func4<T0, T1, T2, T3, TResult> throwable() {
            return (val0, val1, val2, val3) -> { try { return this.invoke(val0, val1, val2, val3); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func4<T0, T1, T2, T3, Optional<TResult>> optional() {
            return (val0, val1, val2, val3) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, TResult>FuncEx4<T0, T1, T2, T3, TResult> funcEx(FuncEx4<T0, T1, T2, T3, TResult> func) { return func; }
    
    public interface Action5<T0, T1, T2, T3, T4> extends Action1<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4);
        @Override default void invoke(system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4> arg0) { invoke(arg0.val0.val0.val0.val0, arg0.val0.val0.val0.val1, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4]); return null; }
        static <T0, T1, T2, T3, T4>Action5<T0, T1, T2, T3, T4> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4 }) :
                    (val0, val1, val2, val3, val4) -> system.invoke(method, val0, new Object[] { val1, val2, val3, val4 });
        }
    }
    public static <T0, T1, T2, T3, T4>Action5<T0, T1, T2, T3, T4> action(Action5<T0, T1, T2, T3, T4> action) { return action; }
    public interface Func5<T0, T1, T2, T3, T4, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4]); }
        static <T0, T1, T2, T3, T4, TResult>Func5<T0, T1, T2, T3, T4, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4 }) :
                    (val0, val1, val2, val3, val4) -> system.invoke(method, val0, new Object[] { val1, val2, val3, val4 });
        }
    }
    public static <T0, T1, T2, T3, T4, TResult>Func5<T0, T1, T2, T3, T4, TResult> func(Func5<T0, T1, T2, T3, T4, TResult> func) { return func; }
    public interface ActionEx5<T0, T1, T2, T3, T4> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4) throws Throwable;
        default Action5<T0, T1, T2, T3, T4> throwable() {
            return (val0, val1, val2, val3, val4) -> { try { this.invoke(val0, val1, val2, val3, val4); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func5<T0, T1, T2, T3, T4, Boolean> optional() {
            return (val0, val1, val2, val3, val4) -> { try { this.invoke(val0, val1, val2, val3, val4); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3, T4>ActionEx5<T0, T1, T2, T3, T4> actionEx(ActionEx5<T0, T1, T2, T3, T4> action) { return action; }
    public interface FuncEx5<T0, T1, T2, T3, T4, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4) throws Throwable;
        default Func5<T0, T1, T2, T3, T4, TResult> throwable() {
            return (val0, val1, val2, val3, val4) -> { try { return this.invoke(val0, val1, val2, val3, val4); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func5<T0, T1, T2, T3, T4, Optional<TResult>> optional() {
            return (val0, val1, val2, val3, val4) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, T4, TResult>FuncEx5<T0, T1, T2, T3, T4, TResult> funcEx(FuncEx5<T0, T1, T2, T3, T4, TResult> func) { return func; }
    
    public interface Action6<T0, T1, T2, T3, T4, T5> extends Action1<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5);
        @Override default void invoke(system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5> arg0) { invoke(arg0.val0.val0.val0.val0.val0, arg0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val1, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5]); return null; }
        static <T0, T1, T2, T3, T4, T5>Action6<T0, T1, T2, T3, T4, T5> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5 }) :
                    (val0, val1, val2, val3, val4, val5) -> system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5>Action6<T0, T1, T2, T3, T4, T5> action(Action6<T0, T1, T2, T3, T4, T5> action) { return action; }
    public interface Func6<T0, T1, T2, T3, T4, T5, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5]); }
        static <T0, T1, T2, T3, T4, T5, TResult>Func6<T0, T1, T2, T3, T4, T5, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5 }) :
                    (val0, val1, val2, val3, val4, val5) -> system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, TResult>Func6<T0, T1, T2, T3, T4, T5, TResult> func(Func6<T0, T1, T2, T3, T4, T5, TResult> func) { return func; }
    public interface ActionEx6<T0, T1, T2, T3, T4, T5> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) throws Throwable;
        default Action6<T0, T1, T2, T3, T4, T5> throwable() {
            return (val0, val1, val2, val3, val4, val5) -> { try { this.invoke(val0, val1, val2, val3, val4, val5); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func6<T0, T1, T2, T3, T4, T5, Boolean> optional() {
            return (val0, val1, val2, val3, val4, val5) -> { try { this.invoke(val0, val1, val2, val3, val4, val5); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5>ActionEx6<T0, T1, T2, T3, T4, T5> actionEx(ActionEx6<T0, T1, T2, T3, T4, T5> action) { return action; }
    public interface FuncEx6<T0, T1, T2, T3, T4, T5, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) throws Throwable;
        default Func6<T0, T1, T2, T3, T4, T5, TResult> throwable() {
            return (val0, val1, val2, val3, val4, val5) -> { try { return this.invoke(val0, val1, val2, val3, val4, val5); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func6<T0, T1, T2, T3, T4, T5, Optional<TResult>> optional() {
            return (val0, val1, val2, val3, val4, val5) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4, val5)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, TResult>FuncEx6<T0, T1, T2, T3, T4, T5, TResult> funcEx(FuncEx6<T0, T1, T2, T3, T4, T5, TResult> func) { return func; }
    
    public interface Action7<T0, T1, T2, T3, T4, T5, T6> extends Action1<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6);
        @Override default void invoke(system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6> arg0) { invoke(arg0.val0.val0.val0.val0.val0.val0, arg0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val1, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6]); return null; }
        static <T0, T1, T2, T3, T4, T5, T6>Action7<T0, T1, T2, T3, T4, T5, T6> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6 }) :
                    (val0, val1, val2, val3, val4, val5, val6) -> system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6>Action7<T0, T1, T2, T3, T4, T5, T6> action(Action7<T0, T1, T2, T3, T4, T5, T6> action) { return action; }
    public interface Func7<T0, T1, T2, T3, T4, T5, T6, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6]); }
        static <T0, T1, T2, T3, T4, T5, T6, TResult>Func7<T0, T1, T2, T3, T4, T5, T6, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6 }) :
                    (val0, val1, val2, val3, val4, val5, val6) -> system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, TResult>Func7<T0, T1, T2, T3, T4, T5, T6, TResult> func(Func7<T0, T1, T2, T3, T4, T5, T6, TResult> func) { return func; }
    public interface ActionEx7<T0, T1, T2, T3, T4, T5, T6> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6) throws Throwable;
        default Action7<T0, T1, T2, T3, T4, T5, T6> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func7<T0, T1, T2, T3, T4, T5, T6, Boolean> optional() {
            return (val0, val1, val2, val3, val4, val5, val6) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6>ActionEx7<T0, T1, T2, T3, T4, T5, T6> actionEx(ActionEx7<T0, T1, T2, T3, T4, T5, T6> action) { return action; }
    public interface FuncEx7<T0, T1, T2, T3, T4, T5, T6, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6) throws Throwable;
        default Func7<T0, T1, T2, T3, T4, T5, T6, TResult> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6) -> { try { return this.invoke(val0, val1, val2, val3, val4, val5, val6); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func7<T0, T1, T2, T3, T4, T5, T6, Optional<TResult>> optional() {
            return (val0, val1, val2, val3, val4, val5, val6) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4, val5, val6)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, TResult>FuncEx7<T0, T1, T2, T3, T4, T5, T6, TResult> funcEx(FuncEx7<T0, T1, T2, T3, T4, T5, T6, TResult> func) { return func; }
    
    public interface Action8<T0, T1, T2, T3, T4, T5, T6, T7> extends Action1<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7);
        @Override default void invoke(system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7> arg0) { invoke(arg0.val0.val0.val0.val0.val0.val0.val0, arg0.val0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val1, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6], (T7)args[7]); return null; }
        static <T0, T1, T2, T3, T4, T5, T6, T7>Action8<T0, T1, T2, T3, T4, T5, T6, T7> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6, val7) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6, val7 }) :
                    (val0, val1, val2, val3, val4, val5, val6, val7) -> system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6, val7 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7>Action8<T0, T1, T2, T3, T4, T5, T6, T7> action(Action8<T0, T1, T2, T3, T4, T5, T6, T7> action) { return action; }
    public interface Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6], (T7)args[7]); }
        static <T0, T1, T2, T3, T4, T5, T6, T7, TResult>Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6, val7) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6, val7 }) :
                    (val0, val1, val2, val3, val4, val5, val6, val7) -> system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6, val7 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, TResult>Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> func(Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> func) { return func; }
    public interface ActionEx8<T0, T1, T2, T3, T4, T5, T6, T7> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7) throws Throwable;
        default Action8<T0, T1, T2, T3, T4, T5, T6, T7> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6, val7) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6, val7); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func8<T0, T1, T2, T3, T4, T5, T6, T7, Boolean> optional() {
            return (val0, val1, val2, val3, val4, val5, val6, val7) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6, val7); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7>ActionEx8<T0, T1, T2, T3, T4, T5, T6, T7> actionEx(ActionEx8<T0, T1, T2, T3, T4, T5, T6, T7> action) { return action; }
    public interface FuncEx8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7) throws Throwable;
        default Func8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6, val7) -> { try { return this.invoke(val0, val1, val2, val3, val4, val5, val6, val7); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func8<T0, T1, T2, T3, T4, T5, T6, T7, Optional<TResult>> optional() {
            return (val0, val1, val2, val3, val4, val5, val6, val7) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4, val5, val6, val7)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, TResult>FuncEx8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> funcEx(FuncEx8<T0, T1, T2, T3, T4, T5, T6, T7, TResult> func) { return func; }
    
    public interface Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends Action1<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>,T8>> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8);
        @Override default void invoke(system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<system.Toast2<T0,T1>,T2>,T3>,T4>,T5>,T6>,T7>,T8> arg0) { invoke(arg0.val0.val0.val0.val0.val0.val0.val0.val0, arg0.val0.val0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val0.val1, arg0.val0.val0.val0.val1, arg0.val0.val0.val1, arg0.val0.val1, arg0.val1); }
        @Override default Object call(Object[] args) { invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6], (T7)args[7], (T8)args[8]); return null; }
        static <T0, T1, T2, T3, T4, T5, T6, T7, T8>Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6, val7, val8 }) :
                    (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6, val7, val8 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8>Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action(Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action) { return action; }
    public interface Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> extends ICallable {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8);
        @Override default Object call(Object[] args) { return invoke((T0)args[0], (T1)args[1], (T2)args[2], (T3)args[3], (T4)args[4], (T5)args[5], (T6)args[6], (T7)args[7], (T8)args[8]); }
        static <T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult>Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> of(Method method) {
            return Modifier.isStatic(method.getModifiers()) ?
                    (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> system.invoke(method, null, new Object[] { val0, val1, val2, val3, val4, val5, val6, val7, val8 }) :
                    (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> system.invoke(method, val0, new Object[] { val1, val2, val3, val4, val5, val6, val7, val8 });
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult>Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> func(Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> func) { return func; }
    public interface ActionEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8> {
        void invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8) throws Throwable;
        default Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, Boolean> optional() {
            return (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> { try { this.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8); return true; } catch (Throwable e) { return false; } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8>ActionEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8> actionEx(ActionEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action) { return action; }
    public interface FuncEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> {
        TResult invoke(T0 arg0, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8) throws Throwable;
        default Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> throwable() {
            return (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> { try { return this.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8); } catch (Throwable e) { throw new IllegalArgumentException(e); } };
        }
        default Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, Optional<TResult>> optional() {
            return (val0, val1, val2, val3, val4, val5, val6, val7, val8) -> { try { return Optional.ofNullable(this.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8)); } catch (Throwable e) { return Optional.empty(); } };
        }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult>FuncEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> funcEx(FuncEx9<T0, T1, T2, T3, T4, T5, T6, T7, T8, TResult> func) { return func; }
    
//</generator>
    //</editor-fold>

    public static abstract class ILockToast<T extends IToast> {
        private final T toast;
        private final Lock lock = system.Lock.create();

        protected ILockToast(T toast) {
            this.toast = toast;
        }

        public int size() {
            return lock.invoke(toast::size);
        }

        protected Object[] getValues() { return lock.invoke(toast::getValues);}
        protected Object get(int index) { return lock.invoke(() -> toast.get(index)); }
        protected void set(int index, Object value) { lock.invoke(() -> toast.set(index, value)); }
        protected Object edit(int index, system.Func1<Object, Object> func) { return lock.invoke(() -> toast.edit(index, func)); }

        public void invoke(system.Action1<T> action) { lock.invoke(() -> action.invoke(toast)); }
        public <I>I call(system.Func1<T, I> func) { return lock.invoke(() -> func.invoke(toast)); }

        @Override public int hashCode() {
            return Objects.hash(11, Objects.hash(getValues()));
        }
        @Override public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ILockToast<?> _obj)) return false;
            return _obj.size() == this.size() && Arrays.equals(_obj.getValues(), this.getValues());
        }
        public static <T extends ILockToast<?>> boolean equals(T obj1, T obj2) { return Objects.equals(obj1, obj2); }
    }
    public static abstract class IToast {
        public abstract int size();
        public abstract Object[] getValues();

        public abstract Object get(int index);
        public abstract void set(int index, Object value);
        public abstract Object edit(int index, system.Func1<Object, Object> func);

        @Override public int hashCode() {
            return Objects.hash(size(), 10, Objects.hash(getValues()));
        }
        @Override public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof IToast _obj)) return false;
            return _obj.size() == this.size() && Arrays.equals(_obj.getValues(), this.getValues());
        }
        public static <T extends IToast>boolean equals(T obj1, T obj2) {
            return Objects.equals(obj1, obj2);
        }

        @Override public String toString() { return "(" + Arrays.stream(getValues()).map(v -> v + "").collect(Collectors.joining(", ")) + ")"; }
    }

    //<editor-fold desc="Toasts">
    //<generator name="system-toast.js">

    public static <T0>Toast1<T0> toast(T0 val0){ return new Toast1<>(val0); }
    public static class Toast1<T0> extends IToast {
        public LockToast1<T0> lock() { return new LockToast1<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0 }; }
        public Toast1(T0 val0) { this.val0 = val0; }
        public T0 val0;
        @Override public int size() { return 1; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; } }
        @Override public Object edit(int index, system.Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0>Toast1<A0> map(system.Func1<T0, A0> map0) { return system.toast(map0.invoke(val0)); }
        public void invoke(system.Action1<T0> action) { action.invoke(val0); }
        public <T>T invokeGet(system.Func1<T0, T> func) { return func.invoke(val0); }
    }
    public static class LockToast1<T0> extends ILockToast<Toast1<T0>> {
        public LockToast1(Toast1<T0> base) { super(base); }
        public T0 get0() { return (T0)get(0); }
        public void set0(T0 value) { set(0, value); }
        public T0 edit0(system.Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); }
    }
    public static <T0, T1>Toast2<T0, T1> toast(T0 val0, T1 val1){ return new Toast2<>(val0, val1); }
    public static class Toast2<T0, T1> extends IToast {
        public LockToast2<T0, T1> lock() { return new LockToast2<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1 }; }
        public Toast2(T0 val0, T1 val1) { this.val0 = val0; this.val1 = val1; }
        public T0 val0; public T1 val1;
        @Override public int size() { return 2; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; } }
        @Override public Object edit(int index, system.Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1>Toast2<A0, A1> map(system.Func1<T0, A0> map0, system.Func1<T1, A1> map1) { return system.toast(map0.invoke(val0), map1.invoke(val1)); }
        public void invoke(system.Action2<T0, T1> action) { action.invoke(val0, val1); }
        public <T>T invokeGet(system.Func2<T0, T1, T> func) { return func.invoke(val0, val1); }
    }
    public static class LockToast2<T0, T1> extends ILockToast<Toast2<T0, T1>> {
        public LockToast2(Toast2<T0, T1> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); }
        public T0 edit0(system.Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(system.Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); }
    }
    public static <T0, T1, T2>Toast3<T0, T1, T2> toast(T0 val0, T1 val1, T2 val2){ return new Toast3<>(val0, val1, val2); }
    public static class Toast3<T0, T1, T2> extends IToast {
        public LockToast3<T0, T1, T2> lock() { return new LockToast3<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2 }; }
        public Toast3(T0 val0, T1 val1, T2 val2) { this.val0 = val0; this.val1 = val1; this.val2 = val2; }
        public T0 val0; public T1 val1; public T2 val2;
        @Override public int size() { return 3; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; } }
        @Override public Object edit(int index, system.Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2>Toast3<A0, A1, A2> map(system.Func1<T0, A0> map0, system.Func1<T1, A1> map1, system.Func1<T2, A2> map2) { return system.toast(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2)); }
        public void invoke(system.Action3<T0, T1, T2> action) { action.invoke(val0, val1, val2); }
        public <T>T invokeGet(system.Func3<T0, T1, T2, T> func) { return func.invoke(val0, val1, val2); }
    }
    public static class LockToast3<T0, T1, T2> extends ILockToast<Toast3<T0, T1, T2>> {
        public LockToast3(Toast3<T0, T1, T2> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); }
        public T0 edit0(system.Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(system.Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(system.Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); }
    }
    public static <T0, T1, T2, T3>Toast4<T0, T1, T2, T3> toast(T0 val0, T1 val1, T2 val2, T3 val3){ return new Toast4<>(val0, val1, val2, val3); }
    public static class Toast4<T0, T1, T2, T3> extends IToast {
        public LockToast4<T0, T1, T2, T3> lock() { return new LockToast4<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3 }; }
        public Toast4(T0 val0, T1 val1, T2 val2, T3 val3) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3;
        @Override public int size() { return 4; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; } }
        @Override public Object edit(int index, system.Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3>Toast4<A0, A1, A2, A3> map(system.Func1<T0, A0> map0, system.Func1<T1, A1> map1, system.Func1<T2, A2> map2, system.Func1<T3, A3> map3) { return system.toast(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3)); }
        public void invoke(system.Action4<T0, T1, T2, T3> action) { action.invoke(val0, val1, val2, val3); }
        public <T>T invokeGet(system.Func4<T0, T1, T2, T3, T> func) { return func.invoke(val0, val1, val2, val3); }
    }
    public static class LockToast4<T0, T1, T2, T3> extends ILockToast<Toast4<T0, T1, T2, T3>> {
        public LockToast4(Toast4<T0, T1, T2, T3> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); }
        public T0 edit0(system.Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(system.Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(system.Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(system.Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); }
    }
    public static <T0, T1, T2, T3, T4>Toast5<T0, T1, T2, T3, T4> toast(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4){ return new Toast5<>(val0, val1, val2, val3, val4); }
    public static class Toast5<T0, T1, T2, T3, T4> extends IToast {
        public LockToast5<T0, T1, T2, T3, T4> lock() { return new LockToast5<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4 }; }
        public Toast5(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4;
        @Override public int size() { return 5; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; } }
        @Override public Object edit(int index, system.Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3, A4>Toast5<A0, A1, A2, A3, A4> map(system.Func1<T0, A0> map0, system.Func1<T1, A1> map1, system.Func1<T2, A2> map2, system.Func1<T3, A3> map3, system.Func1<T4, A4> map4) { return system.toast(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4)); }
        public void invoke(system.Action5<T0, T1, T2, T3, T4> action) { action.invoke(val0, val1, val2, val3, val4); }
        public <T>T invokeGet(system.Func5<T0, T1, T2, T3, T4, T> func) { return func.invoke(val0, val1, val2, val3, val4); }
    }
    public static class LockToast5<T0, T1, T2, T3, T4> extends ILockToast<Toast5<T0, T1, T2, T3, T4>> {
        public LockToast5(Toast5<T0, T1, T2, T3, T4> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); } public T4 get4() { return (T4)get(4); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); } public void set4(T4 value) { set(4, value); }
        public T0 edit0(system.Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(system.Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(system.Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(system.Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); } public T4 edit4(system.Func1<T4, T4> func) { return (T4)edit(4, v -> func.invoke((T4)v)); }
    }
    public static <T0, T1, T2, T3, T4, T5>Toast6<T0, T1, T2, T3, T4, T5> toast(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5){ return new Toast6<>(val0, val1, val2, val3, val4, val5); }
    public static class Toast6<T0, T1, T2, T3, T4, T5> extends IToast {
        public LockToast6<T0, T1, T2, T3, T4, T5> lock() { return new LockToast6<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4, val5 }; }
        public Toast6(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; this.val5 = val5; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4; public T5 val5;
        @Override public int size() { return 6; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; case 5: return val5; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; case 5: val5 = (T5)value; break; } }
        @Override public Object edit(int index, system.Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3, A4, A5>Toast6<A0, A1, A2, A3, A4, A5> map(system.Func1<T0, A0> map0, system.Func1<T1, A1> map1, system.Func1<T2, A2> map2, system.Func1<T3, A3> map3, system.Func1<T4, A4> map4, system.Func1<T5, A5> map5) { return system.toast(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4), map5.invoke(val5)); }
        public void invoke(system.Action6<T0, T1, T2, T3, T4, T5> action) { action.invoke(val0, val1, val2, val3, val4, val5); }
        public <T>T invokeGet(system.Func6<T0, T1, T2, T3, T4, T5, T> func) { return func.invoke(val0, val1, val2, val3, val4, val5); }
    }
    public static class LockToast6<T0, T1, T2, T3, T4, T5> extends ILockToast<Toast6<T0, T1, T2, T3, T4, T5>> {
        public LockToast6(Toast6<T0, T1, T2, T3, T4, T5> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); } public T4 get4() { return (T4)get(4); } public T5 get5() { return (T5)get(5); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); } public void set4(T4 value) { set(4, value); } public void set5(T5 value) { set(5, value); }
        public T0 edit0(system.Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(system.Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(system.Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(system.Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); } public T4 edit4(system.Func1<T4, T4> func) { return (T4)edit(4, v -> func.invoke((T4)v)); } public T5 edit5(system.Func1<T5, T5> func) { return (T5)edit(5, v -> func.invoke((T5)v)); }
    }
    public static <T0, T1, T2, T3, T4, T5, T6>Toast7<T0, T1, T2, T3, T4, T5, T6> toast(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6){ return new Toast7<>(val0, val1, val2, val3, val4, val5, val6); }
    public static class Toast7<T0, T1, T2, T3, T4, T5, T6> extends IToast {
        public LockToast7<T0, T1, T2, T3, T4, T5, T6> lock() { return new LockToast7<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4, val5, val6 }; }
        public Toast7(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; this.val5 = val5; this.val6 = val6; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4; public T5 val5; public T6 val6;
        @Override public int size() { return 7; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; case 5: return val5; case 6: return val6; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; case 5: val5 = (T5)value; break; case 6: val6 = (T6)value; break; } }
        @Override public Object edit(int index, system.Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3, A4, A5, A6>Toast7<A0, A1, A2, A3, A4, A5, A6> map(system.Func1<T0, A0> map0, system.Func1<T1, A1> map1, system.Func1<T2, A2> map2, system.Func1<T3, A3> map3, system.Func1<T4, A4> map4, system.Func1<T5, A5> map5, system.Func1<T6, A6> map6) { return system.toast(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4), map5.invoke(val5), map6.invoke(val6)); }
        public void invoke(system.Action7<T0, T1, T2, T3, T4, T5, T6> action) { action.invoke(val0, val1, val2, val3, val4, val5, val6); }
        public <T>T invokeGet(system.Func7<T0, T1, T2, T3, T4, T5, T6, T> func) { return func.invoke(val0, val1, val2, val3, val4, val5, val6); }
    }
    public static class LockToast7<T0, T1, T2, T3, T4, T5, T6> extends ILockToast<Toast7<T0, T1, T2, T3, T4, T5, T6>> {
        public LockToast7(Toast7<T0, T1, T2, T3, T4, T5, T6> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); } public T4 get4() { return (T4)get(4); } public T5 get5() { return (T5)get(5); } public T6 get6() { return (T6)get(6); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); } public void set4(T4 value) { set(4, value); } public void set5(T5 value) { set(5, value); } public void set6(T6 value) { set(6, value); }
        public T0 edit0(system.Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(system.Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(system.Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(system.Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); } public T4 edit4(system.Func1<T4, T4> func) { return (T4)edit(4, v -> func.invoke((T4)v)); } public T5 edit5(system.Func1<T5, T5> func) { return (T5)edit(5, v -> func.invoke((T5)v)); } public T6 edit6(system.Func1<T6, T6> func) { return (T6)edit(6, v -> func.invoke((T6)v)); }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7>Toast8<T0, T1, T2, T3, T4, T5, T6, T7> toast(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, T7 val7){ return new Toast8<>(val0, val1, val2, val3, val4, val5, val6, val7); }
    public static class Toast8<T0, T1, T2, T3, T4, T5, T6, T7> extends IToast {
        public LockToast8<T0, T1, T2, T3, T4, T5, T6, T7> lock() { return new LockToast8<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4, val5, val6, val7 }; }
        public Toast8(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, T7 val7) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; this.val5 = val5; this.val6 = val6; this.val7 = val7; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4; public T5 val5; public T6 val6; public T7 val7;
        @Override public int size() { return 8; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; case 5: return val5; case 6: return val6; case 7: return val7; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; case 5: val5 = (T5)value; break; case 6: val6 = (T6)value; break; case 7: val7 = (T7)value; break; } }
        @Override public Object edit(int index, system.Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3, A4, A5, A6, A7>Toast8<A0, A1, A2, A3, A4, A5, A6, A7> map(system.Func1<T0, A0> map0, system.Func1<T1, A1> map1, system.Func1<T2, A2> map2, system.Func1<T3, A3> map3, system.Func1<T4, A4> map4, system.Func1<T5, A5> map5, system.Func1<T6, A6> map6, system.Func1<T7, A7> map7) { return system.toast(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4), map5.invoke(val5), map6.invoke(val6), map7.invoke(val7)); }
        public void invoke(system.Action8<T0, T1, T2, T3, T4, T5, T6, T7> action) { action.invoke(val0, val1, val2, val3, val4, val5, val6, val7); }
        public <T>T invokeGet(system.Func8<T0, T1, T2, T3, T4, T5, T6, T7, T> func) { return func.invoke(val0, val1, val2, val3, val4, val5, val6, val7); }
    }
    public static class LockToast8<T0, T1, T2, T3, T4, T5, T6, T7> extends ILockToast<Toast8<T0, T1, T2, T3, T4, T5, T6, T7>> {
        public LockToast8(Toast8<T0, T1, T2, T3, T4, T5, T6, T7> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); } public T4 get4() { return (T4)get(4); } public T5 get5() { return (T5)get(5); } public T6 get6() { return (T6)get(6); } public T7 get7() { return (T7)get(7); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); } public void set4(T4 value) { set(4, value); } public void set5(T5 value) { set(5, value); } public void set6(T6 value) { set(6, value); } public void set7(T7 value) { set(7, value); }
        public T0 edit0(system.Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(system.Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(system.Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(system.Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); } public T4 edit4(system.Func1<T4, T4> func) { return (T4)edit(4, v -> func.invoke((T4)v)); } public T5 edit5(system.Func1<T5, T5> func) { return (T5)edit(5, v -> func.invoke((T5)v)); } public T6 edit6(system.Func1<T6, T6> func) { return (T6)edit(6, v -> func.invoke((T6)v)); } public T7 edit7(system.Func1<T7, T7> func) { return (T7)edit(7, v -> func.invoke((T7)v)); }
    }
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8>Toast9<T0, T1, T2, T3, T4, T5, T6, T7, T8> toast(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, T7 val7, T8 val8){ return new Toast9<>(val0, val1, val2, val3, val4, val5, val6, val7, val8); }
    public static class Toast9<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends IToast {
        public LockToast9<T0, T1, T2, T3, T4, T5, T6, T7, T8> lock() { return new LockToast9<>(this); }
        @Override public Object[] getValues() { return new Object[] { val0, val1, val2, val3, val4, val5, val6, val7, val8 }; }
        public Toast9(T0 val0, T1 val1, T2 val2, T3 val3, T4 val4, T5 val5, T6 val6, T7 val7, T8 val8) { this.val0 = val0; this.val1 = val1; this.val2 = val2; this.val3 = val3; this.val4 = val4; this.val5 = val5; this.val6 = val6; this.val7 = val7; this.val8 = val8; }
        public T0 val0; public T1 val1; public T2 val2; public T3 val3; public T4 val4; public T5 val5; public T6 val6; public T7 val7; public T8 val8;
        @Override public int size() { return 9; }
        @Override public int hashCode() { return super.hashCode(); }
        @Override public boolean equals(Object obj) { return super.equals(obj); }
        @Override public Object get(int index) { switch (index) { case 0: return val0; case 1: return val1; case 2: return val2; case 3: return val3; case 4: return val4; case 5: return val5; case 6: return val6; case 7: return val7; case 8: return val8; } return null; }
        @Override public void set(int index, Object value) { switch (index) { case 0: val0 = (T0)value; break; case 1: val1 = (T1)value; break; case 2: val2 = (T2)value; break; case 3: val3 = (T3)value; break; case 4: val4 = (T4)value; break; case 5: val5 = (T5)value; break; case 6: val6 = (T6)value; break; case 7: val7 = (T7)value; break; case 8: val8 = (T8)value; break; } }
        @Override public Object edit(int index, system.Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }
        public <A0, A1, A2, A3, A4, A5, A6, A7, A8>Toast9<A0, A1, A2, A3, A4, A5, A6, A7, A8> map(system.Func1<T0, A0> map0, system.Func1<T1, A1> map1, system.Func1<T2, A2> map2, system.Func1<T3, A3> map3, system.Func1<T4, A4> map4, system.Func1<T5, A5> map5, system.Func1<T6, A6> map6, system.Func1<T7, A7> map7, system.Func1<T8, A8> map8) { return system.toast(map0.invoke(val0), map1.invoke(val1), map2.invoke(val2), map3.invoke(val3), map4.invoke(val4), map5.invoke(val5), map6.invoke(val6), map7.invoke(val7), map8.invoke(val8)); }
        public void invoke(system.Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8> action) { action.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8); }
        public <T>T invokeGet(system.Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, T> func) { return func.invoke(val0, val1, val2, val3, val4, val5, val6, val7, val8); }
    }
    public static class LockToast9<T0, T1, T2, T3, T4, T5, T6, T7, T8> extends ILockToast<Toast9<T0, T1, T2, T3, T4, T5, T6, T7, T8>> {
        public LockToast9(Toast9<T0, T1, T2, T3, T4, T5, T6, T7, T8> base) { super(base); }
        public T0 get0() { return (T0)get(0); } public T1 get1() { return (T1)get(1); } public T2 get2() { return (T2)get(2); } public T3 get3() { return (T3)get(3); } public T4 get4() { return (T4)get(4); } public T5 get5() { return (T5)get(5); } public T6 get6() { return (T6)get(6); } public T7 get7() { return (T7)get(7); } public T8 get8() { return (T8)get(8); }
        public void set0(T0 value) { set(0, value); } public void set1(T1 value) { set(1, value); } public void set2(T2 value) { set(2, value); } public void set3(T3 value) { set(3, value); } public void set4(T4 value) { set(4, value); } public void set5(T5 value) { set(5, value); } public void set6(T6 value) { set(6, value); } public void set7(T7 value) { set(7, value); } public void set8(T8 value) { set(8, value); }
        public T0 edit0(system.Func1<T0, T0> func) { return (T0)edit(0, v -> func.invoke((T0)v)); } public T1 edit1(system.Func1<T1, T1> func) { return (T1)edit(1, v -> func.invoke((T1)v)); } public T2 edit2(system.Func1<T2, T2> func) { return (T2)edit(2, v -> func.invoke((T2)v)); } public T3 edit3(system.Func1<T3, T3> func) { return (T3)edit(3, v -> func.invoke((T3)v)); } public T4 edit4(system.Func1<T4, T4> func) { return (T4)edit(4, v -> func.invoke((T4)v)); } public T5 edit5(system.Func1<T5, T5> func) { return (T5)edit(5, v -> func.invoke((T5)v)); } public T6 edit6(system.Func1<T6, T6> func) { return (T6)edit(6, v -> func.invoke((T6)v)); } public T7 edit7(system.Func1<T7, T7> func) { return (T7)edit(7, v -> func.invoke((T7)v)); } public T8 edit8(system.Func1<T8, T8> func) { return (T8)edit(8, v -> func.invoke((T8)v)); }
    }
    //</generator>
    //</editor-fold>

    public static abstract class IRange {
        public static IRange parse(String text) {
            String[] damageList = text.split(":");

            if (damageList.length == 1) {
                String prefix = damageList[0];
                int length = prefix.length();
                return prefix.endsWith("%")
                        ? new PercentRange(Double.parseDouble(prefix.substring(0, length - 1)) / 100)
                        : new OnceRange(Double.parseDouble(prefix));
            }

            return new DoubleRange(parse(damageList[0]), parse(damageList[1]));
        }

        public abstract double getMin(double max);
        public abstract double getMax(double max);
        public abstract double getValue(double max);
        public int getIntValue(double max) { return (int)Math.round(getValue(max)); }
        public abstract String displayText();

        public boolean inRange(double value, double max) {
            double _min = getMin(max);
            double _max = getMax(max);

            return value <= _max && value >= _min;
        }
    }
    public static class OnceRange extends IRange {
        public final double value;
        public OnceRange(double value) {
            this.value = value;
        }

        public double getMin(double max) {
            return value;
        }
        public double getMax(double max) {
            return value;
        }
        public double getValue(double max) {
            return value;
        }
        public String displayText() {
            return (int)value + "";
        }
    }
    public static class PercentRange extends IRange {
        public final double value;
        public PercentRange(double value) {
            this.value = value;
        }

        public double getMin(double max) {
            return value * max;
        }
        public double getMax(double max) {
            return value * max;
        }
        public double getValue(double max) {
            return value * max;
        }
        public String displayText() {
            return (int)(value*100) + "%";
        }
    }
    public static class DoubleRange extends IRange {
        public final IRange from;
        public final IRange to;
        public DoubleRange(IRange from, IRange to) {
            this.from = from;
            this.to = to;
        }

        public double getMin(double max) {
            return Math.min(from.getMin(max), to.getMin(max));
        }
        public double getMax(double max) {
            return Math.max(from.getMax(max), to.getMax(max));
        }

        public double getValue(double max) {
            double _v1 = from.getValue(max);
            double _v2 = to.getValue(max);
            return rand(Math.min(_v1, _v2), Math.max(_v1, _v2));
        }
        public String displayText() {
            return from.displayText() + " - " + to.displayText();
        }
    }

    public static class PostToast<T0> {
        public Func0<T0> func;
        private T0 value;
        private boolean isInit = false;
        private boolean isUpdate = false;
        public PostToast(Func0<T0> func) {
            this.func = func;
        }
        public T0 get() {
            return isInit ? value : (value = func.invoke());
        }
        public boolean isInited() {
            return isInit;
        }
        public boolean isUpdated() {
            return isUpdate;
        }
        public void set(T0 value) {
            this.value = value;
            this.isInit = true;
            this.isUpdate = true;
        }
    }

    public static class Property<T> {
        private final Func0<T> _get;
        private final Action1<T> _set;
        private T _obj;

        public Property() {
            this._get = () -> _obj;
            this._set = (value) -> _obj = value;
        }
        public Property(T _value) {
            this();
            _obj = _value;
        }
        public Property(Action1<T> _set)
        {
            this(null, _set);
        }
        public Property(Func0<T> _get)
        {
            this(_get, null);
        }
        public Property(Func0<T> _get, Action1<T> _set) {
            this._get = _get;
            this._set = _set;
        }

        public T get()
        {
            return _get.invoke();
        }
        public void set(T value)
        {
            _set.invoke(value);
        }
    }

    public static String FormatTime(int total_sec)
    {
        int sec = total_sec % 60;
        total_sec /= 60;
        int min = total_sec % 60;
        int hour = total_sec / 60;

        return String.format("%02d:%02d:%02d", hour, min, sec);
    }

    public static double GetProgress(double value, double min, double max, double max_value) {
        return Math.min(max_value, Math.max(0, Math.round((value - min) / (max - min) * max_value)));
    }
    public static int GetProgress(double value, double min, double max, int max_value) {
        return (int)Math.min(max_value, Math.max(0, Math.round((value - min) / (max - min) * max_value)));
    }

    public static String getDouble(double value) {
        return getDouble(value, 3);
    }
    public static String getDouble(double value, int parts) {
        return String.format("%1."+parts+"f", value).replace(",", ".");
    }

    public static system.Toast3<Integer, Integer, Integer> getPosToast(String str) {
        String[] _pos = str.split(" ");
        return system.toast(Integer.parseInt(_pos[0]), Integer.parseInt(_pos[1]), Integer.parseInt(_pos[2]));
    }
    public static Vector getVector(String str) {
        String[] _pos = str.split(" ");
        return new Vector(Double.parseDouble(_pos[0]), Double.parseDouble(_pos[1]), Double.parseDouble(_pos[2]));
    }
    public static Location getLocation(World world, String str) {
        String[] _pos = str.split(" ");
        Location location = new Location(world, Double.parseDouble(_pos[0]) + 0.5, Double.parseDouble(_pos[1]), Double.parseDouble(_pos[2]) + 0.5);
        if (_pos.length >= 5) {
            location.setYaw(Float.parseFloat(_pos[3]));
            location.setPitch(Float.parseFloat(_pos[4]));
        }
        return location;
    }
    public static String getString(Vector str) {
        return String.format("%1.3f %1.3f %1.3f", str.getX(), str.getY(), str.getZ()).replace(",", ".");
    }
    public static String getString(Location str) {
        return String.format("%1.3f %1.3f %1.3f %1.3f %1.3f", str.getX(), str.getY(), str.getZ(), (double)str.getYaw(), (double)str.getPitch()).replace(",", ".");
    }
    private static StringBuilder padInt(int value, char ch, int length) {
        StringBuilder builder = new StringBuilder();
        builder.append(value);
        length -= builder.length();
        for (int i = 0; i < length; i++)
            builder.insert(0, ch);
        return builder;
    }
    public static String getIntString(Vector str, String separator) {
        return padInt(str.getBlockX(), ' ', 5) + separator + padInt(str.getBlockY(), ' ', 3) + separator + padInt(str.getBlockZ(), ' ', 5);
    }

    public static void clear(JsonArray json) {
        removeAll(json.iterator());
    }
    public static void removeAll(Iterator<?> iterator) {
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }
    public static <T extends java.lang.Enum<T>>T parseEnum(Class<T> tClass, JsonElement json) {
        try { return json == null || json.isJsonNull() ? null : T.valueOf(tClass, json.getAsString()); } catch (Exception ignore) { return null; }
    }
    public static <T extends java.lang.Enum<T>>T parseEnum(Class<T> tClass, JsonElement json, T def) {
        try { return json == null || json.isJsonNull() ? def : T.valueOf(tClass, json.getAsString()); } catch (Exception ignore) { return def; }
    }

    public static <T extends java.lang.Enum<T>>Optional<T> tryParse(Class<T> tClass, String name) {
        try {
            for (T each : tClass.getEnumConstants())
                if (each.name().compareToIgnoreCase(name) == 0)
                    return Optional.of(each);
            return Optional.empty();
        } catch (Exception e) { return Optional.empty(); }
    }

    public static JsonObject EditStringToObject(JsonObject jsonObject, system.Func1<String, JsonElement> edit) {
        JsonObject result = new JsonObject();
        jsonObject.entrySet().forEach(kv -> result.add(edit.invoke(kv.getKey()).getAsString(), EditStringToObject(kv.getValue(), edit)));
        return result;
    }
    public static JsonArray EditStringToObject(JsonArray jsonArray, system.Func1<String, JsonElement> edit) {
        JsonArray result = new JsonArray();
        jsonArray.forEach(e -> result.add(EditStringToObject(e, edit)));
        return result;
    }
    public static JsonElement EditStringToObject(JsonElement jsonElement, system.Func1<String, JsonElement> edit) {
        if (jsonElement.isJsonNull()) return jsonElement;
        else if (jsonElement.isJsonPrimitive()) return jsonElement.getAsJsonPrimitive().isString() ? edit.invoke(jsonElement.getAsString()) : jsonElement;
        else if (jsonElement.isJsonObject()) return EditStringToObject(jsonElement.getAsJsonObject(), edit);
        else if (jsonElement.isJsonArray()) return EditStringToObject(jsonElement.getAsJsonArray(), edit);
        else throw new UnsupportedOperationException("Unsupported element: " + jsonElement);
    }

    public static Stream<? extends Player> GetPlayerList(Location location, double distance) {
        World world = location.getWorld();
        return Bukkit
                .getOnlinePlayers()
                .stream()
                .filter(other -> other.getWorld() == world && other.getLocation().distance(location) <= distance);
    }
    public static Player GetNearPlayer(List<Player> list, Location location) {
        return GetNearPlayer(list, location, null, null);
    }
    public static Player GetNearPlayer(List<Player> list, Location location, Double minDistance, system.Func1<Player, Boolean> filter) {
        Player result = null;
        double lastDistance = minDistance == null ? Double.MAX_VALUE : minDistance;
        for(Player p : list) {
            if (p.getWorld() != location.getWorld()) continue;
            double distance = location.distance(p.getLocation());
            if(distance < lastDistance) {
                if (filter != null && !filter.invoke(p)) continue;
                lastDistance = distance;
                result = p;
            }
        }
        return result;
    }

    public static Player GetNearPlayer(Map<Player, Location> map, Location location) {
        return GetNearPlayer(map, location, null, null);
    }
    public static Player GetNearPlayer(Map<Player, Location> map, Location location, Double minDistance, system.Func1<Player, Boolean> filter) {
        if (location == null) return null;
        Player result = null;
        double lastDistance = minDistance == null ? Double.MAX_VALUE : minDistance;
        for(Map.Entry<Player, Location> kv : map.entrySet()) {
            Location l = kv.getValue();
            if (l.getWorld() != location.getWorld()) continue;
            double distance = location.distance(l);
            if(distance < lastDistance) {
                Player p = kv.getKey();
                if (filter != null && !filter.invoke(p)) continue;
                lastDistance = distance;
                result = p;
            }
        }
        return result;
    }

    public static <T, TL extends Collection<T>>TL distinct(TL list) {
        LinkedHashSet<T> set = new LinkedHashSet<>(list);
        list.clear();
        list.addAll(set);
        return list;
    }
    public static <T> Predicate<T> distinctBy(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }


    public static String toFormat(JsonElement json) {
        try {
            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setIndent("    ");
            jsonWriter.setLenient(true);
            com.google.gson.internal.Streams.write(json, jsonWriter);
            return stringWriter.toString();
        } catch (IOException var3) {
            throw new AssertionError(var3);
        }
    }

    public static JsonObject DeepCopy(JsonObject jsonObject) {
        JsonObject result = new JsonObject();
        jsonObject.entrySet().forEach(kv -> result.add(kv.getKey(), DeepCopy(kv.getValue())));
        return result;
    }
    public static JsonArray DeepCopy(JsonArray jsonArray) {
        JsonArray result = new JsonArray();
        jsonArray.forEach(e -> result.add(DeepCopy(e)));
        return result;
    }
    public static <T extends JsonElement>T DeepCopy(T jsonElement) {
        if (jsonElement == null) return null;
        else if (jsonElement.isJsonPrimitive() || jsonElement.isJsonNull()) return jsonElement;
        else if (jsonElement.isJsonObject()) return (T)DeepCopy(jsonElement.getAsJsonObject());
        else if (jsonElement.isJsonArray()) return (T)DeepCopy(jsonElement.getAsJsonArray());
        else throw new UnsupportedOperationException("Unsupported element: " + jsonElement);
    }

    public static Calendar getMoscowNow() { return Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")); }
    public static Calendar getMoscowTime(long totalMs) {
        Calendar calendar = getMoscowNow();
        calendar.setTimeInMillis(totalMs);
        return calendar;
    }
    public static Calendar getZeroTime() { return getMoscowTime(0); }

    public static String saveItem(ItemStack item) {
        return item == null ? null : new String(Base64.getEncoder().encode(item.serializeAsBytes()));
    }
    public static ItemStack loadItem(String str) {
        return str == null ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(str.getBytes()));
    }

    public static int formattedTime(String time) {
        int total = 0;
        int value = 0;
        for (char ch : time.toCharArray()) {
            if ('0' <= ch && ch <= '9') {
                value = value * 10 + ch - '0';
                continue;
            }
            switch (ch) {
                case 's': total += value; value = 0; continue;
                case 'm': total += value * 60; value = 0; continue;
                case 'h': total += value * 60 * 60; value = 0; continue;
                case 'd': total += value * 60 * 60 * 24; value = 0; continue;
                default: value = 0; continue;
            }
        }
        total += value;
        return total;
    }

    public static Integer compareCalendar(Calendar calendar1, Calendar calendar2) {
        system.Func1<Integer, Integer> comparer = v -> Integer.compare(calendar1.get(v), calendar2.get(v));

        int[] checks = new int[]{
                Calendar.YEAR,
                Calendar.MONTH,
                Calendar.DAY_OF_MONTH,
                Calendar.HOUR_OF_DAY,
                Calendar.MINUTE,
                Calendar.SECOND,
                Calendar.MILLISECOND
        };
        for (int check : checks) {
            int value = comparer.invoke(check);
            if (value != 0) return value;
        }
        return 0;
    }
    private static void applyDate(Calendar calendar, String text) {
        String[] args = text.split("\\.");
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(args[0]));
        calendar.set(Calendar.MONTH, Integer.parseInt(args[1]) - 1);
        calendar.set(Calendar.YEAR, Integer.parseInt(args[2]));
    }
    private static void applyTime(Calendar calendar, String text) {
        String[] args = text.split(":");
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(args[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(args[1]));
        calendar.set(Calendar.SECOND, Integer.parseInt(args[2]));
    }
    public static Calendar parseCalendar(String text) {
        String[] args = text.split(" ");
        Calendar calendar = getMoscowNow();
        if (args.length == 1) {
            applyTime(calendar, "00:00:00");
            applyDate(calendar, args[0]);
        } else {
            applyTime(calendar, args[0]);
            applyDate(calendar, args[1]);
        }
        return calendar;
    }
    public static String formatCalendar(Calendar calendar, boolean withTime) {
        if (calendar == null) return "";
        StringBuilder builder = new StringBuilder();
        if (withTime) {
            builder
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)), 2, '0'))
                    .append(':')
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MINUTE)), 2, '0'))
                    .append(':')
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.SECOND)),  2, '0'))
                    .append(' ');
        }
        return builder
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), 2, '0'))
                .append('.')
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MONTH) + 1), 2, '0'))
                .append('.')
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.YEAR)), 4, '0')).toString();
    }
    public static String formatMiniCalendar(Calendar calendar, boolean withTime) {
        if (calendar == null) return "";
        StringBuilder builder = new StringBuilder();
        if (withTime) {
            builder
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.HOUR_OF_DAY)), 2, '0'))
                    .append(':')
                    .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MINUTE)), 2, '0'))
                    .append(' ');
        }
        return builder
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)), 2, '0'))
                .append('.')
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.MONTH) + 1), 2, '0'))
                .append('.')
                .append(StringUtils.leftPad(String.valueOf(calendar.get(Calendar.YEAR)), 4, '0').substring(2)).toString();
    }

    public enum FormatTime {
        DAY_TIME("%d %02d:%02d:%02d", total -> new Object[] { total / (60*60*24), (total % (60*60*24)) / (60*60), (total % (60*60)) / 60, total % 60 }),
        HOUR_TIME("%02d:%02d:%02d", total -> new Object[] { total / (60*60), (total % (60*60)) / 60, total % 60 }),
        MINUTE_TIME("%02d:%02d", total -> new Object[] { total / 60, total % 60 }),
        SECOND_TIME("%02d", total -> new Object[] { total });

        public final String format;
        public final system.Func1<Integer, Object[]> convert;
        FormatTime(String format, system.Func1<Integer, Object[]> convert) {
            this.format = format;
            this.convert = convert;
        }
        public String format(int totalSec) {
            return String.format(format, convert.invoke(totalSec));
        }
    }
    public static String formatTotalTime(int totalSec, FormatTime format) {
        return format.format(totalSec);
        /*totalSec = Math.max(totalSec, 0);
        int sec = totalSec % 60;
        totalSec /= 60;
        int min = totalSec % 60;
        if (format == FormatTime.MINUTE_TIME) {
            return org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils.leftPad(String.valueOf(hour), 2, '0') +  ":" +
                    org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils.leftPad(String.valueOf(min), 2, '0') +  ":" +
                    org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils.leftPad(String.valueOf(sec), 2, '0');
        }
        totalSec /= 60;
        int hour;
        if (withDays) {
            hour = totalSec;
            return org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils.leftPad(String.valueOf(hour), 2, '0') +  ":" +
                    org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils.leftPad(String.valueOf(min), 2, '0') +  ":" +
                    org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils.leftPad(String.valueOf(sec), 2, '0');
        } else {
            hour = totalSec % 24;
            totalSec /= 24;
            int day = totalSec;

            return day + "." +
                    org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils.leftPad(String.valueOf(hour), 2, '0') +  ":" +
                    org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils.leftPad(String.valueOf(min), 2, '0') +  ":" +
                    org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils.leftPad(String.valueOf(sec), 2, '0');
        }*/
    }
    public static String formatTotalTime(long ms, FormatTime format) {
        return formatTotalTime((int)(ms / 1000), format);
    }

    public static <T> Stream<T> skipLast(Stream<T> s, int count) {
        if(count <= 0) {
            if(count == 0) return s;
            throw new IllegalArgumentException(count + " < 0");
        }
        ArrayDeque<T> pending = new ArrayDeque<>(count+1);
        Spliterator<T> src=s.spliterator();
        return StreamSupport.stream(new Spliterator<T>() {
            public boolean tryAdvance(Consumer<? super T> action) {
                while(pending.size() <= count && src.tryAdvance(pending::add));
                if (pending.size() <= count) return false;
                action.accept(pending.remove());
                return true;
            }

            public Spliterator<T> trySplit() { return null; }
            public long estimateSize() { return src.estimateSize()-count; }
            public int characteristics() { return src.characteristics(); }
        }, false);
    }

    private static final List<system.Toast2<Integer, String>> romans = Arrays.asList(
            system.toast(1000, "M"),
            system.toast(900, "CM"),
            system.toast(500, "D"),
            system.toast(400, "CD"),
            system.toast(100, "C"),
            system.toast(90, "XC"),
            system.toast(50, "L"),
            system.toast(40, "XL"),
            system.toast(10, "X"),
            system.toast(9, "IX"),
            system.toast(5, "V"),
            system.toast(4, "IV"),
            system.toast(1, "I")
    );

    public static String formatRoman(int number) {
        if (number > 3999 || number < 1) return String.valueOf(number);
        for (system.Toast2<Integer, String> roman : romans) {
            if (number < roman.val0) continue;
            number -= roman.val0;
            return roman.val1 + (number == 0 ? "" : formatRoman(number));
        }
        return String.valueOf(number);
    }

    public static system.Func0<Boolean> negative(system.Func0<Boolean> func) {
        return () -> !func.invoke();
    }
    public static <T0>system.Func1<T0, Boolean> negative(system.Func1<T0, Boolean> func) {
        return (v0) -> !func.invoke(v0);
    }
    public static <T0,T1>system.Func2<T0, T1, Boolean> negative(system.Func2<T0, T1, Boolean> func) {
        return (v0,v1) -> !func.invoke(v0,v1);
    }

    public static class DeleteHandle implements IDelete {
        private boolean _deleted = false;
        public boolean isDeleted() {
            return _deleted;
        }
        public void delete() {
            _deleted = true;
        }
    }
    public static class ChildDeleteHandle extends DeleteHandle {
        private final IDelete base;
        public ChildDeleteHandle(IDelete base) {
            this.base = base;
        }
        public boolean isDeleted() {
            return super.isDeleted() || base.isDeleted();
        }
    }
    public interface IDelete {
        IDelete NONE = new IDelete() {
            @Override public boolean isDeleted() { return false; }
            @Override public void delete() { }
        };
        boolean isDeleted();
        void delete();
    }

    public static <T> Stream<T> reverse(Stream<T> stream) {
        LinkedList<T> stack = new LinkedList<>();
        stream.forEach(stack::push);
        return stack.stream();
    }

    public static abstract class Enum {
        protected abstract long getBit();

        public static <T extends Enum>boolean has(long bit, T type) {
            return (bit & type.getBit()) == type.getBit();
        }
        public static <T extends Enum>List<T> from(long bit, Collection<T> values) {
            List<T> list = new ArrayList<>();
            for (T type : values) {
                if (has(bit, type))
                    list.add(type);
            }
            return list;
        }
        public static <T extends Enum>long from(Collection<T> bits) {
            long b = 0;
            for (Enum type : bits) b |= type.getBit();
            return b;
        }

        public static <T extends Enum>long add(List<T> bits, T bit) {
            return add(from(bits), bit);
        }
        public static <T extends Enum>long del(List<T> bits, T bit) {
            return del(from(bits), bit);
        }

        public static <T extends Enum>long add(long bits, T bit) {
            bits |= bit.getBit();
            return bits;
        }
        public static <T extends Enum>long del(long bits, T bit) {
            bits &= ~bit.getBit();
            return bits;
        }
    }
}