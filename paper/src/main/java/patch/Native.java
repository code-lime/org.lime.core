package patch;

import org.jetbrains.annotations.NotNull;
import org.lime.core.common.reflection.LambdaInfo;
import org.lime.core.common.utils.system.execute.*;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;

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

    public static void writeMethod(Callable callable, Action5<Integer, String, String, String, Boolean> method) {
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
    public static void writeField(int opcode, Callable callable, Action4<Integer, String, String, String> field) {
        LambdaInfo.MemberInfo info = LambdaInfo.infoFromFieldLambda(callable);
        field.invoke(opcode, info.owner(), info.name(), info.descriptor());
    }

    public static <T>T getMethod(Callable callable, Func5<Integer, String, String, String, Boolean, T> method) {
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
    public static <T>T getField(int opcode, Callable callable, Func4<Integer, String, String, String, T> field) {
        LambdaInfo.MemberInfo info = LambdaInfo.infoFromFieldLambda(callable);
        return field.invoke(opcode, info.owner(), info.name(), info.descriptor());
    }

    public static boolean isMethod(Callable callable, String owner, String name, String descriptor) {
        LambdaInfo.MemberInfo method = LambdaInfo.MemberInfo.of(LambdaInfo.infoFromLambda(callable));
        if (!method.name().equals(name)
                || !Type.getType(method.descriptor()).equals(Type.getType(descriptor)))
            return false;
        if (method.owner().equals(owner))
            return true;
        return LambdaInfo.getMethod(owner, name, descriptor).equals(LambdaInfo.getMethod(method));
    }
    public static boolean isField(Callable callable, String owner, String name, String descriptor) {
        LambdaInfo.MemberInfo field = LambdaInfo.infoFromFieldLambda(callable);
        return field.owner().equals(owner)
                && field.name().equals(name)
                && Type.getType(field.descriptor()).equals(Type.getType(descriptor));
    }
}
