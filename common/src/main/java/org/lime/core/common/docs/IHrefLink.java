package org.lime.core.common.docs;

import org.jetbrains.annotations.Nullable;

public interface IHrefLink {
    String addHref(String title, String index, @Nullable String path, String fragment);
}