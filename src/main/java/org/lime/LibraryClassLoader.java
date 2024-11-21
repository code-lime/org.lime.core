package org.lime;

import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.PluginClassLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@SuppressWarnings("all")
public class LibraryClassLoader extends URLClassLoader {
    //DummyBukkitPluginLoader
    /**Классы, загруженные из файла*/
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

    /**Плагин, в который классы из файла будут загружены*/
    private final Class<? extends JavaPlugin> pluginClass;

    private record JarInfo(JarFile jar, URL url) {}

    /**Файл, из которого будут загружены классы*/
    private final List<File> files;

    /**JarFile и URL файлов*/
    private List<JarInfo> jars;

    /**Загружено ли содержимое файла или нет*/
    private boolean loaded = false;

    /**
     * Конструктор класса LibraryClassLoader
     * @param plugin Плагин, в который классы из файла будут загружены
     * @param files Файлы, из которого будут загружены классы
     * @exception IllegalStateException Загрузчик для файла уже зарегистрирован
     * */
    public LibraryClassLoader(Class<? extends JavaPlugin> pluginClass, List<File> files) {
        super(new URL[]{}, pluginClass.getClassLoader());
        this.files = files;
        this.pluginClass = pluginClass;
    }
    public Iterable<File> getFiles() { return files; }
    private void writeInfo(String line) {
        System.out.println("[INFO:"+pluginClass.getSimpleName()+"] " + line);
    }
    private void writeWarning(String line) {
        System.out.println("[WARN:"+pluginClass.getSimpleName()+"] " + line);
    }

