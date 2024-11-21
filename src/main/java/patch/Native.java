package patch;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.papermc.paper.util.MappingEnvironment;
import net.neoforged.srgutils.IMappingFile;
import org.lime.json.builder.Json;
import org.lime.system.tuple.*;
import org.lime.system.execute.*;
import org.objectweb.asm.*;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.*;

public class Native {
    public static String sha256(byte[] bytes) throws Throwable {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    public static String classFile(Class<?> tClass) { return className(tClass) + ".class"; }
    public static String className(Class<?> tClass) { return tClass.getName().replace('.', '/'); }

    private static @Nullable String prefix = null;
    public static void log(String log) {
        System.out.println(prefix == null ? log : (prefix + log));
    }
    public interface ICloseable extends Closeable {  @Override void close(); }
    public interface IAction { void execute(); }
    public static ICloseable subLog() {
        String oldPrefix = prefix;
        prefix = prefix == null ? " - " : ("   " + prefix);
        return () -> prefix = oldPrefix;
    }
    public static void subLog(IAction subLogger) {
        try (var ignored = subLog()) { subLogger.execute(); }
    }
    public static void subLog(String log, IAction subLogger) {
        log(log);
        subLog(subLogger);
    }

    public static void writeMethod(ICallable callable, Action5<Integer, String, String, String, Boolean> method) {
        SerializedLambda lambda = infoFromLambda(callable);
        String kind = MethodHandleInfo.referenceKindToString(lambda.getImplMethodKind());
        int opcode = switch (kind) {
            case "invokeVirtual" -> Opcodes.INVOKEVIRTUAL;
            case "invokeStatic" -> Opcodes.INVOKESTATIC;
            case "invokeSpecial", "newInvokeSpecial" -> Opcodes.INVOKESPECIAL;
            case "invokeInterface" -> Opcodes.INVOKEINTERFACE;
            case "getField", "getStatic", "putField", "putStatic" -> throw new IllegalArgumentException("Kind method type '"+kind+"' can be only invokable!");
            default -> throw new IllegalArgumentException("Kind method type '"+kind+"' not supported!");
        };
        boolean isInterface = opcode == Opcodes.INVOKEINTERFACE
                || Execute.funcEx(() -> Class.forName(lambda.getImplClass().replace('/', '.')).isInterface())
                    .optional().invoke().orElse(false);
        method.invoke(opcode, lambda.getImplClass(), lambda.getImplMethodName(), lambda.getImplMethodSignature(), isInterface);
    }
    public static void writeField(int opcode, ICallable callable, Action4<Integer, String, String, String> field) {
        FieldInfo info = infoFromField(callable);
        field.invoke(opcode, info.owner(), info.name(), info.descriptor());
    }

    public static <T>T getMethod(ICallable callable, Func5<Integer, String, String, String, Boolean, T> method) {
        SerializedLambda lambda = infoFromLambda(callable);
        String kind = MethodHandleInfo.referenceKindToString(lambda.getImplMethodKind());
        int opcode = switch (kind) {
            case "invokeVirtual" -> Opcodes.INVOKEVIRTUAL;
            case "invokeStatic" -> Opcodes.INVOKESTATIC;
            case "invokeSpecial", "newInvokeSpecial" -> Opcodes.INVOKESPECIAL;
            case "invokeInterface" -> Opcodes.INVOKEINTERFACE;
            case "getField", "getStatic", "putField", "putStatic" -> throw new IllegalArgumentException("Kind method type '"+kind+"' can be only invokable!");
            default -> throw new IllegalArgumentException("Kind method type '"+kind+"' not supported!");
        };
        boolean isInterface = opcode == Opcodes.INVOKEINTERFACE
                || Execute.funcEx(() -> Class.forName(lambda.getImplClass().replace('/', '.')).isInterface())
                .optional().invoke().orElse(false);
        return method.invoke(opcode, lambda.getImplClass(), lambda.getImplMethodName(), lambda.getImplMethodSignature(), isInterface);
    }
    public static <T>T getField(int opcode, ICallable callable, Func4<Integer, String, String, String, T> field) {
        FieldInfo info = infoFromField(callable);
        return field.invoke(opcode, info.owner(), info.name(), info.descriptor());
    }

    public static Method getMethod(String owner, String name, String descriptor) {
        try {
            Class<?> _owner = Class.forName(owner.replace('/', '.'));
            for (Method method : _owner.getMethods()) {
                if (method.getName().equals(name) && Type.getMethodDescriptor(method).equals(descriptor))
                    return method;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        throw new RuntimeException("Method " + owner + "." + name + descriptor + " not founded");
    }
    public static Method getMethod(SerializedLambda lambda) {
        return getMethod(lambda.getImplClass(), lambda.getImplMethodName(), lambda.getImplMethodSignature());
    }

    public static boolean isMethod(ICallable callable, String owner, String name, String descriptor) {
        SerializedLambda lambda = infoFromLambda(callable);
        if (!lambda.getImplMethodName().equals(name) || !Type.getType(lambda.getImplMethodSignature()).equals(Type.getType(descriptor))) return false;
        if (lambda.getImplClass().equals(owner)) return true;
        return getMethod(owner, name, descriptor).equals(getMethod(lambda));
    }
    public static boolean isField(ICallable callable, String owner, String name, String descriptor) {
        FieldInfo field = infoFromField(callable);
        return field.owner().equals(owner)
                && field.name().equals(name)
                && Type.getType(field.descriptor()).equals(Type.getType(descriptor));
    }

    public static SerializedLambda infoFromLambda(Serializable lambda) {
        try {
            Method m = lambda.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);
            return (SerializedLambda)m.invoke(lambda);
        } catch(ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
    public record FieldInfo(String owner, String name, String descriptor) { }
    public static FieldInfo infoFromField(ICallable callable) {
        try {
            SerializedLambda lambda = infoFromLambda(callable);
            String classFile = lambda.getImplClass();
            String methodOwner = classFile.replace('/', '.');
            Class<?> tClass = Class.forName(methodOwner);

            String methodName = lambda.getImplMethodName();
            String methodDescriptor = lambda.getImplMethodSignature();

            //Native.log("Lambda method: " + methodOwner + "." + methodName + methodDescriptor);
            try (InputStream stream = Objects.requireNonNull(tClass.getClassLoader().getResourceAsStream(classFile + ".class"))) {
                ClassReader cr = new ClassReader(stream);
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                Tuple1<FieldInfo> fieldInfo = Tuple.of(null);
                cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                    @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        //Native.log("Compare method with: " + methodOwner.replace('/', '.') + "." + name + descriptor);
                        if (name.equals(methodName) && descriptor.equals(methodDescriptor)) {
                            //Native.log("Read method fileds");
                            return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                                @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                    fieldInfo.val0 = new FieldInfo(owner, name, descriptor);
                                    //Native.log("Field: " + fieldInfo.val0);
                                    super.visitFieldInsn(opcode, owner, name, descriptor);
                                }
                            };
                        }
                        return super.visitMethod(access, name, descriptor, signature, exceptions);
                    }
                }, 0);
                //Native.log(" - Field: " + fieldInfo.val0);
                return Objects.requireNonNull(fieldInfo.val0);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private record MappingInfo(String name, String description, boolean isMethod) { }
    public static Closeable loadDeobf() throws Throwable {
        InputStream mappingsInputStream = MappingEnvironment.mappingsStreamIfPresent();
        if (mappingsInputStream == null) return () -> {};
        IMappingFile tree = IMappingFile.load(mappingsInputStream);
        for (IMappingFile.IClass classMapping : tree.getClasses()) {
            BiMap<MappingInfo, MappingInfo> members = HashBiMap.create();
            /*MOJANG+YARN - ORIGINAL*/
            /*SPIGOT - MAPPED*/
            for (IMappingFile.IMethod member : classMapping.getMethods())
                members.put(
                        new MappingInfo(member.getOriginal(), member.getMappedDescriptor(), true),
                        new MappingInfo(member.getMapped(), member.getMappedDescriptor(), true)
                );

            for (IMappingFile.IField member : classMapping.getFields())
                members.put(
                        new MappingInfo(member.getOriginal(), member.getMappedDescriptor(), false),
                        new MappingInfo(member.getMapped(), member.getMappedDescriptor(), false)
                );

            classesSpigotToMojang.put(classMapping.getMapped().replace('/', '.'), members);
        }
        return mappingsInputStream;
    }
    private static final Map<String, BiMap<MappingInfo, MappingInfo>> classesSpigotToMojang = new HashMap<>();
    private static final List<Class<?>> dat = new ArrayList<>();
    private static String getRawName(Class<?> tClass, String name, String desc, boolean isMethod, boolean getSpigotName) {
        BiMap<MappingInfo, MappingInfo> mapping = classesSpigotToMojang.get(tClass.getName());
        if (!getSpigotName) mapping = mapping.inverse();
        if (mapping == null) return name;
        if (dat.contains(tClass)) {
            log("Class " + tClass.getName() + " with found " + (isMethod ? "method" : "field") + " " + name + desc + "\n" + Json.object().add(mapping, Record::toString, v -> v).build().toString());
            dat.add(tClass);
        }
        MappingInfo src_info = mapping.get(new MappingInfo(name, desc, isMethod));
        return src_info == null ? name : src_info.name();
    }
    private static String getRawName(Class<?> tClass, String name, Type desc, boolean isMethod, boolean getSpigotName) {
        return getRawName(tClass, name, desc.getDescriptor(), isMethod, getSpigotName);
    }
    public static String getMojangName(Class<?> tClass, String name, String desc, boolean isMethod) {
        return getRawName(tClass, name, desc, isMethod, false);
    }
    public static String getMojangName(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return getRawName(tClass, name, desc, isMethod, false);
    }
    public static String getSpigotName(Class<?> tClass, String name, String desc, boolean isMethod) {
        return getRawName(tClass, name, desc, isMethod, true);
    }
    public static String getSpigotName(Class<?> tClass, String name, Type desc, boolean isMethod) {
        return getRawName(tClass, name, desc, isMethod, true);
    }
}
