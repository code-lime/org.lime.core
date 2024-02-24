package net.minecraft.paper.java;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
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
    /*
    @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
        // It would be easier to call the loadClass() methods of the delegateClassLoaders
        // here, but we have to load the class from the byte code ourselves, because we
        // need it to be associated with our class loader.
        String path = name.replace('.', '/') + ".class";
        URL url = findResource(path);
        if (url == null) {
            throw new ClassNotFoundException(name);
        }
        ByteBuffer byteCode;
        try {
            byteCode = loadResource(url);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
        return defineClass(name, byteCode, null);
    }
    */
    private ByteBuffer loadResource(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            int initialBufferCapacity = Math.min(0x40000, stream.available() + 1);
            if (initialBufferCapacity <= 2) {
                initialBufferCapacity = 0x10000;
            } else {
                initialBufferCapacity = Math.max(initialBufferCapacity, 0x200);
            }
            ByteBuffer buf = ByteBuffer.allocate(initialBufferCapacity);
            while (true) {
                if (!buf.hasRemaining()) {
                    ByteBuffer newBuf = ByteBuffer.allocate(2 * buf.capacity());
                    buf.flip();
                    newBuf.put(buf);
                    buf = newBuf;
                }
                int len = stream.read(buf.array(), buf.position(), buf.remaining());
                if (len <= 0) {
                    break;
                }
                buf.position(buf.position() + len);
            }
            buf.flip();
            return buf;
        }
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