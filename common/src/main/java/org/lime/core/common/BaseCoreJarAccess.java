package org.lime.core.common;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.system.execute.Func0;
import org.lime.core.common.system.execute.FuncEx2;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

public interface BaseCoreJarAccess {
    Stream<Path> jars();

    default Stream<String> jarClasses(@Nullable FuncEx2<JarEntry, JarInputStream, Boolean> filter) {
        return jars()
                .flatMap(path -> {
                    List<String> classes = new ArrayList<>();
                    try (JarInputStream zis = new JarInputStream(new FileInputStream(path.toFile()))) {
                        JarEntry entry;
                        while (true) {
                            entry = zis.getNextJarEntry();
                            if (entry == null)
                                break;
                            try {
                                String name = entry.getName();
                                if (!name.endsWith(".class"))
                                    continue;
                                name = name.substring(0, name.length() - 6).replace('/', '.');
                                if (filter != null && !filter.invoke(entry, zis))
                                    continue;
                                classes.add(name);
                            } finally {
                                zis.closeEntry();
                            }
                        }
                    } catch (Throwable e) {
                        throw new IllegalArgumentException(e);
                    }
                    return classes.stream();
                });
    }
    default Stream<String> jarClasses() {
        return jarClasses((FuncEx2<JarEntry, JarInputStream, Boolean>) null);
    }
    default Stream<String> jarClasses(Func0<AcceptClassVisitor> acceptVisitor) {
        return jarClasses((_, zis) -> {
            ClassReader reader = new ClassReader(zis.readAllBytes());
            AcceptClassVisitor visitor = acceptVisitor.invoke();
            reader.accept(visitor, 0);
            return visitor.isAccept();
        });
    }

    abstract class AcceptClassVisitor extends ClassVisitor {
        private boolean accept = false;

        protected AcceptClassVisitor() {
            this(Opcodes.ASM9);
        }
        protected AcceptClassVisitor(int api) {
            super(api);
        }

        public void accept() {
            this.accept = true;
        }
        public boolean isAccept() {
            return this.accept;
        }
    }
}
