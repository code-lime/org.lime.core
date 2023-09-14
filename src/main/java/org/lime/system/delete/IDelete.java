package org.lime.system.delete;

import org.lime._system;

public interface IDelete {
    IDelete NONE = new IDelete() {
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
