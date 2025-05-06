package patch;

public interface MethodInfo {
    String STATIC_CONSTRUCTOR = "<clinit>";
    String OBJECT_CONSTRUCTOR = "<init>";

    String toInfo();

    static MethodInfo raw(String className, String methodName, String methodDescriptor) {
        return () -> className + "." + methodName + methodDescriptor;
    }
    static MethodInfo raw(Class<?> classType, String methodName, String methodDescriptor) {
        return () -> classType.getSimpleName() + "." + methodName + methodDescriptor;
    }
}
