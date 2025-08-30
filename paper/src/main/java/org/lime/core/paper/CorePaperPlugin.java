package org.lime.core.paper;

import org.lime.core.common.agent.Agents;
import patch.Patcher;
import patch.core.MutatePatcher;

public final class CorePaperPlugin
        extends BasePaperPlugin {
    static {
        Agents.load();
        MutatePatcher.register();
    }

    @Override
    public void onEnable() {
        Patcher.patch();
        super.onEnable();
    }
    @Override
    public void onDisable() {
        super.onDisable();
        Agents.unload();
    }

    @Override
    protected BasePaperInstanceModule<Instance> module(Instance instance) {
        return new BasePaperInstanceModule<>(instance);
    }
}
