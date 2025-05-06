package org.lime.core.paper;

import org.bukkit.plugin.java.JavaPlugin;
import org.lime.core.common.api.BaseIdentity;

public interface PaperIdentity extends BaseIdentity {
    JavaPlugin plugin();
}
