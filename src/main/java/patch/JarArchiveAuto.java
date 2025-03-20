package patch;

import org.lime.system.execute.Func2;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.util.List;

public class JarArchiveAuto implements JarArchiveBase {
    private final List<JarArchive> archives;

    public JarArchiveAuto(JarArchive... archives) {
        this.archives = List.of(archives);
    }

    private <T>JarArchive archive(Class<T> tClass) {
        for (JarArchive archive : archives)
            if (archive.has(tClass))
                return archive;
        throw new PatchException("Class file not founded!", this, Native.classFile(tClass));
    }

    @Override
    public String name() {
        return "auto";
    }

    @Override
    public <T> ClassPatcher<T> of(Class<T> tClass) {
        return archive(tClass).of(tClass);
    }
    @Override
    public <T> JarArchiveBase patchMethod(IMethodFilter<T> filter, MethodPatcher patcher) {
        archive(filter.tClass()).patchMethod(filter, patcher);
        return this;
    }
    @Override
    public <T> JarArchiveBase patchMethod(List<IMethodFilter<T>> filters, MethodPatcher patcher) {
        filters.forEach(filter -> patchMethod(filter, patcher));
        return null;
    }
    @Override
    public <T> JarArchiveBase patch(Class<T> tClass, Func2<JarArchive, ClassWriter, ClassVisitor> visitor, List<PatchException> exceptions) {
        return archive(tClass).patch(tClass, visitor, exceptions);
    }
}
