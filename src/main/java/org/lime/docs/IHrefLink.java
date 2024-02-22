package org.lime.docs;

import javax.annotation.Nullable;

public interface IHrefLink {
    String addHref(String title, String index, @Nullable String path, String fragment);
}