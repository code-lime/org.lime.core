package org.lime.core.fabric;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import org.lime.core.common.api.BaseLogger;

public interface FabricLogger extends BaseLogger, FabricServer {
    default AudienceProvider serverAudiences() {
        return FabricServerAudiences.of(server());
    }

    @Override
    default Audience consoleAudiences() {
        return serverAudiences().console();
    }
    @Override
    default Audience playersAudiences(boolean operatorsOnly) {
        return operatorsOnly
                ? Audience.audience(server().getPlayerList()
                .getPlayers()
                .stream()
                .filter(v -> v.hasPermissions(3))
                .toList())
                : serverAudiences().players();
    }
}
