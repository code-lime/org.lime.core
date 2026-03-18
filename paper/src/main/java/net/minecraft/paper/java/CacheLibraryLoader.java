package net.minecraft.paper.java;

import com.google.common.collect.Streams;
import net.minecraft.java.maven.RemoteRepositoryImpl;
import net.minecraft.java.view.OtherView;
import net.minecraft.unsafe.GlobalConfigure;
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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class CacheLibraryLoader extends LibraryLoader {
    private final Logger logger;

    private final RepositorySystem repository;
    private final DefaultRepositorySystemSession session;

    private static final BiFunction<URL[], ClassLoader, URLClassLoader> defaultFactory;
    private static final ReentrantLock cacheLock = new ReentrantLock();
    private static final HashSet<URL> cacheUrls = new HashSet<>();
    private static URLClassLoader cacheLoader = null;

    static {
        defaultFactory = LibraryLoader.LIBRARY_LOADER_FACTORY;
        LibraryLoader.LIBRARY_LOADER_FACTORY = CacheLibraryLoader::cacheLibraryLoader;

        GlobalConfigure.configure();
    }

    private static URLClassLoader cacheLibraryLoader(URL[] urls, ClassLoader parent) {
        cacheLock.lock();
        try {
            HashSet<URL> append = new HashSet<>();
            for (URL url : urls)
                if (cacheUrls.add(url))
                    append.add(url);

            if (!append.isEmpty())
                cacheLoader = defaultFactory.apply(append.toArray(URL[]::new), Objects.requireNonNullElse(cacheLoader, parent));
            return cacheLoader;
        } finally {
            cacheLock.unlock();
        }
    }

    private final RepositoryLibraryLoader access;

    public CacheLibraryLoader(Logger logger) {
        super(logger);
        this.logger = logger;
        this.access = (RepositoryLibraryLoader)this;
        this.repository = this.access.repository();
        this.session = this.access.session();
    }

    private static Optional<RawPluginMeta> getRawMeta(Object data) {
        return data instanceof RawPluginMeta rpm
                ? Optional.of(rpm)
                : Optional.empty();
    }
    private static Optional<Map<?,?>> getRawMetaData(Object data) {
        return getRawMeta(data)
                .map(RawPluginMeta::rawData)
                .map(v -> mapStringMap(v, str -> {
                    if (!str.startsWith("=") || !str.endsWith("="))
                        return str;
                    String envName = str.substring(1, str.length() - 1);
                    return Objects.requireNonNullElse(System.getenv(envName), str);
                }));
    }

    private RepositoryPolicy cast(RemoteRepositoryImpl.PolicyImpl impl) {
        return new RepositoryPolicy(impl.enable(), impl.updatePolicy(), impl.checksumPolicy());
    }
    private Proxy cast(RemoteRepositoryImpl.ProxyImpl impl) {
        return new Proxy(impl.protocol(), impl.host(), impl.port(), Optional.ofNullable(impl.auth())
                .map(this::cast)
                .orElse(null));
    }
    private Authentication cast(RemoteRepositoryImpl.AuthenticationImpl impl) {
        return new AuthenticationBuilder()
                .addUsername(impl.username())
                .addPassword(impl.password())
                .build();
    }
    private RemoteRepository cast(RemoteRepositoryImpl impl) {
        var repo = new RemoteRepository.Builder(impl.id(), impl.type(), impl.url());
        Optional.ofNullable(impl.snapshotPolicy()).map(this::cast).ifPresent(repo::setSnapshotPolicy);
        Optional.ofNullable(impl.releasePolicy()).map(this::cast).ifPresent(repo::setReleasePolicy);
        Optional.ofNullable(impl.auth()).map(this::cast).ifPresent(repo::setAuthentication);
        Optional.ofNullable(impl.proxy()).map(this::cast).ifPresent(repo::setProxy);
        return repo.build();
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

    public static List<String> getCustomLibraries(PluginDescriptionFile desc) {
        Optional<Map<?,?>> meta = getRawMetaData(desc);
        return meta
                .map(v -> v.get("custom-libraries"))
                .map(v -> {
                    var customLibraries = (Collection<?>)v;
                    List<String> libraries = new ArrayList<>(desc.getLibraries());
                    customLibraries.forEach(customLibrary -> libraries.add(customLibrary.toString()));
                    return libraries;
                })
                .orElseGet(desc::getLibraries);
    }

    @Override public @Nullable ClassLoader createLoader(@Nonnull PluginDescriptionFile desc, List<Path> paths) {
        final String LOG_PREFIX = Objects.requireNonNullElseGet(desc.getPrefix(), desc::getName);

        Optional<Map<?,?>> meta = getRawMetaData(desc);

        meta
                .map(v -> v.get("maven"))
                .ifPresent(value -> {
                    List<RemoteRepository> repositories = new ArrayList<>();
                    repositories.add(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());

                    var map = ((Map<?, ?>) value);

                    logger.log(Level.INFO, "[{0}] Registering {1} repositories... please wait", new Object[] { LOG_PREFIX, map.size() });
                    map.forEach((k, v) -> {
                        var repo = RemoteRepositoryImpl.of(k.toString(), v);
                        repositories.add(cast(repo));
                        this.logger.log(Level.INFO, "[{0}] Registered repository {1}", new Object[] { LOG_PREFIX, repo.toString() });
                    });

                    var newList = repository.newResolutionRepositories(session, repositories);

                    var list = this.access.repositories();
                    list.clear();
                    list.addAll(newList);
                });

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
