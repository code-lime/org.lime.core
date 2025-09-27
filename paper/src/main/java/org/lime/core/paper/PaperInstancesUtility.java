package org.lime.core.paper;

import org.lime.core.common.services.InstancesUtility;

import java.util.stream.Stream;

class PaperInstancesUtility
        implements InstancesUtility {
    @Override
    public BasePaperInstance<?> core() {
        return PaperInstanceProvider.getCore().instance;
    }
    @Override
    public Stream<? extends BasePaperInstance<?>> instances() {
        return PaperInstanceProvider.getOwners().map(v -> v.instance);
    }
}
