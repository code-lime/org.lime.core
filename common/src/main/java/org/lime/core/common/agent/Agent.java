package org.lime.core.common.agent;

import java.io.Closeable;
import java.lang.instrument.Instrumentation;

public interface Agent
        extends Closeable {
    void run(Instrumentation instrumentation);
    @Override void close();
}
