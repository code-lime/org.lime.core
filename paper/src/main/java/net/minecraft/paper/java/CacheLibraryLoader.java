package net.minecraft.paper.java;

import com.google.common.collect.Streams;
import net.minecraft.paper.java.view.OtherView;
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

    private Optional<RawPluginMeta> getRawMeta(Object data) {
        return data instanceof RawPluginMeta rpm
                ? Optional.of(rpm)
                : Optional.empty();
    }

    private String readUpdatePolicy(Map<?, ?> dat) {
        String updatePolicy = Objects.toString(dat.get("update"), RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        boolean updateIntervalPolicy = false;
        if (!updatePolicy.equals(RepositoryPolicy.UPDATE_POLICY_ALWAYS)
                && !updatePolicy.equals(RepositoryPolicy.UPDATE_POLICY_DAILY)
                && !updatePolicy.equals(RepositoryPolicy.UPDATE_POLICY_NEVER)
                && !(updateIntervalPolicy = updatePolicy.startsWith(RepositoryPolicy.UPDATE_POLICY_INTERVAL + ":")))
            throw new IllegalArgumentException("Update policy '" + updatePolicy + "' not supported");
        if (updateIntervalPolicy) {
            String intervalNumber = updatePolicy.substring(RepositoryPolicy.UPDATE_POLICY_INTERVAL.length() + 1);
            try {
                int interval = Integer.parseInt(intervalNumber);
                if (interval <= 0) {
                    throw new IllegalArgumentException("Update policy '" + updatePolicy + "' must be a positive integer after ':', but got: " + interval);
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Update policy '" + updatePolicy + "' must be a valid integer after ':', but got: " + intervalNumber, ex);
            }
        }
        return updatePolicy;
    }
    private String readChecksumPolicy(Map<?, ?> dat) {
        String checksumPolicy = Objects.toString(dat.get("checksum"), RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        if (!checksumPolicy.equals(RepositoryPolicy.CHECKSUM_POLICY_FAIL)
                && !checksumPolicy.equals(RepositoryPolicy.CHECKSUM_POLICY_WARN)
                && !checksumPolicy.equals(RepositoryPolicy.CHECKSUM_POLICY_IGNORE))
            throw new IllegalArgumentException("Checksum policy '" + checksumPolicy + "' not supported");
        return checksumPolicy;
    }
    private RepositoryPolicy readPolicy(Object value) {
        if (value instanceof Map<?,?> dat) {
            String enableStr = Objects.toString(dat.get("enable"), "true");
            boolean enable = switch (enableStr) {
                case "true" -> true;
                case "false" -> false;
                default -> throw new IllegalArgumentException("Enable '" + enableStr + "' can be only 'true' or 'false'");
            };
            String updatePolicy = readUpdatePolicy(dat);
            String checksumPolicy = readChecksumPolicy(dat);

            return new RepositoryPolicy(enable, updatePolicy, checksumPolicy);
        } else {
            String enableStr = value.toString();
            boolean enable = switch (enableStr) {
                case "true" -> true;
                case "false" -> false;
                default -> throw new IllegalArgumentException("Enable '" + enableStr + "' can be only 'true' or 'false'");
            };
            return new RepositoryPolicy(enable, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
        }
    }

    private Authentication readAuthentication(Object value) {
        Map<?, ?> dat = (Map<?,?>) value;
        return new AuthenticationBuilder()
                .addUsername(dat.get("username").toString())
                .addPassword(dat.get("password").toString())
                .build();
    }
    private Proxy readProxy(Object value) {
        Map<?, ?> dat = (Map<?,?>) value;
        String host = dat.get("host").toString();
        int port = Integer.parseInt(dat.get("port").toString());
        Authentication proxyAuth = null;
        if (dat.containsKey("auth"))
            proxyAuth = readAuthentication(dat.get("auth"));
        return new Proxy("http", host, port, proxyAuth);
    }
    private RemoteRepository readRemote(String id, Object value) {
        RemoteRepository.Builder repo;
        if (value instanceof Map<?,?> dat) {
            String url = dat.get("url").toString();
            repo = new RemoteRepository.Builder(id, "default", url);
            dat.forEach((kk,vv) -> {
                var _ = switch (kk.toString()) {
                    case "snapshot" -> repo.setSnapshotPolicy(readPolicy(vv));
                    case "release" -> repo.setReleasePolicy(readPolicy(vv));
                    case "auth" -> repo.setAuthentication(readAuthentication(vv));
                    case "proxy" -> repo.setProxy(readProxy(vv));
                    default -> repo;
                };
            });
        } else {
            repo = new RemoteRepository.Builder(id, "default", value.toString());
        }
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

    @Override public @Nullable ClassLoader createLoader(@Nonnull PluginDescriptionFile desc, List<Path> paths) {
        final String LOG_PREFIX = Objects.requireNonNullElseGet(desc.getPrefix(), desc::getName);

        Optional<Map<?,?>> meta = getRawMeta(desc)
                .map(RawPluginMeta::rawData)
                .map(v -> mapStringMap(v, str -> {
                    if (!str.startsWith("=") || !str.endsWith("="))
                        return str;
                    String envName = str.substring(1, str.length() - 1);
                    String ret = Objects.requireNonNullElse(System.getenv(envName), str);
                    return ret;
                }));

        meta
                .map(v -> v.get("maven"))
                .ifPresent(value -> {
                    List<RemoteRepository> repositories = new ArrayList<>();
                    repositories.add(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());

                    var map = ((Map<?, ?>) value);

                    logger.log(Level.INFO, "[{0}] Registering {1} repositories... please wait", new Object[] { LOG_PREFIX, map.size() });
                    map.forEach((k, v) -> {
                        var repo = readRemote(k.toString(), v);
                        repositories.add(repo);
                        this.logger.log(Level.INFO, "[{0}] Registered repository {1}", new Object[] { LOG_PREFIX, repo.getId() + "::" + repo.getUrl() });
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
