package org.lime.core.common.docs.json;

import org.jetbrains.annotations.Nullable;

public interface IEnumDocs {
    default @Nullable IJElement docsElement() { return null; }
    default @Nullable IComment docsComment() { return null; }
}
