package org.lime.core.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.lime.core.common.api.BaseIdentity;

import java.util.List;
import java.util.Optional;

public interface FabricIdentity extends BaseIdentity {
    String modId();

    static Optional<FabricIdentity> of(ModInitializer initializer) {
        List<EntrypointContainer<ModInitializer>> containers = FabricLoader.getInstance()
                .getEntrypointContainers("main", ModInitializer.class);

        String modId = null;
        String name = null;

        for (EntrypointContainer<ModInitializer> container : containers) {
            if (container.getEntrypoint() == initializer) {
                var metadata = container.getProvider().getMetadata();
                modId = metadata.getId();
                name = metadata.getName();
                break;
            }
        }

        if (modId == null || name == null)
            return Optional.empty();

        String finalModId = modId;
        String finalName = name;

        return Optional.of(new FabricIdentity() {
            @Override
            public String modId() {
                return finalModId;
            }
            @Override
            public String name() {
                return finalName;
            }
        });
    }
}
