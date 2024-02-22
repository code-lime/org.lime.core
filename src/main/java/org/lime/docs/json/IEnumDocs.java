package org.lime.docs.json;

import javax.annotation.Nullable;

public interface IEnumDocs {
    default @Nullable IJElement docsElement() { return null; }
    default @Nullable IComment docsComment() { return null; }
}
