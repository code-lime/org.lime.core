package org.lime.core.common.utils;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.utils.execute.Func0;
import org.lime.core.common.utils.execute.FuncEx2;
import org.objectweb.asm.*;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;

public class JarAccessUtils {
    private static Stream<String> jarClasses(
            Stream<Path> jars,
            @Nullable FuncEx2<JarEntry, JarInputStream, Boolean> filter) {
        return jars
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
    private static Stream<String> jarClasses(
            Stream<Path> jars) {
        return jarClasses(jars, (FuncEx2<JarEntry, JarInputStream, Boolean>) null);
    }
    private static Stream<String> jarClasses(
            Stream<Path> jars,
            Func0<AcceptClassVisitor> acceptVisitor) {
        return jarClasses(jars, (v0, zis) -> {
            ClassReader reader = new ClassReader(zis.readAllBytes());
            AcceptClassVisitor visitor = acceptVisitor.invoke();
            reader.accept(visitor, 0);
            return visitor.isAccept();
        });
    }

    public static Stream<Class<?>> findAnnotatedClasses(
            Logger logger,
            Class<? extends Annotation> annotationClass,
            Stream<Path> jars,
            ClassLoader loader) {
        final String ANNOTATION_DESCRIPTOR = Type.getDescriptor(annotationClass);

        return jarClasses(jars, () -> new AcceptClassVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (descriptor.equals(ANNOTATION_DESCRIPTOR))
                    accept();
                return super.visitAnnotation(descriptor, visible);
            }
        }).flatMap(className -> {
            try {
                return Stream.of(loader.loadClass(className));
            } catch (Throwable e) {
                logger.error("Error load {}", className, e);
                return Stream.empty();
            }
        });
    }

    private static abstract class AcceptClassVisitor extends ClassVisitor {
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
