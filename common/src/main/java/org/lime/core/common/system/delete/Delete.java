package org.lime.core.common.system.delete;

public interface Delete {
     Delete NONE = new Delete() {
        @Override
        public boolean isDeleted() {
            return false;
        }

        @Override
        public void delete() {
        }
    };

    boolean isDeleted();

    void delete();
}
