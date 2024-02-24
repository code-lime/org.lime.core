package net.minecraft.paper.java;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.LibraryLoader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class CacheLibraryLoader extends LibraryLoader {
    private final Logger logger;
    private static final ClassLoader parentClassLoader = LibraryLoader.class.getClassLoader();
    private static final ConcurrentHashMap<URL, URLClassLoader> classLoaders = new ConcurrentHashMap<>();

    public CacheLibraryLoader(Logger logger) {
        super(logger);
        this.logger = logger;
    }

    private static Optional<RawPluginMeta> getRawMeta(Object data) {
        return data instanceof RawPluginMeta rpm
                ? Optional.of(rpm)
                : Optional.empty();
    }

    @Override public @Nullable ClassLoader createLoader(@Nonnull PluginDescriptionFile desc) {
        final String LOG_PREFIX = Objects.requireNonNullElseGet(desc.getPrefix(), desc::getName);

        List<String> rawJars = new ArrayList<>();
        getRawMeta(desc)
                .map(v -> v.rawData().get("raw-libraries"))
                .ifPresent(value -> {
                    if (value instanceof List<?> rawLibs)
                        rawLibs.forEach(v -> rawJars.add(v.toString()));
                });

        ClassLoader classLoader = super.createLoader(desc);
        if (classLoader == null && rawJars.isEmpty()) return null;

        Stream<URL> urls = Stream.empty();
        if (classLoader != null) {
            URLClassLoader urlClassLoader = (URLClassLoader)classLoader;
            urls = Stream.concat(urls, Arrays.stream(urlClassLoader.getURLs()));
        }
        if (!rawJars.isEmpty()) {
            this.logger.log(Level.INFO, "[{0}] Loading {1} raw libraries... please wait", new Object[] { LOG_PREFIX, rawJars.size() });
            urls = Stream.concat(urls, rawJars.stream().map(v -> {
                try {
                    URL url = new File("plugins/libs/" + v).toURI().toURL();
                    this.logger.log(Level.INFO, "[{0}] Loaded raw library {1}", new Object[] { LOG_PREFIX, v });
                    return url;
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }));
        }

        List<URLClassLoader> otherLoaders = new ArrayList<>();
        urls.forEach(url -> {
            otherLoaders.add(classLoaders.computeIfAbsent(url, v -> {
                this.logger.log(Level.INFO, "[{0}] Create new class loader {1}", new Object[] { LOG_PREFIX, v });
                return new URLClassLoader(new URL[] { v }, parentClassLoader);
            }));
            this.logger.log(Level.INFO, "[{0}] Loading class loader {1}", new Object[] { LOG_PREFIX, url });
        });
        if (otherLoaders.isEmpty()) return null;
        return new JoinClassLoader(parentClassLoader, otherLoaders.toArray(URLClassLoader[]::new));
    }
}
