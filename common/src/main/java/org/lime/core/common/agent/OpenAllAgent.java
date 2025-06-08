package org.lime.core.common.agent;

import java.lang.instrument.Instrumentation;
import java.lang.ModuleLayer;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenAllAgent {
    public static void load(Instrumentation instrumentation) {
        System.out.println("[Agent] Begin all-open agent");

        Module javaBase = ModuleLayer.boot()
                .findModule("java.base")
                .orElseThrow(() -> new IllegalStateException("java.base not found"));

        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "open-all-agent");
            t.setDaemon(true);
            return t;
        });

        Loader loader = new Loader(instrumentation, javaBase);
        loader.tick();
        exec.scheduleAtFixedRate(loader::tick, 1, 1, TimeUnit.SECONDS);
    }

    private static class Loader {
        private final Instrumentation instrumentation;
        private final Module javaBase;
        private final Set<String> javaBasePackages;

        private final Set<Module> seen = new HashSet<>();

        public Loader(Instrumentation instrumentation, Module javaBase) {
            this.instrumentation = instrumentation;
            this.javaBase = javaBase;

            this.javaBasePackages = javaBase.getPackages();

            System.out.println("[Agent] Loaded " + javaBasePackages.size() + " packages from " + javaBase.getName() + " module");
        }

        public void tick() {
            try {
                Set<Module> all = Stream.concat(ModuleLayer.boot()
                                .modules()
                                .stream(), Arrays.stream(instrumentation.getAllLoadedClasses())
                                .map(Class::getModule))
                        .collect(Collectors.toSet());
                Set<Module> newbies = all.stream()
                        .filter(m -> !seen.contains(m))
                        .collect(Collectors.toSet());
                if (!newbies.isEmpty()) {
                    Map<String, Set<Module>> opens = javaBasePackages.stream()
                            .collect(Collectors.toMap(pkg -> pkg, pkg -> all));
                    instrumentation.redefineModule(javaBase, Set.of(), Map.of(), opens, Set.of(), Map.of());
                    seen.addAll(newbies);
                    System.out.println("[Agent] Opened java.base to " + newbies
                            .stream()
                            .map(v -> v.isNamed() ? v.getName() : "@" + Integer.toHexString(System.identityHashCode(v)))
                            .sorted()
                            .toList());
                }
                TimeUnit.MILLISECONDS.sleep(40);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
