package org.lime.system.delete;

public class ChildDeleteHandle extends DeleteHandle {
    private final IDelete base;

    public ChildDeleteHandle(IDelete base) {
        this.base = base;
    }

    public boolean isDeleted() {
        return super.isDeleted() || base.isDeleted();
    }
}
