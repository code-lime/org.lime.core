package org.lime.core.velocity;

import org.lime.core.common.services.InstancesUtility;

import java.util.stream.Stream;

class VelocityInstancesUtility
        implements InstancesUtility {
    @Override
    public BaseVelocityPlugin core() {
        return VelocityInstanceProvider.getCore();
    }
    @Override
    public Stream<? extends BaseVelocityPlugin> instances() {
        return VelocityInstanceProvider.getOwners();
    }
}
