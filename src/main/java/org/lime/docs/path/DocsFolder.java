package org.lime.docs.path;

import com.google.common.collect.ImmutableList;
import org.lime.docs.IDocs;
import org.lime.system.execute.Func1;

import java.nio.file.Path;
import java.util.stream.Stream;

public final class DocsFolder extends BaseDocsPath {
    public final ImmutableList<BaseDocsPath> elements;

    private DocsFolder(String name, ImmutableList<BaseDocsPath> elements) {
        super(name);
        this.elements = elements;
        this.elements.forEach(element -> element.setParent(this));
    }
    private DocsFolder(DocsFolder old, BaseDocsPath element) {
        this(old.name, ImmutableList.<BaseDocsPath>builder().addAll(old.elements).add(element).build());
    }

    public static DocsFolder root(String name) {
        return new DocsFolder(name, ImmutableList.of());
    }

    public DocsFolder file(String name, Func1<DocsFile, DocsFile> builder) {
        return new DocsFolder(this, builder.invoke(new DocsFile(name, ImmutableList.of())));
    }
    public DocsFolder folder(String name, Func1<DocsFolder, DocsFolder> builder) {
        return new DocsFolder(this, builder.invoke(new DocsFolder(name, ImmutableList.of())));
    }

    @Override public boolean save(Path folder) {
        for (BaseDocsPath path : elements)
            if (!path.save(folder.resolve(pathPart())))
                return false;
        return true;
    }
    @Override public Stream<String> table(DocsFile owner) {
        return Stream.concat(
                Stream.of("- " + name),
                elements.stream()
                        .flatMap(v -> v.table(owner))
                        .map(v -> "  " + v));
    }
}