    public static List<String> getClasses(File file) {
        if (!file.exists()) throw new IllegalArgumentException("File " + file.getName() + " not found");
        if (!file.getName().endsWith(".jar")) throw new IllegalArgumentException("File " + file.getName() + " has an invalid extension");
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();
            List<String> list = new ArrayList<>();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!isClassEntry(entry)) continue;
                String path = entry.getName();
                String name = path.substring(0, entry.getName().lastIndexOf(".")).replace("/", ".");
                list.add(name);
            }
            return list;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Загрузить классы из файла в загрузчик классов плагина
     * @exception IllegalStateException Содержимое файла уже загружено
     * @exception IllegalArgumentException Файл не существует, либо файл не является jar файлом
     * */
    public synchronized void load() {
        String key = this.files.stream().map(v -> v.getName()).collect(Collectors.joining(" & "));
        writeInfo("Attempting to load the " + key + " files...");

        if (this.loaded) throw new IllegalStateException("Files " + key + " already loaded");
        List<JarInfo> jars = new ArrayList<>();
        this.files.forEach(file -> {
            if (!file.exists()) throw new IllegalArgumentException("File " + file.getName() + " not found");
            if (!file.getName().endsWith(".jar")) throw new IllegalArgumentException("File " + file.getName() + " has an invalid extension");

            URL url = null;
            JarFile jar = null;
            try {
                url = file.toURI().toURL();
                jar = new JarFile(file);
            } catch (IOException e) {
                writeWarning("An unexpected error occurred while processing the "
                        + file.getName() + " file");
                return;
            }
            this.addURL(url);
            jars.add(new JarInfo(jar, url));
        });
        this.jars = jars;

        jars.forEach(info -> {
            Enumeration<JarEntry> entries = info.jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!this.isClassEntry(entry)) continue;
                String path = entry.getName();
                String name = path.substring(0, entry.getName().lastIndexOf(".")).replace("/", ".");
                try {
                    Class<?> c = this.loadClass(name);
                    this.classes.put(c.getName(), c);
                } catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
            }
        });


        this.loadToPluginClassLoader();
        writeInfo("The " + key + " files has been loaded (" + this.classes.size() + " classes loaded)");
    }

    /**
     * Загрузить классы из файла в загрузчик классов плагина
     * @exception IllegalStateException Содержимое файла уже загружено
     * @exception IllegalArgumentException Файл не существует, либо файл не является jar файлом
     * */
    public synchronized Class<?> loadOnce(String tClass) {
        String key = this.files.stream().map(v -> v.getName()).collect(Collectors.joining(" & "));
        LimeCore.instance._logOP("Attempting to load the " + key + " files and class "+tClass+"...");

        if (this.loaded) throw new IllegalStateException("Files " + key + " already loaded");
        List<JarInfo> jars = new ArrayList<>();
        this.files.forEach(file -> {
            if (!file.exists()) throw new IllegalArgumentException("File " + file.getName() + " not found");
            if (!file.getName().endsWith(".jar")) throw new IllegalArgumentException("File " + file.getName() + " has an invalid extension");

            URL url = null;
            JarFile jar = null;
            try {
                url = file.toURI().toURL();
                jar = new JarFile(file);
            } catch (IOException e) {
                writeWarning("An unexpected error occurred while processing the "
                        + file.getName() + " file");
                return;
            }
            this.addURL(url);
            jars.add(new JarInfo(jar, url));
        });
        this.jars = jars;

        return jars.stream().map(info -> {
            Enumeration<JarEntry> entries = info.jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!this.isClassEntry(entry)) continue;
                String path = entry.getName();
                String name = path.substring(0, entry.getName().lastIndexOf(".")).replace("/", ".");
                if (!name.equals(tClass)) continue;
                try {
                    Class<?> c = this.loadClass(name);
                    this.classes.put(c.getName(), c);

                    //this.loadToPluginClassLoader();
                    LimeCore.instance._logOP("The " + key + " file has been loaded (" + this.classes.size() + " classes loaded)");
                    return c;
                } catch (ClassNotFoundException ignored) {}
            }
            return null;
        }).filter(v -> v != null).findFirst().orElse(null);
    }

    /**
     * Выгрузить классы из загрузчика классов плагина
     * @param close true, если загрузчик классов должен быть удален после выгрузки, иначе - false
     * @exception IllegalStateException Содержимое файла не загружено
     * */
    public synchronized void unload(boolean close) {
        String key = this.files.stream().map(v -> v.getName()).collect(Collectors.joining(" & "));
        writeInfo("Attempting to unload the " + key + " files");

        if (!this.loaded) throw new IllegalStateException("Files " + key + " not loaded");

        this.unloadFromPluginClassLoader();
        if (close) try { this.close(); } catch (IOException ignored) {}
    }

    /**
     * Проверить, является ли JarEntry объект классом
     * @param entry JarEntry объект, который необходимо проверить
     * @return true, если JarEntry объект является классом, иначе - false
     * */
    private static boolean isClassEntry(JarEntry entry) {
        if (entry.isDirectory()) return false;
        if (!entry.getName().endsWith(".class")) return false;
        if (entry.getName().endsWith("module-info.class")) return false;
        return true;
    }

    /**
     * Один из костылей.
     * */
    @Override
    public URL getResource(String name) {
        return this.findResource(name);
    }

    /**
     * Костыль №2.
     * */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return this.findResources(name);
    }

    /**
     * Костыль №3. Здесь нужно было переопределить метод, чтобы классы не сохранялись в загрузчике
     * */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name,true);
    }

    /**
     * Костыль №4. Позволяет во время загрузки находить и загружать классы,
     * которые используются в уже загруженных классах
     * */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> c = this.classes.get(name);
        if (c != null)
            return c;

        String path = name.replace(".", "/").concat(".class");
        for (JarInfo info : jars) {
            JarEntry entry = info.jar.getJarEntry(path);

            if (entry != null) {
                byte[] classBytes;
                try (InputStream in = info.jar.getInputStream(entry)) {
                    classBytes = new byte[in.available()];
                    in.read(classBytes);
                } catch (IOException ex) {
                    throw new ClassNotFoundException(name, ex);
                }

                CodeSigner[] signers = entry.getCodeSigners();
                CodeSource source = new CodeSource(info.url, signers);
                try {
                    c = super.defineClass(name, classBytes, 0, classBytes.length, source);
                } catch (ClassFormatError ignored) {}
            }
        }

        if (c == null)
            c = super.findClass(name);

        this.classes.put(name, c);
        return c;
    }

    /**
     * Получить классы, зарегистрированные в загрузчике классов плагина
     * @return Таблицу загруженных классов, привязанных к плагину, если плагин инициализирован.
     * Иначе null
     * */
    private Map<String, Class<?>> getLoadedClasses() {
        Field f;
        try {
            if (pluginClass.getClassLoader() instanceof PluginClassLoader loader) {
                f = loader.getClass().getDeclaredField("classes");
                f.setAccessible(true);
                return (Map<String, Class<?>>) f.get(loader);
            }
            /*
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (!(classLoader instanceof ConfiguredPluginClassLoader)) {
            throw new IllegalStateException("JavaPlugin requires to be created by a valid classloader.");
        }
        ConfiguredPluginClassLoader configuredPluginClassLoader = (ConfiguredPluginClassLoader)((Object)classLoader);
        configuredPluginClassLoader.init(this);
            */
            ConfiguredPluginClassLoader loader = (ConfiguredPluginClassLoader)this.getClass().getClassLoader();
            writeInfo(loader + "");
            /*PluginLoader javaPluginLoader = INSTANCE;
            f = javaPluginLoader.getClass().getDeclaredField("loaders");
            f.setAccessible(true);
            List<PluginClassLoader> loaders = (List<PluginClassLoader>) f.get(javaPluginLoader);
            PluginClassLoader loader = null;
            for (PluginClassLoader classLoader: loaders) {
                if (classLoader.getPlugin().getClass() != pluginClass)
                    continue;
                loader = classLoader;
                break;
            }*/
            if (loader == null)
                throw new IllegalStateException("The " + pluginClass.getName() + " plugin is disabled");
            f = loader.getClass().getDeclaredField("classes");
            f.setAccessible(true);
            return (Map<String, Class<?>>) f.get(loader);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        return null;
    }

    /**
     * Загрузить классы, полученные из файла, в загрузчик классов плагина
     * @exception IllegalStateException Плагин не инициализирован в ядре
     * @exception IllegalStateException Файл не содержал классов или классы из файла уже загружены
     * */
    private boolean loadToPluginClassLoader() {
        Map<String, Class<?>> classes = this.getLoadedClasses();
        if (classes == null)
            throw new IllegalStateException("Plugin " + pluginClass.getName() + " not loaded");
        Map<String, Class<?>> loaded = new ConcurrentHashMap<>();
        for (Map.Entry<String, Class<?>> entry: this.classes.entrySet()) {
            if (classes.containsKey(entry.getKey())) {
                this.resolveClass(entry.getValue());
                continue;
            }
            classes.put(entry.getKey(), entry.getValue());
            loaded.put(entry.getKey(), entry.getValue());
        }
        this.classes.clear();
        if (loaded.size() == 0) return false;
        //   throw new IllegalStateException("No class of the " + file.getName() + " file was loaded");
        this.classes.putAll(loaded);
        this.loaded = true;
        return true;
    }

    /**
     * Выгрузить классы из загрузчика классов плагина
     * @exception IllegalStateException Плагин не инициализирован в ядре
     * */
    private void unloadFromPluginClassLoader() {
        Map<String, Class<?>> classes = this.getLoadedClasses();
        if (classes == null)
            throw new IllegalStateException("Plugin " + pluginClass.getName() + " not loaded");
        for (Map.Entry<String, Class<?>> entry: this.classes.entrySet())
            classes.remove(entry.getKey());
        this.classes.clear();
        this.loaded = false;
        if (this.jars != null) {
            this.jars.forEach(info -> { try { info.jar.close(); } catch (IOException ignored) {} });
            this.jars = null;
        }
    }

    /**
     * Закрыть загрузчик классов
     * */
    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (this.jars != null) {
                this.jars.forEach(info -> { try { info.jar.close(); } catch (IOException ignored) {} });
            }
        }
    }

    private Collection<Class<?>> getClasses() {
        return classes.values();
    }

}