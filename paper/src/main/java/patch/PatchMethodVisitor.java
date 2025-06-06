package patch;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PatchMethodVisitor extends MethodVisitor {
    public final JarArchive jar;
    public final MethodInfo method;

    protected PatchMethodVisitor(JarArchive jar, MethodInfo method, MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, methodVisitor);
        this.jar = jar;
        this.method = method;
    }
}
