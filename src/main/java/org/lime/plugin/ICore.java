package org.lime.plugin;

import org.lime.core;
import org.lime.plugin.CoreElement;

public interface ICore {
    void core(core base_core);

    core core();

    public static class Abstract implements ICore {
        public core base_core;

        @Override
        public void core(core base_core) {
            this.base_core = base_core;
        }

        @Override
        public core core() {
            return this.base_core;
        }
    }
}
