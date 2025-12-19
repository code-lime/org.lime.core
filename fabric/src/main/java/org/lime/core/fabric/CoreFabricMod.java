package org.lime.core.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.metadata.ModDependency;
import org.lime.core.common.agent.Agents;

import java.util.*;
import java.util.stream.Collectors;

public final class CoreFabricMod
        extends BaseFabricMod {
    static {
        Agents.load();
    }

    @Override
    protected BaseFabricInstanceModule createModule() {
        return new BaseFabricInstanceModule(this);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> scheduleTaskService
                .runNextTick(() -> loadingByDependencySort(FabricInstanceProvider.getOwners()
                        .collect(Collectors.toMap(BaseFabricMod::id, v -> v)))
                        .forEach(BaseFabricMod::enable), true));
    }

    private List<BaseFabricMod> loadingByDependencySort(Map<String, BaseFabricMod> mods) {
        List<BaseFabricMod> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        mods.forEach((modId, mod) -> dfs(modId, mod, mods, visited, visiting, result));
        return result;
    }
    private void dfs(
            String modId,
            BaseFabricMod mod,
            Map<String, BaseFabricMod> mods,
            Set<String> visited,
            Set<String> visiting,
            List<BaseFabricMod> result) {
        if (visiting.contains(modId))
            throw new IllegalStateException("Cycled dependency: " + modId);

        if (visited.contains(modId))
            return;

        visiting.add(modId);
        mod.metadata.getDependencies()
                .stream()
                .filter(v -> v.getKind().isPositive())
                .map(ModDependency::getModId)
                .forEach(depModId -> {
                    var depMod = mods.get(depModId);
                    if (depMod == null)
                        return;
                    dfs(depModId, depMod, mods, visited, visiting, result);
                });

        visiting.remove(modId);
        visited.add(modId);
        result.add(mod);
    }

    @Override
    public void disable() {
        super.disable();
        Agents.unload();
    }
}
