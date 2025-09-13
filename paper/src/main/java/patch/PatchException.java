package patch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PatchException extends RuntimeException {
    public final List<String> additional = new ArrayList<>();

    public PatchException(String message, JarArchiveBase jar, String className) {
        super(message + " (" + jar.name() + "::" + className + ")");
    }
    public PatchException(String message, JarArchiveBase jar, MethodInfo method) {
        super(message + " (" + jar.name() + "::" + method.toInfo() + ")");
    }

    @Override
    public String getMessage() {
        if (additional.isEmpty())
            return super.getMessage();
        return super.getMessage() + "\n" + String.join("\n", additional);
    }

    public PatchException addAdditional(Collection<String> additional) {
        this.additional.addAll(additional);
        return this;
    }
}
