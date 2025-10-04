package org.lime.core.fabric.services.objectives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.minecraft.world.scores.Scoreboard;
import org.lime.core.fabric.services.NativeComponent;

@Singleton
public class ObjectiveService {
    @Inject Scoreboard scoreboard;
    @Inject NativeComponent nativeComponent;

    public ObjectiveAccess access(String objectiveName) {
        return new ObjectiveAccess(this, objectiveName);
    }
}
