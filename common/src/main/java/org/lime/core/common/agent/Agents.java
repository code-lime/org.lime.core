package org.lime.core.common.agent;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.tuple.LockTuple1;
import org.lime.core.common.utils.tuple.Tuple;

import java.lang.instrument.Instrumentation;

public class Agents {
    private static final LockTuple1<Agent @Nullable []> agents = Tuple.lock(null);
    public static void load(Instrumentation instrumentation) {
        agents.invoke(v -> {
            if (v.val0 != null)
                return;
            //noinspection resource
            v.val0 = new Agent[]
            {
                    new OpenAllAgent(),
            };
            try {
                for (Agent agent : v.val0) {
                    System.out.println("[Agent] Load agent " + agent.getClass().getSimpleName());
                    agent.run(instrumentation);
                }
            } catch (Throwable ex) {
                System.out.println("ERROR : " + ex);
                throw new RuntimeException(ex);
            }
        });
    }
    public static void load() {
        load(ByteBuddyAgent.install());
    }
    public static void unload() {
        agents.invoke(v -> {
            if (v.val0 == null)
                return;
            for (var i : v.val0)
                i.close();
            v.val0 = null;
        });
    }
}
