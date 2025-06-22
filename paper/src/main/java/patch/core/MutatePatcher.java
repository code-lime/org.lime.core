package patch.core;

import io.papermc.paper.plugin.provider.type.spigot.SpigotPluginProvider;
import net.minecraft.paper.java.CacheLibraryLoader;
import net.minecraft.paper.java.OptionSetUtils;
import net.minecraft.paper.java.RawPluginMeta;
import net.minecraft.paper.java.RepositoryLibraryLoader;
import net.minecraft.server.Main;
import net.minecraft.server.players.PlayerList;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.LibraryLoader;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.lime.core.common.system.execute.Execute;
import org.lime.core.common.system.execute.Callable;
import org.lime.core.paper.CoreInstancePlugin;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import patch.*;
import patch.JarArchiveAuto;

import java.io.File;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public class MutatePatcher extends BasePluginPatcher {
    public static void register() {
        Patcher.addPatcher(new MutatePatcher());
    }

    private MutatePatcher() {
        super(CoreInstancePlugin.class);
    }

    @Override public void patch(JarArchiveAuto archive) {
        var mutateCacheLibraryLoader = MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
            @Override protected List<String> createProgressList() {
                return List.of("Replace.Type", "Replace.Method");
            }

            private final String from = Type.getInternalName(LibraryLoader.class);
            private final String to = Type.getInternalName(CacheLibraryLoader.class);

            @Override public void visitTypeInsn(int opcode, String type) {
                if (type.equals(from)) {
                    Native.log("Replace '" + from + "' to '" + to + "' by " + OpcodesNames.getName(opcode));
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
        });

        archive
                .patchMethod(MethodFilter.of(SpigotPluginProvider.class, MethodInfo.STATIC_CONSTRUCTOR, false),
                        mutateCacheLibraryLoader)
                .patchMethod(MethodFilter.of(org.bukkit.plugin.java.JavaPluginLoader.class, MethodInfo.OBJECT_CONSTRUCTOR, false),
                        mutateCacheLibraryLoader)
                .patchMethod(MethodFilter.of(Execute.actionEx(Main::main)),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return List.of("Append.OptionSet");
                            }

                            private int index = 0;
                            @Override public void visitLineNumber(int line, Label start) {
                                super.visitLineNumber(line, start);
                                if (index != 0) return;
                                index++;
                                super.visitVarInsn(Opcodes.ALOAD, 0);
                                Native.writeMethod(Execute.action(OptionSetUtils::setOptions), super::visitMethodInsn);
                                setProgress("Append.OptionSet");
                            }
                        }))
                .patchMethod(MethodFilter.of(PlayerList.class, MethodInfo.STATIC_CONSTRUCTOR, false),
                        MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
                            @Override protected List<String> createProgressList() {
                                return modifyFields.keySet().stream().map(v -> "ModifyField." + v).toList();
                            }

                            private static final Map<String, Callable> modifyFields = Map.of(
                                    "OPLIST_FILE", Execute.func(() -> PlayerList.OPLIST_FILE),
                                    "WHITELIST_FILE", Execute.func(() -> PlayerList.WHITELIST_FILE),
                                    "IPBANLIST_FILE", Execute.func(() -> PlayerList.IPBANLIST_FILE),
                                    "USERBANLIST_FILE", Execute.func(() -> PlayerList.USERBANLIST_FILE)
                            );

                            @Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                modifyFields.forEach((id, field) -> {
                                    if (Native.isField(field, owner, name, descriptor)) {
                                        Native.writeMethod(Execute.<File, File>func(OptionSetUtils::getUniverseFile), super::visitMethodInsn);
                                        setProgress("ModifyField." + id);
                                    }
                                });
                                super.visitFieldInsn(opcode, owner, name, descriptor);
                            }
                        }));

        String mapAnyAnySignature = signatureType(Type.getInternalName(Map.class), "*", "*");

        archive
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
                .patchMethod(MethodFilter.of(PluginDescriptionFile.class, "loadMap", false),
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

        var libraryLoaderPatcher = archive
                .of(LibraryLoader.class)
                .addInterface(Type.getInternalName(RepositoryLibraryLoader.class));
        addGetter(libraryLoaderPatcher, "repository", RepositorySystem.class);
        addGetter(libraryLoaderPatcher, "session", DefaultRepositorySystemSession.class);
        addGetter(libraryLoaderPatcher, "repositories", List.class, signatureType(Type.getDescriptor(List.class), Type.getDescriptor(RemoteRepository.class)));
        libraryLoaderPatcher.patch();
    }
    private <T>void addGetter(ClassPatcher<T> patcher, String name, Class<?> getClass) {
        addGetter(patcher, name, getClass, Type.getDescriptor(getClass));
    }
    private <T>void addGetter(ClassPatcher<T> patcher, String name, Class<?> getClass, String getSignature) {
        patcher.addMethod(MethodPatcher.mutate(v -> new ProgressMethodVisitor(v, v) {
            @Override protected List<String> createProgressList() {
                return List.of("Method.Append");
            }

            @Override public void visitCode() {
                super.visitCode();
                super.visitIntInsn(Opcodes.ALOAD, 0);
                super.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(patcher.tClass()), name, Type.getDescriptor(getClass));
                super.visitInsn(Opcodes.ARETURN);
                super.visitMaxs(0, 0);
                super.visitEnd();

                setProgress("Method.Append");
            }
        }), Opcodes.ACC_PUBLIC, name, Type.getMethodDescriptor(Type.getType(getClass)), signatureMethod(getSignature), new String[0]);
    }
}
