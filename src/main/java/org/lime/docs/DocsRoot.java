package org.lime.docs;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.stream.Stream;

public record DocsRoot(
    ImmutableList<IDocs> docs
) implements IDocs {
    public DocsRoot(DocsRoot old, IDocs item) {
        this(ImmutableList.<IDocs>builder()
                .addAll(old.docs())
                .add(item)
                .build()
        );
    }

    public static DocsRoot of() { return new DocsRoot(ImmutableList.of()); }
    public static DocsRoot of(Stream<IDocs> docs) { return new DocsRoot(docs.collect(ImmutableList.toImmutableList())); }
    public static DocsRoot of(Iterator<IDocs> docs) { return new DocsRoot(ImmutableList.copyOf(docs)); }
    public static DocsRoot of(Iterable<IDocs> docs) { return new DocsRoot(ImmutableList.copyOf(docs)); }
    public static DocsRoot of(IDocs... docs) { return new DocsRoot(ImmutableList.copyOf(docs)); }
    public DocsRoot add(IDocs item) { return new DocsRoot(this, item); }

    @Override public Stream<String> lines() { return docs.stream().flatMap(IDocs::lines); }
}















