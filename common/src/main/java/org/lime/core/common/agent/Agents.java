package org.lime.core.common.agent;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.Instrumentation;

public class Agents {
    public static void load(Instrumentation instrumentation) {
        OpenAllAgent.load(instrumentation);
    }
    public static void load() {
        load(ByteBuddyAgent.install());
    }
}
