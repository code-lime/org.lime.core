package patch.core;

import io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider;
import net.minecraft.paper.java.CacheLibraryLoader;
import net.minecraft.paper.java.RawPluginMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.LibraryLoader;
import org.lime.core;
import org.lime.system.execute.Execute;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import patch.*;

import java.util.List;
import java.util.Map;

public class MutatePatcher extends BasePluginPatcher {
    public static void register() {
        Patcher.addPatcher(new MutatePatcher());
    }

    private MutatePatcher() {
        super(core.class);
    }

    @Override public void patch(JarArchive versionArchive, JarArchive bukkitArchive) {
        versionArchive
                .patchMethod(IMethodFilter.of(SpigotPluginProvider.class, IMethodInfo.STATIC_CONSTRUCTOR, false),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Replace.Type", "Replace.Method");
                            }

                            private final String from = Type.getInternalName(LibraryLoader.class);
                            private final String to = Type.getInternalName(CacheLibraryLoader.class);

                            @Override public void visitTypeInsn(int opcode, String type) {
                                if (type.equals(from)) {
                                    Native.log("Replace '" + from + "' to '" + to + "'");
                                    type = to;
                                    setProgressDuplicate("Replace.Type");
                                }
                                super.visitTypeInsn(opcode, type);
                            }
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                if (owner.equals(from)) {
                                    Native.log("Replace '" + from + "' to '" + to + "' in method " + owner + "." + name + descriptor);
                                    owner = to;
                                    setProgressDuplicate("Replace.Method");
                                }
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }));

        String mapAnyAnySignature = signatureType(Type.getInternalName(Map.class), "*", "*");

        bukkitArchive
                .of(PluginDescriptionFile.class)
                .addInterface(Type.getInternalName(RawPluginMeta.class))
                .addField(Opcodes.ACC_PRIVATE, "rawData", Type.getDescriptor(Map.class), mapAnyAnySignature, null)
                .addMethod(MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                    @Override protected List<String> createProgressList() {
                        return List.of("Method.Append");
                    }

                    @Override public void visitCode() {
                        super.visitCode();
                        super.visitIntInsn(Opcodes.ALOAD, 0);
                        super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(PluginDescriptionFile.class), "rawData", Type.getDescriptor(Map.class));
                        super.visitInsn(Opcodes.ARETURN);
                        super.visitMaxs(0, 0);
                        super.visitEnd();

                        setProgress("Method.Append");
                    }
                }), Opcodes.ACC_PUBLIC, "rawData", Type.getMethodDescriptor(Type.getType(Map.class)), signatureMethod(mapAnyAnySignature), new String[0])
                .addMethod(MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                    @Override protected List<String> createProgressList() {
                        return List.of("Method.Append");
                    }

                    @Override public void visitCode() {
                        super.visitCode();
                        super.visitIntInsn(Opcodes.ALOAD, 0);
                        super.visitIntInsn(Opcodes.ALOAD, 1);
                        super.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(PluginDescriptionFile.class), "rawData", Type.getDescriptor(Map.class));
                        super.visitInsn(Opcodes.RETURN);
                        super.visitMaxs(0, 0);
                        super.visitEnd();

                        setProgress("Method.Append");
                    }
                }), Opcodes.ACC_PUBLIC, "rawData", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Map.class)), signatureMethod(Type.VOID_TYPE.getDescriptor(), mapAnyAnySignature), new String[0])
                .patchMethod(IMethodFilter.of(PluginDescriptionFile.class, "loadMap", false),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Method.loadMap");
                            }

                            private int index = 0;
                            @Override public void visitLineNumber(int line, Label start) {
                                super.visitLineNumber(line, start);
                                if (index != 0) return;
                                index++;
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                super.visitVarInsn(Opcodes.ALOAD, 1);
                                Native.writeMethod(Execute.<RawPluginMeta, Map<?,?>>action(RawPluginMeta::rawData), super::visitMethodInsn);
                                setProgress("Method.loadMap");
                            }
                        }))
                .patch();
    }
}
