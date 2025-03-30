package patch;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.papermc.paper.util.MappingEnvironment;
import net.neoforged.srgutils.IMappingFile;
import org.jetbrains.annotations.NotNull;
import org.lime.json.builder.Json;
import org.lime.reflection.LambdaInfo;
import org.lime.system.execute.*;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.util.*;

public class Native {
    static @NotNull Action1<String> logger = System.out::println;

    public static String classFile(Class<?> tClass) { return className(tClass) + ".class"; }
    public static String className(Class<?> tClass) { return tClass.getName().replace('.', '/'); }

    private static @Nullable String prefix = null;
    public static void log(String log) {
        logger.invoke(prefix == null ? log : (prefix + log));
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
        SerializedLambda lambda = LambdaInfo.infoFromLambda(callable);
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
        LambdaInfo.MemberInfo info = LambdaInfo.infoFromFieldLambda(callable);
        field.invoke(opcode, info.owner(), info.name(), info.descriptor());
    }

    public static <T>T getMethod(ICallable callable, Func5<Integer, String, String, String, Boolean, T> method) {
        SerializedLambda lambda = LambdaInfo.infoFromLambda(callable);
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
        LambdaInfo.MemberInfo info = LambdaInfo.infoFromFieldLambda(callable);
        return field.invoke(opcode, info.owner(), info.name(), info.descriptor());
    }

    public static boolean isMethod(ICallable callable, String owner, String name, String descriptor) {
        LambdaInfo.MemberInfo method = LambdaInfo.MemberInfo.of(LambdaInfo.infoFromLambda(callable));
        if (!method.name().equals(name)
                || !Type.getType(method.descriptor()).equals(Type.getType(descriptor)))
            return false;
        if (method.owner().equals(owner))
            return true;
        return LambdaInfo.getMethod(owner, name, descriptor).equals(LambdaInfo.getMethod(method));
    }
    public static boolean isField(ICallable callable, String owner, String name, String descriptor) {
        LambdaInfo.MemberInfo field = LambdaInfo.infoFromFieldLambda(callable);
        return field.owner().equals(owner)
                && field.name().equals(name)
                && Type.getType(field.descriptor()).equals(Type.getType(descriptor));
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
