package org.lime.core.paper;

import com.google.common.collect.Iterables;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.permissions.ServerOperator;
import org.lime.core.common.api.BaseLogger;

public interface PaperLogger extends BaseLogger {
    @Override
    default Audience consoleAudiences() {
        return Bukkit.getConsoleSender();
    }

    @Override
    default Audience playersAudiences(boolean operatorsOnly) {
        return operatorsOnly
                ? Audience.audience(Iterables.filter(Bukkit.getOnlinePlayers(), ServerOperator::isOp))
                : Audience.audience(Bukkit.getOnlinePlayers());
    }
}
