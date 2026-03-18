package org.lime.core.velocity.dependencies;

import com.google.gson.JsonElement;
import com.velocitypowered.api.plugin.PluginManager;
import net.minecraft.java.maven.RemoteRepositoryImpl;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.Lazy;
import org.lime.core.velocity.CoreVelocityPlugin;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class MavenResolverVelocityDependencyLoader
        implements VelocityDependencyLoader, Disposable {
    private static final String MAVEN_CENTRAL_DEFAULT_MIRROR = getDefaultMavenCentralMirror();
    private static String getDefaultMavenCentralMirror() {
        String central = System.getenv("PAPER_DEFAULT_CENTRAL_REPOSITORY");
        if (central == null)
            central = System.getProperty("org.bukkit.plugin.java.LibraryLoader.centralURL");
        if (central == null)
            central = "https://maven-central.storage-download.googleapis.com/maven2";
        return central;
    }

    private final Disposable.Composite composite = Disposable.composite();

    private final CoreVelocityPlugin plugin;
    private final Logger logger;
    private final PluginManager pluginManager;
    private final RepositorySystem system;
    private final RepositorySystemSession.CloseableSession session;

    private final List<RemoteRepository> repositories = new ArrayList<>();
    private final HashSet<Path> loadedPaths = new HashSet<>();

    private Supplier<List<RemoteRepository>> resolutionRepositories;

    public MavenResolverVelocityDependencyLoader(
            CoreVelocityPlugin plugin,
            Logger logger,
            Path dataDirectory) {
        this.plugin = plugin;
        this.logger = logger;
        this.pluginManager = plugin.server.getPluginManager();

        RepositorySystemSupplier systemSupplier = new RepositorySystemSupplier();
        system = systemSupplier.get();

        SessionBuilderSupplier builderSupplier = new SessionBuilderSupplier(system);
        RepositorySystemSession.SessionBuilder sb = builderSupplier.get();

        Path localBase = dataDirectory.resolve("libraries");
        try {
            Files.createDirectories(localBase);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sb.withLocalRepositoryBaseDirectories(localBase);

        session = sb.build();

        RemoteRepository central = new RemoteRepository.Builder("central", "default", MAVEN_CENTRAL_DEFAULT_MIRROR).build();
        repositories.add(central);

        resolutionRepositories = createCachedResolutionRepositories();
    }

    private Supplier<List<RemoteRepository>> createCachedResolutionRepositories() {
        return Lazy.of(() -> system.newResolutionRepositories(session, repositories))::value;
    }

    @Override
    public void close() {
        composite.close();
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

    @Override
    public void loadRepository(String id, JsonElement repository) {
        var repo = RemoteRepositoryImpl.of(id, repository);
        repositories.add(cast(repo));
        logger.info("Registered repository {}", repo);
        resolutionRepositories = createCachedResolutionRepositories();
    }
    @Override
    public void loadDependency(String dependency) {
        Dependency root = new Dependency(new DefaultArtifact(dependency), "runtime");

        CollectRequest collect = new CollectRequest();
        collect.setRoot(root);
        collect.setRepositories(resolutionRepositories.get());

        DependencyRequest depReq = new DependencyRequest(collect, null);

        DependencyResult result;
        try {
            result = system.resolveDependencies(session, depReq);
        } catch (DependencyResolutionException e) {
            throw new RuntimeException(e);
        }

        result.getArtifactResults().forEach(artifactResult -> {
            var exs = artifactResult.getExceptions();
            if (!exs.isEmpty())
                exs.forEach(ex -> logger.error("Error load {}", dependency, ex));

            var artifact = artifactResult.getArtifact();
            if (artifact == null)
                throw new RuntimeException("Artifact "+dependency+" not loaded");

            var path = artifact.getPath().toAbsolutePath();
            if (loadedPaths.add(path)) {
                pluginManager.addToClasspath(plugin, path);
                logger.info("Loaded {} at library file {}", artifact, path);
            }
        });
    }
}
