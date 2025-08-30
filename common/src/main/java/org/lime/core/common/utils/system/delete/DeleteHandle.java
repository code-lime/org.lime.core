package org.lime.core.common.utils.system.delete;

public class DeleteHandle implements Delete {
    private boolean _deleted = false;

    public boolean isDeleted() {
        return _deleted;
    }

    public void delete() {
        _deleted = true;
    }
}
