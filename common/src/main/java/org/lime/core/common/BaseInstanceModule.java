package org.lime.core.common;

import com.google.common.base.CaseFormat;
import com.google.gson.*;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.unsafe.GlobalConfigure;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.Unsafe;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.api.*;
import org.lime.core.common.api.commands.LiteCommandConsumer;
import org.lime.core.common.api.commands.NativeCommandConsumer;
import org.lime.core.common.api.impl.ConfigAccessImpl;
import org.lime.core.common.services.UpdateConfigService;
import org.lime.core.common.utils.*;
import org.lime.core.common.utils.json.builder.Json;
import org.lime.core.common.utils.typeadapers.RuntimeTypeAdapterFactory;

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

    @SuppressWarnings("unchecked")
    protected <T>T readConfig(
            Path file,
            @Nullable String part,
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
            if (Files.exists(file)) {
                JsonElement rootJson = JsonParser.parseString(Files.readString(file));
                if (part != null) {
                    JsonObject rootJsonObject = rootJson.getAsJsonObject();
                    if (rootJsonObject.has(part)) {
                        valueJson = rootJsonObject.get(part);
                    } else {
                        valueJson = null;
                    }
                } else {
                    valueJson = rootJson;
                }
                Optional<JsonElement> merged = valueJson == null ? Optional.of(defaultJson) : JsonUtils.merge(valueJson, defaultJson);
                if (merged.isPresent()) {
                    valueJson = merged.get();
                    if (part != null) {
                        rootJson.getAsJsonObject().add(part, valueJson);
                    } else {
                        rootJson = valueJson;
                    }
                    Files.writeString(file, JsonUtils.toJson(gson, rootJson));
                }
                return gson.fromJson(valueJson, configClass);
            }

            Path parent = file.getParent();
            if (!Files.exists(parent))
                Files.createDirectories(parent);
            valueJson = gson.toJsonTree(defaultValue, configClass);
            Files.writeString(file, JsonUtils.toJson(gson, part == null
                    ? valueJson
                    : Json.object().add(part, valueJson).build()));
            return gson.fromJson(valueJson, configClass);
        } catch (IOException e) {
            throw new RuntimeException("Error load config " + file + " of " + configClass, e);
        }
    }
    protected <T>void writeConfig(
            Path file,
            @Nullable String part,
            Gson gson,
            Type configClass,
            T value) {
        try {
            Path parent = file.getParent();
            if (!Files.exists(parent))
                Files.createDirectories(parent);

            String rawValue;
            if (part == null) {
                rawValue = gson.toJson(value, configClass);
            } else {
                JsonObject rootJson = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                rootJson.add(part, gson.toJsonTree(value, configClass));
                rawValue = JsonUtils.toJson(gson, rootJson);
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
        final @Nullable String part = Optional.of(config.part()).filter(v -> !v.isBlank()).orElse(null);
        final String key = Optional.of(config.key())
                .filter(v -> !v.isBlank())
                .orElseGet(() -> String.join(".", config.part()));

        //noinspection unchecked
        TypeLiteral<ConfigAccess<T>> typeLiteral = (TypeLiteral<ConfigAccess<T>>)TypeLiteral.get(configAccessType);

        bind(typeLiteral)
                .annotatedWith(config)
                .toProvider(new Provider<>() {
                    @Inject
                    Instance instance;
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
                        if (part != null)
                            configs.computeIfAbsent(key + "#" + part, v -> new ArrayList<>())
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
        final @Nullable String part = Optional.of(config.part()).filter(v -> !v.isBlank()).orElse(null);

        //noinspection unchecked
        TypeLiteral<T> typeLiteral = (TypeLiteral<T>)TypeLiteral.get(configType);

        bind(typeLiteral)
                .annotatedWith(config)
                .toProvider(new Provider<>() {
                    @Inject
                    Instance instance;
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
    protected <T>void bindCustom(
            Class<T> custom) {
        if (services.contains(custom))
            return;
        for (Require children : custom.getDeclaredAnnotationsByType(Require.class))
            bindCustom(children.value());

        if (Service.class.isAssignableFrom(custom)) {
            //noinspection unchecked
            services.add((Class<? extends Service>) custom);
            bind(custom).asEagerSingleton();
        }

        AnnotationUtils.recursiveAnnotations(RequireConfig.class, custom)
                .distinct()
                .forEach(data -> bindConfig(data.annotation(), data.target()));
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

        bindCustom(UpdateConfigService.class);

        instance
                .findAnnotatedClasses(BindService.class)
                .filter(v -> Optional.ofNullable(v.getDeclaredAnnotation(BindService.class))
                        .map(BindService::enable)
                        .orElse(false))
                .forEach(this::bindCustom);
    }

    public Collection<String> configKeys() {
        return configs.keySet();
    }
    public void updateConfigs(Iterable<String> keys) {
        keys.forEach(key -> {
            var configList = configs.get(key);
            if (configList == null)
                return;
            configList.forEach(ConfigAccess::update);
        });
    }
}
