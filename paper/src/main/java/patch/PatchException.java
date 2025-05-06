package patch;

public class PatchException extends RuntimeException {
    public PatchException(String message, JarArchiveBase jar, String className) {
        super(message + " (" + jar.name() + "::" + className + ")");
    }
    public PatchException(String message, JarArchiveBase jar, MethodInfo method) {
        super(message + " (" + jar.name() + "::" + method.toInfo() + ")");
    }
}
