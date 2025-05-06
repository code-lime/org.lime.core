package org.lime.core.paper;

import org.lime.core.common.api.BaseState;

public interface PaperState extends BaseState, PaperIdentity {
    @Override
    default boolean isEnabled() {
        return plugin().isEnabled();
    }
}
