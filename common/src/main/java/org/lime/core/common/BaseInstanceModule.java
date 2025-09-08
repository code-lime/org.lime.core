package org.lime.core.common;

import com.google.common.base.CaseFormat;
import com.google.gson.*;
import com.google.inject.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.unsafe.GlobalConfigure;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.lime.core.common.utils.Unsafe;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.api.*;
import org.lime.core.common.api.commands.LiteCommandConsumer;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.impl.ConfigAccessImpl;
import org.lime.core.common.services.UpdateConfigService;
import org.lime.core.common.utils.*;
import org.lime.core.common.utils.adapters.GsonTypeAdapters;
import org.lime.core.common.utils.json.builder.Json;
import org.lime.core.common.utils.adapters.RuntimeTypeAdapterFactory;
import org.lime.core.common.utils.system.Lazy;
import org.lime.core.common.utils.system.execute.Func1;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public abstract class BaseInstanceModule<Instance extends BaseInstance<Instance>>
        extends AbstractModule {
    protected final Disposable.Composite compositeDisposable;

    protected final Deque<Class<? extends Service>> services = new ConcurrentLinkedDeque<>();
    protected final Set<Class<?>> bindCache = ConcurrentHashMap.newKeySet();
    protected final ConcurrentHashMap<String, List<ConfigAccess<?>>> configs = new ConcurrentHashMap<>();

    protected final Class<Instance> instanceClass;
    protected final Instance instance;

    private BaseInstanceModule(
            Class<Instance> instanceClass,
            Disposable.Composite compositeDisposable,
            Instance instance) {
        this.instanceClass = instanceClass;
        this.compositeDisposable = compositeDisposable;
        this.instance = instance;
    }
    public BaseInstanceModule(Instance instance) {
        //noinspection unchecked
        this((Class<Instance>) instance.getClass(), instance.compositeDisposable, instance);
    }

    protected abstract UnsafeMappingsUtility mappings();

    protected abstract LiteCommandConsumer.Factory<?,?,?> liteCommandFactory();
    protected abstract NativeCommandConsumer.Factory<?,?> nativeCommandFactory();

    private static void addPart(
            JsonObject root,
            String[] part,
            JsonElement value) {
        int length = part.length;
        if (length == 0)
            throw new IllegalArgumentException("Part array is empty");

        JsonObject current = root;
        for (int i = 0; i < length - 1; i++) {
            String name = part[i];
            JsonObject next;
            if (current.has(name)) next = current.getAsJsonObject(name);
            else current.add(name, next = new JsonObject());
            current = next;
        }
        current.add(part[length - 1], value);
    }
    private static JsonObject createPart(
            String[] part,
            JsonElement value) {
        JsonObject root = new JsonObject();
        addPart(root, part, value);
        return root;
    }

    @SuppressWarnings("unchecked")
    protected <T>T readConfig(
            Path file,
            String[] part,
            Gson gson,
            Type configClass) {
        Class<T> rawConfigClass = (Class<T>)TypeUtils.getRawType(configClass, configClass);
        if (rawConfigClass == null)
            throw new IllegalArgumentException("Not found raw class for config file " + file + " of " + configClass);
        T defaultValue = Arrays.stream(rawConfigClass.getDeclaredFields())
                .filter(v -> v.getName().equals("INSTANCE")
                        && v.getGenericType().equals(configClass)
                        && Modifier.isFinal(v.getModifiers())
                        && Modifier.isStatic(v.getModifiers()))
                .findFirst()
                .map(v -> {
                    try {
                        return rawConfigClass.cast(v.get(null));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseGet(() -> {
                    try {
                        return rawConfigClass.getDeclaredConstructor().newInstance();
                    } catch (NoSuchMethodException e) {
                        throw new IllegalArgumentException("Not found empty constructor for initialize config file " + file + " of " + configClass);
                    } catch (InstantiationException
                             | IllegalAccessException
                             | InvocationTargetException e) {
                        throw new IllegalArgumentException("Error create new instance for config file " + file + " of " + configClass, e);
                    }
                });
        JsonElement defaultJson = gson.toJsonTree(defaultValue);
        try {
            JsonElement valueJson;
            int partLength = part.length;
            if (Files.exists(file)) {
                JsonElement rootJson = JsonParser.parseString(Files.readString(file));
                valueJson = rootJson;
                for (String name : part) {
                    JsonObject nextJsonObject = valueJson.getAsJsonObject();
                    if (nextJsonObject.has(name)) {
                        valueJson = nextJsonObject.get(name);
                    } else {
                        valueJson = null;
                        break;
                    }
                }
                Optional<JsonElement> merged = valueJson == null ? Optional.of(defaultJson) : JsonUtils.merge(valueJson, defaultJson);

                if (merged.isPresent()) {
                    valueJson = merged.get();
                    if (partLength == 0) {
                        rootJson = valueJson;
                    } else {
                        addPart(rootJson.getAsJsonObject(), part, valueJson);
                    }
                    Files.writeString(file, Json.format(rootJson));
                }
                return gson.fromJson(valueJson, configClass);
            }

            Path parent = file.getParent();
            if (!Files.exists(parent))
                Files.createDirectories(parent);
            valueJson = gson.toJsonTree(defaultValue, configClass);
            Files.writeString(file, Json.format(partLength == 0
                    ? valueJson
                    : createPart(part, valueJson)));
            return gson.fromJson(valueJson, configClass);
        } catch (IOException e) {
            throw new RuntimeException("Error load config " + file + " of " + configClass, e);
        }
    }
    protected <T>void writeConfig(
            Path file,
            String[] part,
            Gson gson,
            Type configClass,
            T value) {
        try {
            Path parent = file.getParent();
            if (!Files.exists(parent))
                Files.createDirectories(parent);

            String rawValue;
            int partLength = part.length;
            if (partLength == 0) {
                rawValue = gson.toJson(value, configClass);
            } else {
                JsonObject rootJson = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                addPart(rootJson, part, gson.toJsonTree(value, configClass));
                rawValue = Json.format(rootJson);
            }
            Files.writeString(file, rawValue);
        } catch (IOException e) {
            throw new RuntimeException("Error write config " + file + " of " + configClass, e);
        }
    }
    private String getConfigPath(
            RequireConfig config,
            Type configType) {
        String[] path = config.path();
        if (path.length != 0)
            return String.join(File.separator, path) + ".json";

        String typeName = configType.getTypeName();
        int dotIndex = typeName.lastIndexOf('.');
        if (dotIndex >= 0)
            typeName = typeName.substring(dotIndex + 1);
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, typeName) + ".json";
    }
    private <T>void bindConfigAccess(
            RequireConfig config,
            Type configType,
            Type configAccessType) {
        final String filePath = getConfigPath(config, configType);
        final String[] part = config.part();
        final String key = Optional.of(config.key())
                .filter(v -> !v.isBlank())
                .orElseGet(() -> String.join(".", config.path()));

        //noinspection unchecked
        TypeLiteral<ConfigAccess<T>> typeLiteral = (TypeLiteral<ConfigAccess<T>>)TypeLiteral.get(configAccessType);

        bind(typeLiteral)
                .annotatedWith(config)
                .toProvider(new Provider<>() {
                    @Inject
                    Gson gson;

                    @Override
                    public ConfigAccess<T> get() {
                        Path configFile = instance.dataFolder()
                                .toPath()
                                .resolve(filePath);
                        ConfigAccess<T> configAccess = new ConfigAccessImpl<>(config.updatable()) {
                            @Override
                            protected T read() {
                                return readConfig(configFile, part, gson, configType);
                            }
                            @Override
                            protected void write(T value) {
                                writeConfig(configFile, part, gson, configType, value);
                            }
                        };
                        configs.computeIfAbsent(key, v -> new ArrayList<>())
                                .add(configAccess);
                        if (part.length > 0)
                            configs.computeIfAbsent(key + "+" + String.join(".", config.part()), v -> new ArrayList<>())
                                    .add(configAccess);
                        return configAccess;
                    }
                })
                .asEagerSingleton();
    }
    protected <T>void bindConfig(
            RequireConfig config,
            Type configType) {
        if (configType instanceof ParameterizedType configParameterizedType
                && configParameterizedType.getRawType().equals(ConfigAccess.class)
                && configParameterizedType.getActualTypeArguments().length == 1) {
            bindConfigAccess(config, configParameterizedType.getActualTypeArguments()[0], configType);
            return;
        }

        final String filePath = getConfigPath(config, configType);
        final String[] part = config.part();

        //noinspection unchecked
        TypeLiteral<T> typeLiteral = (TypeLiteral<T>)TypeLiteral.get(configType);

        bind(typeLiteral)
                .annotatedWith(config)
                .toProvider(new Provider<>() {
                    @Inject
                    Gson gson;

                    @Override
                    public T get() {
                        Path configFile = instance.dataFolder()
                                .toPath()
                                .resolve(filePath);
                        return readConfig(configFile, part, gson, configType);
                    }
                })
                .asEagerSingleton();
    }
    protected void bindCustom(
            Class<? extends Service> custom) {
        bindCustom(custom, true);
    }
    protected <T>void bindCustom(
            Class<T> custom,
            boolean singelton) {
        if (!bindCache.add(custom) || services.contains(custom))
            return;
        for (Require children : custom.getDeclaredAnnotationsByType(Require.class))
            bindCustom(children.value(), false);

        if (Service.class.isAssignableFrom(custom)) {
            //noinspection unchecked
            services.add((Class<? extends Service>) custom);
            singelton = true;
        }
        if (singelton) {
            bind(custom).asEagerSingleton();
        }

        AnnotationUtils.recursiveAnnotations(RequireConfig.class, custom)
                .distinct()
                .forEach(data -> bindConfig(data.annotation(), data.target()));
    }
    protected <F, T>void bindMapped(Class<T> serviceClass, Class<F> targetClass, Func1<F, T> provider) {
        bind(serviceClass)
                .toProvider(new Provider<>() {
                    @Inject Injector injector;
                    private final Lazy<Provider<F>> targetProvider = Lazy.of(() -> injector.getProvider(targetClass));

                    @Override
                    public T get() {
                        return provider.invoke(targetProvider.value().get());
                    }
                });
    }
    protected <F extends T, T>void bindCast(Class<T> serviceClass, Class<F> targetClass) {
        bind(serviceClass)
                .toProvider(new Provider<>() {
                    @Inject Injector injector;
                    private final Lazy<Provider<F>> targetProvider = Lazy.of(() -> injector.getProvider(targetClass));

                    @Override
                    public T get() {
                        return injector.getInstance(targetClass);
                    }
                });
    }


    protected MiniMessage miniMessage() {
        return MiniMessage.miniMessage();
    }

    protected GsonBuilder configureGson(GsonBuilder builder) {
        return builder
                .registerTypeAdapterFactory(GsonTypeAdapters.range())
                .registerTypeAdapterFactory(GsonTypeAdapters.duration())
                .registerTypeAdapterFactory(GsonTypeAdapters.miniMessage(miniMessage()))
                .registerTypeAdapterFactory(GsonTypeAdapters.key())
                .registerTypeAdapterFactory(RuntimeTypeAdapterFactory.AUTO);
    }

    protected void executeCore() {
        Unsafe.MAPPINGS = mappings();
        GlobalConfigure.configure();
    }

    @Override
    protected void configure() {
        bind(UnsafeMappingsUtility.class).toInstance(Unsafe.MAPPINGS);

        bind(Logger.class).toInstance(instance.logger());
        bind(Gson.class)
                .toProvider(() -> {
                    GsonBuilder builder = new GsonBuilder()
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .serializeNulls()
                            .disableHtmlEscaping();
                            //.setFormattingStyle(FormattingStyle.PRETTY.withIndent("    "));
                    return configureGson(builder).create();
                });

        bind(BaseInstance.class).toInstance(instance);
        bind(instanceClass).toInstance(instance);
        bind(LiteCommandConsumer.Factory.class).toInstance(liteCommandFactory());
        bind(NativeCommandConsumer.Factory.class).toInstance(nativeCommandFactory());

        bind(new TypeLiteral<BaseInstance<?>>() {}).toInstance(instance);
        bind(new TypeLiteral<LiteCommandConsumer.Factory<?,?,?>>() {}).toInstance(liteCommandFactory());
        bind(new TypeLiteral<NativeCommandConsumer.Factory<?,?>>() {}).toInstance(nativeCommandFactory());

        bindCustom(UpdateConfigService.class);

        instance
                .findAnnotatedClasses(BindService.class)
                .filter(v -> Optional.ofNullable(v.getDeclaredAnnotation(BindService.class))
                        .map(BindService::enable)
                        .orElse(false))
                .forEach(v -> bindCustom(v, true));
    }

    public Collection<String> configKeys() {
        return configs.keySet();
    }
    public int updateConfigs(Iterable<String> keys) {
        int count = 0;
        for (String key : keys) {
            var configList = configs.get(key);
            if (configList == null)
                continue;
            for (ConfigAccess<?> configAccess : configList) {
                if (configAccess.update())
                    count++;
            }
        }
        return count;
    }
}
