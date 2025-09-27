package org.lime.core.fabric;

import org.lime.core.common.services.InstancesUtility;

import java.util.stream.Stream;

class FabricInstancesUtility
        implements InstancesUtility {
    @Override
    public BaseFabricMod core() {
        return FabricInstanceProvider.getCore();
    }
    @Override
    public Stream<? extends BaseFabricMod> instances() {
        return FabricInstanceProvider.getOwners();
    }
}
