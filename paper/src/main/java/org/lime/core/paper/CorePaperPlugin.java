package org.lime.core.paper;

public final class CorePaperPlugin
        extends BasePaperPlugin {
    @Override
    protected BasePaperInstanceModule<Instance> module(Instance instance) {
        return new BasePaperInstanceModule<>(instance);
    }
}
