package org.lime.system.delete;

public class DeleteHandle implements IDelete {
    private boolean _deleted = false;

    public boolean isDeleted() {
        return _deleted;
    }

    public void delete() {
        _deleted = true;
    }
}
