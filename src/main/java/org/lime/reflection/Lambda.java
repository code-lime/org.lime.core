package org.lime.reflection;

import com.google.common.primitives.Primitives;
import org.lime.system.execute.Execute;
import org.lime.system.execute.ICallable;
import org.lime.system.tuple.Tuple;
import org.lime.system.tuple.Tuple1;
import org.lime.system.tuple.Tuple3;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureWriter;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Lambda {
    private static final ConcurrentHashMap<Tuple3<Class<?>, Member, Object>, Object> cachedCallable = new ConcurrentHashMap<>();

    private static final String INVOKE_NAME_EXECUTE = "invoke";

    private static class DynamicClassLoader extends ClassLoader {
        public DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    private static int getExecuteCount(Executable executable) {
        return executable.getParameterCount() + (Modifier.isStatic(executable.getModifiers()) ? 0 : 1);
    }

    public static ICallable lambda(Method method) {
        return lambda(method, Execute.findClass(getExecuteCount(method), method.getReturnType() != void.class, false));
    }
    public static <T extends ICallable>T lambda(Method method, Class<T> tClass) {
        return lambda(method, tClass, INVOKE_NAME_EXECUTE);
    }
    public static <T>T lambda(Method method, Class<T> tClass, String invokeName) {
        return createLambda(method, tClass, invokeName);
    }

    public static ICallable lambda(Constructor<?> constructor) {
        return lambda(constructor, Execute.findClass(constructor.getParameterCount(), true, false), INVOKE_NAME_EXECUTE);
    }
    public static <T extends ICallable>T lambda(Constructor<?> constructor, Class<T> tClass) {
        return lambda(constructor, tClass, INVOKE_NAME_EXECUTE);
    }
    public static <T>T lambda(Constructor<?> constructor, Class<T> tClass, String invokeName) {
        return createLambda(constructor, tClass, invokeName);
    }

    public static ICallable getter(Field field) {
        return getter(field, Execute.findClass(Modifier.isStatic(field.getModifiers()) ? 0 : 1, true, false), INVOKE_NAME_EXECUTE);
    }
    public static <T extends ICallable>T getter(Field field, Class<T> tClass) {
        return getter(field, tClass, INVOKE_NAME_EXECUTE);
    }
    public static <T>T getter(Field field, Class<T> tClass, String invokeName) {
        return createField(field, true, tClass, invokeName);
    }

    public static ICallable setter(Field field) {
        return setter(field, Execute.findClass(1 + (Modifier.isStatic(field.getModifiers()) ? 0 : 1), false, false), INVOKE_NAME_EXECUTE);
    }
    public static <T extends ICallable>T setter(Field field, Class<T> tClass) {
        return setter(field, tClass, INVOKE_NAME_EXECUTE);
    }
    public static <T>T setter(Field field, Class<T> tClass, String invokeName) {
        return createField(field, false, tClass, invokeName);
    }

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

    private static <T, J extends Executable>T createLambda(
            J executable,
            Class<T> tClass,
            String invokeName) {
        return (T) cachedCallable.computeIfAbsent(Tuple.of(tClass, executable, null), _ -> createExecutableLambda(executable, tClass, invokeName));
    }
    private static <T, J extends Executable>T createExecutableLambda(
            J executable,
            Class<T> tClass,
            String invokeName) {
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

            Class<?> returnType;
            boolean isMethod;
            String memberDescriptor;
            boolean isStatic = Modifier.isStatic(executable.getModifiers());

            if (executable instanceof Method method) {
                returnType = method.getReturnType();
                isMethod = true;
                memberDescriptor = Type.getMethodDescriptor(method);
            }
            else if (executable instanceof Constructor constructor) {
                returnType = constructor.getDeclaringClass();
                isMethod = false;
                memberDescriptor = Type.getConstructorDescriptor(constructor);
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
            Class<?> dClass = executable.getDeclaringClass();
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
    private static <T>T createField(
            Field field,
            boolean isGetter,
            Class<T> tClass,
            String invokeName) {
        return (T) cachedCallable.computeIfAbsent(Tuple.of(tClass, field, isGetter), _ -> createFieldLambda(field, isGetter, tClass, invokeName));
    }
    private static <T>T createFieldLambda(
            Field field,
            boolean isGetter,
            Class<T> tClass,
            String invokeName) {
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

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
