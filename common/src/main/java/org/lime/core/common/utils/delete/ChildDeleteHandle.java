package org.lime.core.common.utils.delete;

public class ChildDeleteHandle extends DeleteHandle {
    private final Delete base;

    public ChildDeleteHandle(Delete base) {
        this.base = base;
    }

    public boolean isDeleted() {
        return super.isDeleted() || base.isDeleted();
    }
}
