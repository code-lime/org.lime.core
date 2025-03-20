package net.minecraft.paper.java;

import com.google.common.collect.Streams;
import net.minecraft.paper.java.view.OtherView;
import net.minecraft.unsafe.Native;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.LibraryLoader;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
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

    private Optional<RawPluginMeta> getRawMeta(Object data) {
        return data instanceof RawPluginMeta rpm
                ? Optional.of(rpm)
                : Optional.empty();
    }

    private static Object mapStringObject(Object in, Function<String, String> modify) {
        return in instanceof Map<?,?> map
                ? mapStringMap(map, modify)
                : in instanceof Collection<?> list
                ? mapStringCollection(list, modify)
                : in instanceof String str
                ? modify.apply(str)
                : in;
    }
    private static Collection<?> mapStringCollection(Collection<?> in, Function<String, String> modify) {
        return OtherView.<Object>of(in, v -> mapStringObject(v, modify));
    }
    private static Map<?,?> mapStringMap(Map<?,?> in, Function<String, String> modify) {
        return OtherView.<Object>ofValues(in, v -> mapStringObject(v, modify));
    }

    @Override public @Nullable ClassLoader createLoader(@Nonnull PluginDescriptionFile desc, List<java.nio.file.Path> paths) {
        final String LOG_PREFIX = Objects.requireNonNullElseGet(desc.getPrefix(), desc::getName);

        Optional<Map<?,?>> meta = getRawMeta(desc)
                .map(RawPluginMeta::rawData)
                .map(v -> mapStringMap(v, str -> {
                    if (!str.startsWith("=") || !str.endsWith("="))
                        return str;
                    String envName = str.substring(1, str.length() - 1);
                    String ret = Objects.requireNonNullElse(System.getenv(envName), str);
                    logger.warning("Get env '"+envName+"': " + ret);
                    return ret;
                }));

        List<String> rawJars = new ArrayList<>();
        meta
                .map(v -> v.get("raw-libraries"))
                .ifPresent(value -> {
                    if (value instanceof List<?> rawLibs)
                        rawLibs.forEach(v -> rawJars.add(v.toString()));
                });

        if (!rawJars.isEmpty()) {
            this.logger.log(Level.INFO, "[{0}] Loading {1} raw libraries... please wait", new Object[] { LOG_PREFIX, rawJars.size() });
            Stream<Path> additionalPaths = rawJars.stream().map(v -> {
                Path path = Path.of("plugins", "libs", v);
                Path abs = path.toAbsolutePath();
                if (!Files.exists(abs)) {
                    this.logger.log(Level.WARNING, "[{0}] Error load raw library {1}. File {2} not found", new Object[] { LOG_PREFIX, v, abs });
                    throw new RuntimeException("File " + abs + " not found");
                }
                this.logger.log(Level.INFO, "[{0}] Loaded raw library {1}", new Object[] { LOG_PREFIX, v });
                return abs;
            });
            paths = (paths == null ? additionalPaths : Streams.concat(paths.stream(), additionalPaths)).toList();
        }

        return super.createLoader(desc, paths);
    }
}
