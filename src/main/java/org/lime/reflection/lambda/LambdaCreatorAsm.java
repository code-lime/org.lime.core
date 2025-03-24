package org.lime.reflection.lambda;

import com.google.common.primitives.Primitives;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.PluginClassLoader;
import org.lime.LimeCore;
import org.lime.reflection.Lambda;
import org.lime.reflection.Reflection;
import org.lime.reflection.TestNative;
import org.lime.system.tuple.Tuple;
import org.lime.system.tuple.Tuple1;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureWriter;

import javax.annotation.Nullable;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class LambdaCreatorAsm implements LambdaCreator {
    private static void visitUnboxingType(MethodVisitor mv, Class<?> primitiveType) {
        Class<?> argType = Primitives.wrap(primitiveType);
        if (primitiveType != argType) {
            String owner = Type.getInternalName(argType);
            String methodName = primitiveType.getSimpleName() + "Value";
            String descriptor = Type.getMethodDescriptor(Type.getType(primitiveType));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    owner, methodName,
                    descriptor, false);
        } else {
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(argType));
        }
    }
    private static void visitBoxingType(MethodVisitor mv, Class<?> argType) {
        if (argType.isPrimitive()) {
            Class<?> wrapperType = Primitives.wrap(argType);
            String owner = Type.getInternalName(wrapperType);
            String methodDescriptor = Type.getMethodDescriptor(Type.getType(wrapperType), Type.getType(argType));
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "valueOf", methodDescriptor, false);
        } else {
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(argType));
        }
    }

    private static class CombinedClassLoader extends ClassLoader {
        private final ClassLoader[] classLoaders;

        public CombinedClassLoader(ClassLoader parent, ClassLoader... classLoaders) {
            super(parent);
            this.classLoaders = Arrays.stream(classLoaders)
                    .filter(v -> !parent.equals(v))
                    .toArray(ClassLoader[]::new);
        }
        public CombinedClassLoader(Class<?> parent, Class<?>... classes) {
            this(parent.getClassLoader(), getLoaders(classes));
        }
        private static ClassLoader[] getLoaders(Class<?>[] classes) {
            Set<ClassLoader> loaders = new HashSet<>();
            for (Class<?> clazz : classes) {
                ClassLoader loader = clazz.getClassLoader();
                loaders.add(loader);
            }
            return loaders.toArray(ClassLoader[]::new);
        }

        private static @Nullable Class<?> tryLoadClass(ClassLoader loader, String name) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException _) {
                return null;
            }
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                System.out.println("LOAD CLASS [LOADED] " + name);
                return loadedClass;
            }
            loadedClass = tryLoadClass(getParent(), name);
            if (loadedClass != null){
                System.out.println("LOAD CLASS [PARENT] " + name);
                return loadedClass;
            }

            for (ClassLoader cl : classLoaders) {
                loadedClass = tryLoadClass(cl, name);
                if (loadedClass != null){
                    System.out.println("LOAD CLASS ["+cl+"] " + name);
                    return loadedClass;
                }
            }

            throw new ClassNotFoundException("Class " + name + " not found");
        }
    }
    private static <T>Class<?> defineClass(Class<?> dClass, Class<T> tClass, byte[] bytes) throws Throwable {
        MethodHandles.Lookup lookup = TestNative.allowedModes(MethodHandles.privateLookupIn(dClass, TestNative.lookup(dClass)));
        ClassLoader loader = LimeCore.instance.getClass().getClassLoader();//dClass.getClassLoader();//new CombinedClassLoader(dClass, tClass);
        System.out.println("TRY DEFINE CLASS: " + dClass);
        for (var p : Bukkit.getPluginManager().getPlugins()) {
            PluginClassLoader pcl = (PluginClassLoader)p.getClass().getClassLoader();
            System.out.println(" - " + p.getName());
            try {
                Class<?> loaded = pcl.loadClass(dClass.getName(), false, false, true);
                if (loaded == null)
                    continue;
                //lookup = TestNative.allowedModes(MethodHandles.privateLookupIn(p.getClass(), TestNative.lookup(p.getClass())));
                loader = pcl;
                break;
            } catch (Throwable e) {
                //System.out.println("   - CLASS "+name+" NOT LOADED: " + e.getMessage());
            }
            /*
            Collection<Class<?>> classes;
            if (pl instanceof PluginClassLoader pcl) {
                classes = (Collection<Class<?>>)ReflectionMethod.of(PluginClassLoader.class, "getClasses")
                        .call(pcl, new Object[0]);
                if (classes.contains(dClass)) {
                    System.out.println("SET LOADER "+p.getName()+" TO LOAD " + dClass);
                    loader = pcl;
                    break;
                }
                //System.out.println("   - CLASSES COUNT: " + classes.size());
            } else {
                classes = Collections.emptyList();
            }
            */
            /*
            List.of(dClass, tClass).forEach(vv -> {
                //System.out.println("   - HAS CLASS "+vv+": " + classes.contains(vv));
                String name = vv.getName();
                try {
                    Class<?> loaded = pcl.loadClass(name, true, false, true);
                    System.out.println("   - CLASS "+name+" LOADED: " + loaded);
                } catch (Throwable e) {
                    System.out.println("   - CLASS "+name+" NOT LOADED: " + e.getMessage());
                }
            });
            */
        }
        loader = new CombinedClassLoader(dClass.getClassLoader(), tClass.getClassLoader());
        System.out.println("   LOADER: " + loader);

        //dClass.getClassLoader().loadClass()
        //ClassLoader oldLoader = dClass.getClassLoader();
        //TestNative.replaceLoader(dClass, loader);
        TestNative.addOpensToAllUnnamed(dClass);
        try {
            return TestNative.defineHiddenClass(lookup, loader, bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE)
                    .lookupClass();
        } finally {
            //TestNative.replaceLoader(dClass, oldLoader);
        }
    }

    @Override
    public <T, J extends Executable> T createExecutable(J executable, Class<T> tClass, Method invoke) {
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

            String invokeName = invoke.getName();

            Class<?> returnType;
            boolean isMethod;
            String memberDescriptor;
            boolean isStatic = Modifier.isStatic(executable.getModifiers());

            if (executable instanceof Method method) {
                returnType = method.getReturnType();
                isMethod = true;
                memberDescriptor = org.objectweb.asm.Type.getMethodDescriptor(method);
            }
            else if (executable instanceof Constructor<?> constructor) {
                returnType = constructor.getDeclaringClass();
                isMethod = false;
                memberDescriptor = org.objectweb.asm.Type.getConstructorDescriptor(constructor);
                isStatic = true;
            }
            else throw new IllegalArgumentException("Unsupported member type: " + executable.getClass());

            List<Class<?>> argsTypes = Arrays.stream(executable.getParameters())
                    .map(Parameter::getType)
                    .collect(Collectors.toList());

            if (!isStatic)
                argsTypes.addFirst(executable.getDeclaringClass());

            SignatureWriter sw = new SignatureWriter();
            sw.visitSuperclass();
            sw.visitClassType(org.objectweb.asm.Type.getInternalName(Object.class));
            sw.visitEnd();

            sw.visitInterface();
            sw.visitClassType(org.objectweb.asm.Type.getInternalName(tClass));
            for (Class<?> argType : argsTypes) {
                sw.visitTypeArgument('=')
                        .visitClassType(org.objectweb.asm.Type.getInternalName(Primitives.wrap(argType)));
                sw.visitEnd();
            }
            if (returnType != void.class) {
                sw.visitTypeArgument('=')
                        .visitClassType(org.objectweb.asm.Type.getInternalName(Primitives.wrap(returnType)));
                sw.visitEnd();
            }
            sw.visitEnd();

            String signature = sw.toString();

            String uid = UUID.randomUUID().toString().replace("-", "");
            Class<?> dClass = executable.getDeclaringClass();
            String outerInternalName = org.objectweb.asm.Type.getInternalName(dClass);
            String innerSimpleName = "Impl_" + uid;
            String innerInternalName = outerInternalName + "$" + innerSimpleName;

            cw.visit(Opcodes.V23, Opcodes.ACC_PRIVATE | Opcodes.ACC_SUPER | Opcodes.ACC_STATIC,
                    innerInternalName, signature,
                    org.objectweb.asm.Type.getInternalName(Object.class), new String[]{ org.objectweb.asm.Type.getInternalName(tClass) });

            cw.visitOuterClass(outerInternalName, null, null);
            cw.visitInnerClass(innerInternalName, outerInternalName, innerSimpleName,
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);


            {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                        org.objectweb.asm.Type.getMethodDescriptor(org.objectweb.asm.Type.VOID_TYPE), null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);

                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, org.objectweb.asm.Type.getInternalName(Object.class),
                        "<init>", org.objectweb.asm.Type.getMethodDescriptor(org.objectweb.asm.Type.VOID_TYPE), false);

                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            String invokeDescriptor = org.objectweb.asm.Type.getMethodDescriptor(
                    returnType == void.class ? org.objectweb.asm.Type.VOID_TYPE : org.objectweb.asm.Type.getType(Primitives.wrap(returnType)),
                    argsTypes.stream()
                            .map(Primitives::wrap)
                            .map(org.objectweb.asm.Type::getType)
                            .toArray(org.objectweb.asm.Type[]::new));

            {
                MethodVisitor mv;
                mv = cw.visitMethod(Opcodes.ACC_PUBLIC, invokeName,
                        invokeDescriptor,
                        null, null);

                mv.visitCode();

                if (!isMethod) {
                    mv.visitTypeInsn(Opcodes.NEW, outerInternalName);
                    mv.visitInsn(Opcodes.DUP);
                }

                Tuple1<Integer> varIndex = Tuple.of(1);
                for (Class<?> argType : argsTypes) {
                    mv.visitVarInsn(Opcodes.ALOAD, varIndex.val0);
                    visitUnboxingType(mv, argType);
                    varIndex.val0++;
                }

                if (isMethod) {
                    String methodName = executable.getName();
                    mv.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : dClass.isInterface() ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
                            outerInternalName, methodName,
                            memberDescriptor, dClass.isInterface());
                    if (returnType != void.class) {
                        visitBoxingType(mv, returnType);
                        mv.visitInsn(Opcodes.ARETURN);
                    } else {
                        mv.visitInsn(Opcodes.RETURN);
                    }
                } else {
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, outerInternalName, "<init>", memberDescriptor, false);
                    mv.visitInsn(Opcodes.ARETURN);
                }

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            {
                org.objectweb.asm.Type[] argsObjTypes = new org.objectweb.asm.Type[argsTypes.size()];
                Arrays.fill(argsObjTypes, org.objectweb.asm.Type.getType(Object.class));
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, invokeName,
                        org.objectweb.asm.Type.getMethodDescriptor(returnType == void.class ? org.objectweb.asm.Type.VOID_TYPE : org.objectweb.asm.Type.getType(Object.class), argsObjTypes), null, null);

                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);

                Tuple1<Integer> varIndex = Tuple.of(1);
                for (Class<?> argType : argsTypes) {
                    mv.visitVarInsn(Opcodes.ALOAD, varIndex.val0);
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Primitives.wrap(argType)));
                    varIndex.val0++;
                }
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, innerInternalName, invokeName, invokeDescriptor, false);
                if (returnType != void.class) {
                    mv.visitInsn(Opcodes.ARETURN);
                } else {
                    mv.visitInsn(Opcodes.RETURN);
                }

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            cw.visitEnd();
            byte[] bytes = cw.toByteArray();
            /*
            try {
            */
            /*
            var v = TestNative.allowedModes(MethodHandles.privateLookupIn(dClass, TestNative.lookup(dClass)));
            System.out.println("Lookup: " + v.lookupClass());
            System.out.println("Lookup loader: " + v.lookupClass().getClassLoader());
            Class<?> newClass = v
                    .defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE)
                    .lookupClass();
            */
            Class<?> newClass = defineClass(dClass, tClass, bytes);
            return (T) Reflection.constructor(newClass).newInstance();
            /*
            } catch (Throwable e) {
                try {
                    Files.write(Path.of("build", "tmp", "tmp.class").toAbsolutePath(), bytes);
                } catch (Throwable _) { }
                throw e;
            }
            */
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public <T> T createField(Field field, boolean isGetter, Class<T> tClass, Method invoke) {
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

            String invokeName = invoke.getName();

            Class<?> returnType;
            String memberDescriptor = Type.getDescriptor(field.getType());
            boolean isStatic = Modifier.isStatic(field.getModifiers());

            List<Class<?>> argsTypes = new ArrayList<>();

            if (isGetter) {
                returnType = field.getType();
            } else {
                returnType = void.class;
                argsTypes.add(field.getType());
            }

            if (!isStatic)
                argsTypes.addFirst(field.getDeclaringClass());

            SignatureWriter sw = new SignatureWriter();
            sw.visitSuperclass();
            sw.visitClassType(Type.getInternalName(Object.class));
            sw.visitEnd();

            sw.visitInterface();
            sw.visitClassType(Type.getInternalName(tClass));
            for (Class<?> argType : argsTypes) {
                sw.visitTypeArgument('=')
                        .visitClassType(Type.getInternalName(Primitives.wrap(argType)));
                sw.visitEnd();
            }
            if (returnType != void.class) {
                sw.visitTypeArgument('=')
                        .visitClassType(Type.getInternalName(Primitives.wrap(returnType)));
                sw.visitEnd();
            }
            sw.visitEnd();

            String signature = sw.toString();

            String uid = UUID.randomUUID().toString().replace("-", "");
            Class<?> dClass = field.getDeclaringClass();
            String outerInternalName = Type.getInternalName(dClass);
            String innerSimpleName = "Impl_" + uid;
            String innerInternalName = outerInternalName + "$" + innerSimpleName;

            cw.visit(Opcodes.V23, Opcodes.ACC_PRIVATE | Opcodes.ACC_SUPER | Opcodes.ACC_STATIC,
                    innerInternalName, signature,
                    Type.getInternalName(Object.class), new String[]{ Type.getInternalName(tClass) });

            cw.visitOuterClass(outerInternalName, null, null);
            cw.visitInnerClass(innerInternalName, outerInternalName, innerSimpleName,
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);

            {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                        Type.getMethodDescriptor(Type.VOID_TYPE), null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);

                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class),
                        "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);

                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            String invokeDescriptor = Type.getMethodDescriptor(
                    returnType == void.class ? Type.VOID_TYPE : Type.getType(Primitives.wrap(returnType)),
                    argsTypes.stream()
                            .map(Primitives::wrap)
                            .map(Type::getType)
                            .toArray(Type[]::new));

            {
                MethodVisitor mv;
                mv = cw.visitMethod(Opcodes.ACC_PUBLIC, invokeName,
                        invokeDescriptor,
                        null, null);

                mv.visitCode();

                Tuple1<Integer> varIndex = Tuple.of(1);
                for (Class<?> argType : argsTypes) {
                    mv.visitVarInsn(Opcodes.ALOAD, varIndex.val0);
                    visitUnboxingType(mv, argType);
                    varIndex.val0++;
                }
                String fieldName = field.getName();
                mv.visitFieldInsn(isGetter
                                ? (isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD)
                                : (isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD),
                        outerInternalName, fieldName,
                        memberDescriptor);
                if (returnType != void.class) {
                    visitBoxingType(mv, returnType);
                    mv.visitInsn(Opcodes.ARETURN);
                } else {
                    mv.visitInsn(Opcodes.RETURN);
                }

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            {
                Type[] argsObjTypes = new Type[argsTypes.size()];
                Arrays.fill(argsObjTypes, Type.getType(Object.class));
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, invokeName,
                        Type.getMethodDescriptor(returnType == void.class ? Type.VOID_TYPE : Type.getType(Object.class), argsObjTypes), null, null);

                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);

                Tuple1<Integer> varIndex = Tuple.of(1);
                for (Class<?> argType : argsTypes) {
                    mv.visitVarInsn(Opcodes.ALOAD, varIndex.val0);
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Primitives.wrap(argType)));
                    varIndex.val0++;
                }
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, innerInternalName, invokeName, invokeDescriptor, false);
                if (returnType != void.class) {
                    mv.visitInsn(Opcodes.ARETURN);
                } else {
                    mv.visitInsn(Opcodes.RETURN);
                }

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            cw.visitEnd();
            byte[] bytes = cw.toByteArray();
            /*
            try {
            */
            Class<?> newClass = MethodHandles.privateLookupIn(dClass, MethodHandles.lookup())
                    .defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE)
                    .lookupClass();
            return (T) Reflection.constructor(newClass).newInstance();
            /*
            } catch (Throwable e) {
                try {
                    Files.write(Path.of("build", "tmp", "tmp.class").toAbsolutePath(), bytes);
                } catch (Throwable _) { }
                throw e;
            }
            */
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
