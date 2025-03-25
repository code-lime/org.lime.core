package org.lime.reflection;

import org.lime.system.execute.Execute;
import org.lime.system.execute.Func1;
import org.lime.system.tuple.Tuple;
import org.lime.system.tuple.Tuple1;
import org.objectweb.asm.*;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LambdaInfo {
    public record MemberInfo(String owner, String name, String descriptor) {
        public static MemberInfo of(SerializedLambda lambda) {
            return new MemberInfo(lambda.getImplClass(), lambda.getImplMethodName(), lambda.getImplMethodSignature());
        }
    }

    private static final ConcurrentHashMap<String, Class<?>> classes = new ConcurrentHashMap<>();
    private static final Func1<String, Class<?>> classReader = Execute.<String, Class<?>>funcEx(Class::forName).throwable();

    public static Method getMethod(String owner, String name, String descriptor) {
        Class<?> tClass = classes.computeIfAbsent(owner.replace('/', '.'), classReader);
        for (Method method : tClass.getMethods()) {
            if (method.getName().equals(name) && Type.getMethodDescriptor(method).equals(descriptor))
                return method;
        }
        throw new RuntimeException("Method " + owner + "." + name + descriptor + " not founded");
    }
    public static Method getMethod(MemberInfo info) {
        return getMethod(info.owner(), info.name(), info.descriptor());
    }
    public static Method getMethod(SerializedLambda lambda) {
        return getMethod(MemberInfo.of(lambda));
    }
    public static Method getMethod(Serializable lambda) {
        return getMethod(infoFromLambda(lambda));
    }

    public static Field getField(String owner, String name, String descriptor) {
        Class<?> tClass = classes.computeIfAbsent(owner.replace('/', '.'), classReader);
        for (Field field : tClass.getFields()) {
            if (field.getName().equals(name) && Type.getDescriptor(field.getType()).equals(descriptor))
                return field;
        }
        throw new RuntimeException("Field " + descriptor + " " + owner + "." + name + " not founded");
    }
    public static Field getField(MemberInfo info) {
        return getField(info.owner(), info.name(), info.descriptor());
    }
    public static Field getField(SerializedLambda lambda) {
        return getField(infoFromFieldLambda(lambda));
    }
    public static Field getField(Serializable lambda) {
        return getField(infoFromLambda(lambda));
    }

    private static final ConcurrentHashMap<Class<?>, Func1<Serializable, SerializedLambda>> classReaders = new ConcurrentHashMap<>();
    public static SerializedLambda infoFromLambda(Serializable lambda) {
        Func1<Serializable, SerializedLambda> func = classReaders.computeIfAbsent(lambda.getClass(), v -> ReflectionMethod.of(v, "writeReplace").lambda(Func1.class));
        return func.invoke(lambda);
    }
    public static MemberInfo infoFromMethodLambda(Serializable lambda) {
        return MemberInfo.of(infoFromLambda(lambda));
    }
    private static final ConcurrentHashMap<MemberInfo, MemberInfo> lambdaToFieldMembers = new ConcurrentHashMap<>();
    public static MemberInfo infoFromFieldLambda(SerializedLambda lambda) {
        MemberInfo lambdaMember = MemberInfo.of(lambda);
        return lambdaToFieldMembers.computeIfAbsent(lambdaMember, v -> {
            try {
                String classFile = v.owner();
                String methodOwner = classFile.replace('/', '.');
                Class<?> tClass = Class.forName(methodOwner);

                String methodName = v.name();
                String methodDescriptor = v.descriptor();

                try (InputStream stream = Objects.requireNonNull(tClass.getClassLoader().getResourceAsStream(classFile + ".class"))) {
                    ClassReader cr = new ClassReader(stream);
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    Tuple1<MemberInfo> fieldInfo = Tuple.of(null);
                    cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (name.equals(methodName) && descriptor.equals(methodDescriptor)) {
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                    @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                        fieldInfo.val0 = new MemberInfo(owner, name, descriptor);
                                        super.visitFieldInsn(opcode, owner, name, descriptor);
                                    }
                                };
                            }
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                        }
                    }, 0);
                    return Objects.requireNonNull(fieldInfo.val0);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }
    public static MemberInfo infoFromFieldLambda(Serializable lambda) {
        return infoFromFieldLambda(infoFromLambda(lambda));
    }
}
