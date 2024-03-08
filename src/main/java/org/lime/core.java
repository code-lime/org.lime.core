package org.lime;

import com.google.common.collect.Streams;

import org.bukkit.plugin.java.PluginClassLoader;
import org.lime.invokable.IInvokable;
import org.lime.plugin.*;
import org.lime.system.execute.Func1;
import patch.core.MutatePatcher;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class core extends CoreLoader {
    static {
        MutatePatcher.register();
    }

    public static core instance;

    @Override public String getLogPrefix() { return "LIME:" + this.getName().toUpperCase(); }
    @Override public String getConfigFile() { return "plugins/" + this.getName().toLowerCase() + "/"; }

    private final List<CoreElement> elements = new ArrayList<>();
    private final HashMap<String, CoreCommand<?>> commands = new HashMap<>();
    private final List<LibraryClassLoader> libraries = new ArrayList<>();
    private final ConcurrentLinkedQueue<IInvokable> tickCalls = new ConcurrentLinkedQueue<>();

    @Override public TimerBuilder _timer() { return TimerBuilder.create(this); }
    @Override public void _invokable(IInvokable invokable) { tickCalls.add(invokable); }

    @Override public CoreElementLoaded add(CoreElement element) {
        if (element.disable) return CoreElementLoaded.disabled(element);
        elements.add(element);
        if (element.instance instanceof ICore icore) icore.core(this);
        return new CoreElementLoaded() {
            @Override public void cancel() { if (elements.remove(element) && element.uninit != null) element.uninit.invoke(); }
            @Override public Optional<CoreElement> element() { return Optional.of(element); }
            @Override public String name() { return element.name; }
            @Override public Class<?> type() { return element.tClass; }
        };
    }
    @Override public void add(CoreCommand<?> command) { commands.put(command.cmd, command); }
    @Override public void add(String cmd, Func1<CoreCommand<?>, CoreCommand<?>> builder) { add(builder.invoke(CoreCommand.create(cmd))); }

    @Override public void library(File... jars) {
        LibraryClassLoader loader = new LibraryClassLoader(this.getClass(), Arrays.asList(jars));
        loader.load();
        rawLibrary(loader);
    }
    @Override public void library(String... jars) {
        library(Arrays.stream(jars).map(this::_getConfigFile).toArray(File[]::new));
    }
    @Override protected void rawLibrary(LibraryClassLoader loader) {
        libraries.add(loader);
        _logOP("Library "+Streams.stream(loader.getFiles()).map(File::getAbsolutePath).collect(Collectors.joining(" & "))+" loaded!");
    }

    public Collection<String> getJarClassesNames() {
        if (this.getClassLoader() instanceof PluginClassLoader loader) {
            try (JarFile jar = new JarFile(this.getFile())) {
                return Streams.stream(jar.entries().asIterator())
                        .map(JarEntry::getName)
                        .filter(v -> v.endsWith(".class"))
                        .map(v -> v.substring(0, v.length() - 6).replace('/', '.'))
                        .toList();
            }
            catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return Collections.emptyList();
    }
    public List<CoreElementLoaded> addOther() {
        List<CoreElementLoaded> other = new ArrayList<>();
        if (this.getClassLoader() instanceof PluginClassLoader loader) {
            try (JarFile jar = new JarFile(this.getFile())) {
                jar.entries().asIterator().forEachRemaining(entry -> {
                    String class_name = entry.getName();
                    if (!class_name.endsWith(".class")) return;
                    class_name = class_name.substring(0, class_name.length() - 6).replace('/', '.');
                    try {
                        Class<?> tClass = loader.loadClass(class_name);
                        Method method = reflection.access(tClass.getDeclaredMethod("create"));
                        if (!Modifier.isStatic(method.getModifiers())) return;
                        if (method.getReturnType() != CoreElement.class) return;
                        if (elements.stream().anyMatch(v -> v.tClass == tClass)) return;
                        other.add(add((CoreElement)method.invoke(null)));
                    } catch (NoSuchMethodException ignored) {

                    } catch (Throwable e) {
                        core.instance._logOP("ERROR LOAD: " + class_name);
                        core.instance._logStackTrace(e);
                    }
                });
            }
            catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return other;
    }

    @Override public Stream<CoreElement> elements() { return this.elements.stream(); }
    @Override public Stream<LibraryClassLoader> libraries() { return this.libraries.stream(); }

    @Override protected Map<String, CoreCommand<?>> commands() { return this.commands; }
    @Override protected Optional<IUpdateConfig> config() {
        return Stream.concat(Stream.of(this), elements.stream().map(v -> v.instance))
                .map(v -> v instanceof IUpdateConfig config ? config : null)
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Override protected void init() {}
    @Override protected void invokableTick() {
        tickCalls.removeIf(v -> {
            try { return v.tryRemoveInvoke(); }
            catch (Throwable e) { _logStackTrace(e); }
            return true;
        });
    }
}



















