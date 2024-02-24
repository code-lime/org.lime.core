package patch;

public interface IMethodInfo {
    String STATIC_CONSTRUCTOR = "<clinit>";
    String OBJECT_CONSTRUCTOR = "<init>";

    String toInfo();

    static IMethodInfo raw(String className, String methodName, String methodDescriptor) {
        return () -> className + "." + methodName + methodDescriptor;
    }
    static IMethodInfo raw(Class<?> classType, String methodName, String methodDescriptor) {
        return () -> classType.getSimpleName() + "." + methodName + methodDescriptor;
    }
}
