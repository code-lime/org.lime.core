package org.lime.core.common.docs.path;

import org.lime.core.common.docs.IParent;
import org.lime.core.common.docs.IPath;

import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.stream.Stream;

public abstract class BaseDocsPath implements IParent, IPath {
    private DocsFolder folder = null;
    public final String name;

    public BaseDocsPath(String name) {
        this.name = name;
    }

    @Override public @Nullable IParent parent() { return folder; }
    @Override public String pathPart() { return name; }

    protected void setParent(DocsFolder folder) {
        this.folder = folder;
    }

    public abstract boolean save(Path folder);
    public abstract Stream<String> table(DocsFile owner);
}
