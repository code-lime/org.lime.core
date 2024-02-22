package org.lime.docs.path;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.lime.core;
import org.lime.docs.*;
import org.lime.system.execute.Func2;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DocsFile extends BaseDocsPath implements IDocs, IHrefLink {
    public final ImmutableList<IDocs> docs;

    public DocsFile(String name, ImmutableList<IDocs> docs) {
        super(name);
        this.docs = docs;
        this.docs.forEach(element -> {
            if (element instanceof IIndexGroup index)
                index.changeParent(this);
        });
    }
    private <T>DocsFile(DocsFile old, T data, Func2<ImmutableList.Builder<IDocs>, T, ImmutableList.Builder<IDocs>> builder) {
        this(old.name, builder.invoke(ImmutableList.<IDocs>builder().addAll(old.docs), data).build());
    }

    @Override public String pathPart() { return super.pathPart() + ".md"; }

    public DocsFile add(Stream<IDocs> docs) { return new DocsFile(this, docs.iterator(), ImmutableList.Builder::addAll); }
    public DocsFile add(Iterator<IDocs> docs) { return new DocsFile(this, docs, ImmutableList.Builder::addAll); }
    public DocsFile add(Iterable<IDocs> docs) { return new DocsFile(this, docs, ImmutableList.Builder::addAll); }
    public DocsFile add(IDocs... docs) { return new DocsFile(this, docs, ImmutableList.Builder::add); }

    private record HrefInfo(String title, String index, @Nullable String path, String fragment) {
        public static IDocs href(Set<HrefInfo> hrefCache) {
            return () -> Stream.concat(Stream.of("-----"), hrefCache
                    .stream()
                    .sorted(Comparator.comparing(v -> v.index))
                    .flatMap(v -> Stream.concat(
                            IIndexDocs.marker(v.index, v.title, 4),
                            v.path != null
                                    ? Stream.of(" [\\[Перейти к определению\\]](" + v.path + "#" + v.index + ")")
                                    : Stream.of("<warning>Ошибка! Определение объекта не найдено!</warning>")
                    )));
        }
    }

    private final HashSet<HrefInfo> hrefCache = new HashSet<>();
    private final LinkedList<String> logs = new LinkedList<>();
    @Override public String addHref(String title, String index, @Nullable String path, String fragment) {
        hrefCache.add(new HrefInfo(title, index, path, fragment));
        return index;
        /*return hrefCache.computeIfAbsent(new HrefInfo(title, index, path, fragment), v -> {
            //logs.add("Href: '" + title + "' with path '" + path + "' and fragment '" + fragment + "'");
            //logs.add(" - Absolute: " + pathAbsolute);
            //logs.add(" - Base: " + pathBase);
            return "#" + IIndexDocs.RAW_HREF_PREFIX;
            //return "#" + path.replace('/', '-').replace("..", "_") + "&" + fragment;
        });*/
    }
    @Override public Stream<String> lines() {
        hrefCache.clear();
        logs.clear();
        return Streams.concat(
                Stream.of(IDocs.style()),
                IParent.parentTree(this)
                        .flatMap(v -> v instanceof BaseDocsPath path ? Stream.of(path) : Stream.empty())
                        .reduce((a, b) -> b)
                        .stream()
                        .map(v -> () -> Stream.concat(Stream.of("### Список файлов"), v.table(this))),
                docs.stream(),
                Stream.of(HrefInfo.href(hrefCache)),
                Stream.of(logs::stream)
        ).flatMap(IDocs::lines);
    }
    public String save() { return lines().collect(Collectors.joining("\n")); }
    @Override public String toString() { return save(); }

    @Override public boolean save(Path folder) {
        File folderFile = folder.toFile();
        if (!folderFile.exists() && !folderFile.mkdirs()) {
            core.instance._logOP("ERROR SAVE TO: " + folder + " ("+folderFile.exists()+")");
            return false;
        }
        try {
            Files.writeString(folder.resolve(pathPart()), save());
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override public Stream<String> table(DocsFile owner) {
        if (owner == this)
            return Stream.of("- <name>"+name+"</name>");

        Path pathAbsolute = Paths.get(IPath.parentPath(this));
        Path pathBase = Paths.get(IPath.parentPath(owner));
        if (pathAbsolute.equals(pathBase))
            return Stream.of("- <name>"+name+"</name>");
        Path pathCurrent = pathBase.getParent();
        if (pathCurrent == null)
            return Stream.of("- <name>"+name+"</name> <warning>Ошибка! Определение файла не найдено!</warning>");

        Path pathRelative = pathCurrent.relativize(pathAbsolute);
        String file = pathRelative.toString();
        return Stream.of("- ["+name+"]("+file+")");
    }
}
