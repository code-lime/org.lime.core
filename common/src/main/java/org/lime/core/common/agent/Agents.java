package org.lime.core.common.agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.tuple.LockTuple1;
import org.lime.core.common.utils.tuple.Tuple;

import java.lang.instrument.Instrumentation;
import java.util.List;

public class Agents {
    private static final LockTuple1<@Nullable List<Agent>> agents = Tuple.lock(null);
    public static void load(Instrumentation instrumentation) {
        agents.invoke(v -> {
            if (v.val0 != null)
                return;
            v.val0 = List.of(new OpenAllAgent());
            v.val0.forEach(agent -> agent.run(instrumentation));
        });
    }
    public static void load() {
        load(ByteBuddyAgent.install());
    }
    public static void unload() {
        agents.invoke(v -> {
            if (v.val0 == null)
                return;
            v.val0.forEach(Agent::close);
            v.val0 = null;
        });
    }
}
