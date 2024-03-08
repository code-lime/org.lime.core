package net.minecraft.paper.java;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class JoinClassLoader extends ClassLoader {
    private final ClassLoader[] delegateClassLoaders;

    private final List<ClassLoader> classLoaders = new ArrayList<>();

    public JoinClassLoader(ClassLoader parent, ClassLoader...delegateClassLoaders) {
        super(parent);
        this.delegateClassLoaders = delegateClassLoaders;

        this.classLoaders.add(parent);
        Collections.addAll(this.classLoaders, delegateClassLoaders);
    }
    @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ClassLoader classLoader : classLoaders) {
            try { return classLoader.loadClass(name); } catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException(name);
    }
    @Override protected URL findResource(String name) {
        for (ClassLoader delegate: delegateClassLoaders) {
            URL resource = delegate.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }
    @Override protected Enumeration<URL> findResources(String name) throws IOException {
        Vector<URL> vector = new Vector<URL>();
        for (ClassLoader delegate: delegateClassLoaders) {
            Enumeration<URL> enumeration = delegate.getResources(name);
            while (enumeration.hasMoreElements()) {
                vector.add(enumeration.nextElement());
            }
        }
        return vector.elements();
    }
}