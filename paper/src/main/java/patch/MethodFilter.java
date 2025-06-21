package patch;

import net.minecraft.world.entity.player.Player;
import org.lime.core.common.UnsafeMappings;
import org.lime.core.common.reflection.LambdaInfo;
import org.lime.core.common.system.execute.Execute;
import org.lime.core.common.system.execute.Callable;
import org.lime.core.paper.PaperUnsafeMappings;
import org.objectweb.asm.Type;

import java.lang.invoke.SerializedLambda;

public interface MethodFilter<T> extends MethodInfo {
    Class<T> tClass();
    boolean test(int access, String name, String descriptor, String signature, String[] exceptions);
    String toInfo();

    static <T> MethodFilter<T> of(Callable callable) {
        SerializedLambda lambda = LambdaInfo.infoFromLambda(callable);
        Class<T> methodClass = Execute.funcEx(() -> (Class<T>)Class.forName(lambda.getImplClass().replace('/','.'))).throwable().invoke();
        String methodName = lambda.getImplMethodName();
        String methodDescriptor = lambda.getImplMethodSignature();
        return new MethodFilter<T>() {
            @Override public Class<T> tClass() { return methodClass; }
            @Override public boolean test(int access, String name, String descriptor, String signature, String[] exceptions) {
                return name.equals(methodName) && descriptor.equals(methodDescriptor);
            }
            @Override public String toInfo() { return methodClass.getSimpleName() + "." + methodName + methodDescriptor; }
        };
    }
    static <T> MethodFilter<T> of(Class<T> tClass, String methodName, Type methodDescriptor, boolean isMojang) {
        return new MethodFilter<T>() {
            @Override public Class<T> tClass() { return tClass; }
            @Override public boolean test(int access, String name, String descriptor, String signature, String[] exceptions) {
                String resultName = isMojang ? PaperUnsafeMappings.INSTANCE.ofMojang(tClass, name, descriptor, true) : name;
                return resultName.equals(methodName) && Type.getType(descriptor).equals(methodDescriptor);
            }
            @Override public String toInfo() { return tClass.getSimpleName() + "." + methodName + methodDescriptor; }
        };
    }
    static <T> MethodFilter<T> of(Class<T> tClass, String methodName, boolean isMojang) {
        return new MethodFilter<T>() {
            @Override public Class<T> tClass() { return tClass; }
            @Override public boolean test(int access, String name, String descriptor, String signature, String[] exceptions) {
                String resultName = isMojang ? PaperUnsafeMappings.INSTANCE.ofMojang(tClass, name, descriptor, true) : name;
                return resultName.equals(methodName);
            }
            @Override public String toInfo() { return tClass.getSimpleName() + "." + methodName + "(*)*"; }
        };
    }
}
















