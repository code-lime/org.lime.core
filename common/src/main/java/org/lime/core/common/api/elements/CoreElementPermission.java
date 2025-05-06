package org.lime.core.common.api.elements;

public interface CoreElementPermission<Self extends CoreElementPermission<Self>> {
    Self addPermissions(String... permissions);

    default Self addPermission(String permission) {
        return addPermissions(permission);
    }
}
