package org.lime.core.fabric;

import org.lime.core.common.agent.Agents;

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
    public void disable() {
        super.disable();
        Agents.unload();
    }
}
