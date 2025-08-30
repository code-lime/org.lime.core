package patch;

import org.lime.core.common.utils.system.tuple.Tuple;
import org.lime.core.common.utils.system.tuple.Tuple5;
import org.lime.core.common.utils.system.tuple.Tuple6;
import org.objectweb.asm.*;
import com.google.common.collect.Lists;

import java.util.*;

public final class ClassPatcher<T> {
    private final JarArchive archive;
    private final Class<T> tClass;
    private final List<ProgressMethodVisitor> progressVisitors = new ArrayList<>();

    private final List<String> appendInterfaces = new ArrayList<>();
    private final List<Tuple5<Integer, String, String, String, Object>> appendFields = new ArrayList<>();
    private final List<Tuple6<MethodPatcher, Integer, String, String, String, String[]>> appendMethods = new ArrayList<>();
    private final Map<MethodFilter<T>, MethodPatcher> patchMethods = new HashMap<>();

    public ClassPatcher(JarArchive archive, Class<T> tClass) {
        this.archive = archive;
        this.tClass = tClass;
    }

    public Class<T> tClass() {
        return tClass;
    }

    public ClassPatcher<T> addInterface(String interfaceClass) {
        this.appendInterfaces.add(interfaceClass);
        return this;
    }
    public ClassPatcher<T> addField(int access, String name, String descriptor, String signature, Object value) {
        appendFields.add(Tuple.of(access, name, descriptor, signature, value));
        return this;
    }
    public ClassPatcher<T> addMethod(MethodPatcher patcher, int access, String name, String descriptor, String signature, String[] exceptions) {
        appendMethods.add(Tuple.of(patcher, access, name, descriptor, signature, exceptions));
        return this;
    }
    public ClassPatcher<T> patchMethod(MethodFilter<T> filter, MethodPatcher patcher) {
        patchMethods.put(filter, patcher);
        return this;
    }

    public JarArchive patch() {
        List<PatchException> exceptions = new ArrayList<>();
        JarArchive result = archive.patch(tClass, (jar, writer) -> new ClassVisitor(Opcodes.ASM9, writer) {
            private final Map<MethodFilter<T>, MethodPatcher> filters = new HashMap<>(patchMethods);
            @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                if (!appendInterfaces.isEmpty()) {
                    List<String> _interfaces = Lists.newArrayList(interfaces);
                    _interfaces.addAll(appendInterfaces);
                    Native.log("Append interfaces: " + String.join(", ", appendInterfaces) + "...");
                    interfaces = _interfaces.toArray(new String[0]);
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }

            private final Set<String> updatedFields = new HashSet<>();

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                return appendFields.stream().filter(v -> v.val1.equals(name))
                        .findFirst()
                        .map(field -> {
                            Native.log("Update field: " + field.val1 + field.val2 + "...");
                            updatedFields.add(field.val1);
                            return field.invokeGet(super::visitField);
                        })
                        .orElseGet(() -> super.visitField(access, name, descriptor, signature, value));
            }

            @Override public void visitEnd() {
                appendFields.forEach(field -> {
                    if (updatedFields.contains(field.val1))
                        return;
                    Native.log("Append field: " + field.val1 + field.val2 + "...");
                    field.invokeGet(super::visitField).visitEnd();
                });
                appendMethods.forEach(method -> {
                    Native.log("Append method: " + method.val2 + method.val3 + "...");
                    ProgressMethodVisitor progressVisitor = method.val0.patch(jar, MethodInfo.raw(tClass, method.val2, method.val3), this, method.val1, method.val2, method.val3, method.val4, method.val5);
                    progressVisitors.add(progressVisitor);
                    progressVisitor.visitCode();
                });
                super.visitEnd();
                throwNotFoundCheck();
            }
            private void throwNotFoundCheck() {
                filters.forEach((filter, patcher) -> {
                    throw new PatchException("METHOD '" + filter.toInfo() + "' NOT FOUNDED FOR PATCH!", jar, filter);
                });
            }
            private boolean isSuper = false;
            @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (!isSuper) {
                    for (Map.Entry<MethodFilter<T>, MethodPatcher> kv : patchMethods.entrySet()) {
                        MethodFilter<T> filter = kv.getKey();
                        if (filter.test(access, name, descriptor, signature, exceptions)) {
                            filters.remove(filter);
                            Native.log("Patch method " + filter.toInfo() + "...");
                            isSuper = true;
                            ProgressMethodVisitor progressVisitor = kv.getValue().patch(jar, filter, this, access, name, descriptor, signature, exceptions);
                            progressVisitors.add(progressVisitor);
                            isSuper = false;
                            return progressVisitor;
                        }
                    }
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, exceptions);
        throwProgressCheck();
        exceptions.forEach(exception -> {
            throw exception;
        });
        return result;
    }
    public void throwProgressCheck() {
        progressVisitors.forEach(ProgressMethodVisitor::throwProgressCheck);
    }
}







