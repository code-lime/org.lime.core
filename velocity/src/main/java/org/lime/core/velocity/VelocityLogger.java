package org.lime.core.velocity;

import net.kyori.adventure.audience.Audience;
import org.lime.core.common.api.BaseLogger;

public interface VelocityLogger extends BaseLogger, VelocityServer {
    @Override
    default Audience consoleAudiences() {
        return server().getConsoleCommandSource();
    }
    @Override
    default Audience playersAudiences(boolean operatorsOnly) {
        return operatorsOnly
                ? Audience.audience(server().getAllPlayers()
                .stream()
                .filter(v -> v.hasPermission("velocity.operator"))
                .toList())
                : Audience.audience(server().getAllPlayers());
    }
}
