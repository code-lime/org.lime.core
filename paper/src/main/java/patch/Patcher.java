package patch;

import net.minecraft.server.Main;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.lime.core.common.utils.json.JsonElementOptional;
import org.lime.core.common.utils.json.builder.Json;
import org.lime.core.common.utils.system.Digest;
import org.lime.core.common.utils.system.execute.Action1;
import org.lime.core.common.utils.system.tuple.Tuple;
import org.lime.core.common.utils.system.tuple.Tuple2;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

public class Patcher {
    private static final List<BasePluginPatcher> patchers = new ArrayList<>();

    public static void addPatcher(BasePluginPatcher patcher) {
        patchers.add(patcher);
    }

    private static Path of(Class<?> tClass) throws Throwable {
        return new File(tClass.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath();
    }
    private static Path loadBaseFile(Path base, boolean resetCacheOriginal) throws Throwable {
        Path dir = base.getParent();
        Path orig = Paths.get(
                dir.toString(),
                FilenameUtils.getBaseName(base.toString()) + "-orig." + FilenameUtils.getExtension(base.toString())
        );
        if (resetCacheOriginal || !Files.exists(orig)) Files.copy(base, orig, StandardCopyOption.REPLACE_EXISTING);
        return orig;
    }

    private static Tuple2<String, String> calculateVersion(List<BasePluginPatcher> patchers) throws Throwable {
        List<ModifyClass> checkFiles = new ArrayList<>();
        try (var ignored = Native.subLog()) {
            for (BasePluginPatcher patcher : patchers) {
                checkFiles.addAll(patcher.modifyList());
            }
        }
        checkFiles.sort(Comparator.comparing(ModifyClass::name));
        return Tuple.of(
                Digest.SHA1.miniHash(checkFiles.stream().flatMap(v -> Stream.of(v.name().getBytes(), v.rawClass()))),
                Digest.SHA1.miniHash(checkFiles.stream().filter(ModifyClass::isModify).flatMap(v -> Stream.of(v.name().getBytes(), v.rawClass())))
        );
    }

    public static void patch() {
        patch(null);
    }
    public static void patch(@Nullable Action1<String> logger) {
        try {
            if (logger != null)
                Native.logger = logger;
            throwablePatch(patchers);
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }
    private static void throwablePatch(List<BasePluginPatcher> patchers) throws Throwable {
        Native.log("Check patch...");
        Path paperPath = Paths.get(ManagementFactory.getRuntimeMXBean().getClassPath());
        Native.log("Read paper jar...");
        JarArchive paperArchive = JarArchive.of("paper", paperPath);

        for (BasePluginPatcher patcher : patchers) {
            Native.log("Read " + patcher.name + " plugin jar...");
            patcher.pluginArchive(JarArchive.of("plugin", of(patcher.plugin)));
        }

        Native.log("Calculate patch version...");

        boolean resetCacheOriginal = !paperArchive.entries.containsKey("lime-patch.json");

        Tuple2<String, String> currentVersion = Optional.ofNullable(paperArchive.entries.get("lime-patch.json"))
                .map(String::new)
                .map(Json::parse)
                .map(JsonElementOptional::of)
                .flatMap(JsonElementOptional::getAsJsonObject)
                .flatMap(v -> v.getAsJsonObject("version"))
                .flatMap(v -> v.getAsString("value")
                        .flatMap(patch -> v.getAsString("append")
                                .map(append -> Tuple.of(patch, append))))
                .orElse(null);

        Tuple2<String, String> patchVersion = calculateVersion(patchers);

        Native.log("Current patch version: " + (currentVersion == null ? "Not patched" : currentVersion));
        Native.log("New patch version:     " + patchVersion);

        if (patchVersion.equals(currentVersion)) return;

        boolean isOnlyAppendPart = currentVersion == null || !currentVersion.val1.equals(patchVersion.val1);

        Native.log("Patch...");
        paperArchive.entries.put("lime-patch.json", Json.format(Json.object()
                .addObject("version", v -> v
                        .add("value", isOnlyAppendPart ? "??????????" : patchVersion.val0)
                        .add("append", patchVersion.val1)
                ).build()
        ).getBytes());

        Path versionBase = of(Main.class);
        String sha256Old = Digest.SHA256.hash(Files.readAllBytes(versionBase));
        Path bukkitBase = of(Bukkit.class);
        String bukkitOldSha256 = Digest.SHA256.hash(Files.readAllBytes(bukkitBase));

        Native.log("Getting original version jar...");
        Path versionOrig = loadBaseFile(versionBase, resetCacheOriginal);
        Path bukkitOrig = loadBaseFile(bukkitBase, resetCacheOriginal);

        Native.log("Read version jar...");
        JarArchive versionArchive = JarArchive.of("version", versionOrig);
        Native.subLog("Append:", () -> patchers.forEach(patcher -> patcher.modifyList().forEach(modify -> {
            if (!modify.isModify()) return;
            Native.log("Append '"+modify.name()+"'...");
            versionArchive.entries.put(modify.name(), modify.rawClass());
        })));

        if (isOnlyAppendPart) {
            Native.log("Save version jar...");
        } else {
            Native.log("Read bukkit jar...");
            JarArchive bukkitArchive = JarArchive.of("bukkit", bukkitOrig);

            JarArchiveAuto archive = new JarArchiveAuto(versionArchive, bukkitArchive);

            Native.log("Read deobf file...");
            for (BasePluginPatcher patcher : patchers)
                patcher.patch(archive);
            Native.log("Save version jar...");
            bukkitArchive.toFile(bukkitBase);
        }
        versionArchive.toFile(versionBase);

        String baseSha256 = Digest.SHA256.hash(Files.readAllBytes(versionBase));
        String origSha256 = Digest.SHA256.hash(Files.readAllBytes(versionOrig));
        String bukkitBaseSha256 = Digest.SHA256.hash(Files.readAllBytes(bukkitBase));
        String bukkitOrigSha256 = Digest.SHA256.hash(Files.readAllBytes(bukkitOrig));

        Native.log("Apply paper jar...");
        Native.log(" - " + sha256Old + " or " + origSha256 + " > " + baseSha256);
        Native.log(" - " + bukkitOldSha256 + " or " + bukkitOrigSha256 + " > " + bukkitBaseSha256);
        Native.log("Apply paper jar...");
        for (String path : new String[] { "META-INF/versions.list", "META-INF/patches.list", "META-INF/libraries.list" }) {
            paperArchive.entries.put(path, new String(paperArchive.entries.get(path))
                    .replace(sha256Old, baseSha256)
                    .replace(origSha256, baseSha256)
                    .replace(bukkitOldSha256, bukkitBaseSha256)
                    .replace(bukkitOrigSha256, bukkitBaseSha256)
                    .getBytes());
        }

        Native.log("Save paper jar...");
        paperArchive.toFile(paperPath);

        Native.log("Patch status: OK");
        Native.log("Exit...");

        Runtime.getRuntime().halt(0);
    }
}
