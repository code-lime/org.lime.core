package org.lime.core.fabric.services.objectives;

public abstract class ScoreProvider<Key> {
    public final Key key;
    public final ObjectiveAccess owner;
    public ScoreProvider(Key key, ObjectiveAccess owner) {
        this.key = key;
        this.owner = owner;
    }

    public Key key() {
        return key;
    }
    public ObjectiveAccess owner() {
        return owner;
    }

    public abstract int get();
    public abstract void set(int value);
    public abstract int add(int value);
    public abstract void reset();
}
