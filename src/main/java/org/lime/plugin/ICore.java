package org.lime.plugin;

import org.lime.LimeCore;

public interface ICore {
    void core(LimeCore base_core);

    LimeCore core();

    public static class Abstract implements ICore {
        public LimeCore base_core;

        @Override
        public void core(LimeCore base_core) {
            this.base_core = base_core;
        }

        @Override
        public LimeCore core() {
            return this.base_core;
        }
    }
}
