package patch;

import org.lime.system.execute.Func2;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.util.List;

public interface JarArchiveBase {
    String name();

    <T>ClassPatcher<T> of(Class<T> tClass);

    <T> JarArchiveBase patchMethod(IMethodFilter<T> filter, MethodPatcher patcher);
    <T> JarArchiveBase patchMethod(List<IMethodFilter<T>> filters, MethodPatcher patcher);
    <T> JarArchiveBase patch(Class<T> tClass, Func2<JarArchive, ClassWriter, ClassVisitor> visitor, List<PatchException> exceptions);
}
