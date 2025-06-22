package org.lime.core.common;

import org.lime.core.common.api.BaseConfig;
import org.lime.core.common.api.CoreElementLoaded;
import org.lime.core.common.api.commands.BaseCoreCommandRegister;
import org.lime.core.common.api.elements.BaseCoreElement;
import org.lime.core.common.reflection.Reflection;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.system.execute.Func1;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public interface BaseCoreElementAccess<Command extends BaseCoreCommandRegister<Self>, Self extends BaseCoreElementAccess<Command, Self>>
        extends BaseCoreJarAccess, BaseCoreCommandAccess<Command, Self>, BaseConfig {
    <T, Element extends BaseCoreElement<T, Command, Self, Element>> CoreElementLoaded<T, Element> addElement(Element element);
    Class<? super BaseCoreElement<?, Command, Self, ?>> elementClass();
    Stream<? extends BaseCoreElement<?, Command, Self, ?>> elements();

    ClassLoader classLoader();

    default List<CoreElementLoaded<?, ?>> addOther() {
        List<CoreElementLoaded<?, ?>> other = new ArrayList<>();
        ClassLoader loader = classLoader();

        final int ACC_PUBLIC_STATIC = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        final String CREATE_NAME = "create";
        final String CREATE_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(elementClass()));

        jarClasses(() -> new AcceptClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ((access & ACC_PUBLIC_STATIC) == ACC_PUBLIC_STATIC && name.equals(CREATE_NAME) && descriptor.equals(CREATE_DESCRIPTOR))
                    accept();
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }).forEach(className -> {
            try {
                Class<?> tClass = loader.loadClass(className);
                Method method = Reflection.access(tClass.getDeclaredMethod(CREATE_NAME));
                if (!Modifier.isStatic(method.getModifiers())) return;
                if (method.getReturnType() != elementClass()) return;
                if (elements().anyMatch(v -> v.tClass == tClass)) return;
                BaseCoreElement element = (BaseCoreElement)method.invoke(null);
                other.add(this.addElement(element));
            } catch (NoSuchMethodException ignored) {

            } catch (Throwable e) {
                BaseCoreInstance.global.$logOP("ERROR LOAD: " + className);
                BaseCoreInstance.global.$logStackTrace(e);
            }
        });
        return other;
    }
}
