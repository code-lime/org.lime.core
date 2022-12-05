package org.lime.timings.lib;

abstract public class MCTiming implements AutoCloseable {
    public abstract MCTiming startTiming();
    public abstract void stopTiming();
    @Override public void close() { stopTiming(); }
}

