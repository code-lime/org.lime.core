package org.lime.core.common;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.minecraft.unsafe.GlobalConfigure;
import org.lime.core.common.reflection.ReflectionField;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.api.RequireCommand;
import org.lime.core.common.api.Service;
import org.lime.core.common.api.commands.CommandConsumer;
import org.lime.core.common.utils.AnnotationUtils;
import org.lime.core.common.utils.Disposable;
import org.lime.core.common.utils.JarAccessUtils;
import org.slf4j.Logger;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class BaseInstance<Instance extends BaseInstance<Instance>> {
    public BaseInstanceModule<?> module;

    protected abstract boolean isCore();

    public abstract String name();
    public abstract Artifact artifact();
    protected abstract Logger logger();
    protected abstract Stream<Path> jars();
    protected abstract ClassLoader loader();
    protected abstract File dataFolder();

    protected abstract BaseInstanceModule<Instance> createModule();

    private Injector injector;

    private final Set<Service> services = ConcurrentHashMap.newKeySet();
    protected final Disposable.Composite compositeDisposable = Disposable.composite();

    private Disposable registerCommandCast(Object command) {
        if (command instanceof CommandConsumer<?> commandConsumer)
            return registerCommand(commandConsumer);
        else
            throw new IllegalArgumentException(command.getClass() + " is not command supplier");
    }
    protected Disposable registerCommand(CommandConsumer<?> command) {
        for (var register : commandRegisters()) {
            if (command.isCast(register)) {
                command.applyCast(register);
                return Disposable.empty();
            }
        }
        throw new IllegalArgumentException("Not supported " + command.getClass() + " command supplier with register " + command.registerClass());
    }
    protected abstract Iterable<CommandConsumer.BaseRegister> commandRegisters();

    protected void enableService(Service service) {}
    protected void disableService(Service service) {}

    public void enable() {
        if (isCore())
            GlobalConfigure.configure();

        module = createModule();
        if (isCore())
            module.executeCore();
        injector = Guice.createInjector(module);
        Logger logger = logger();
        module.services
                .forEach(serviceClass -> {
                    Service service = injector.getInstance(serviceClass);
                    if (!services.add(service))
                        return;
                    try {
                        AnnotationUtils.recursiveAnnotations(RequireCommand.class, serviceClass)
                                .forEach(v -> {
                                    if (v.member() instanceof Field field) {
                                        compositeDisposable.add(registerCommandCast(ReflectionField.of(field).access().get(service)));
                                    }
                                    else if (v.member() instanceof Method method)
                                        compositeDisposable.add(registerCommandCast(ReflectionMethod.of(method).access().call(service, new Object[0])));
                                });
                        enableService(service);
                        compositeDisposable.add(service.register());
                        logger.info("Service '{}' loaded", service.getClass().getSimpleName());
                    } catch (Exception e) {
                        logger.error("Service '{}' error loading", service.getClass().getSimpleName(), e);
                        throw e;
                    }
                });
    }
    public void disable() {
        Logger logger = logger();
        module.services
                .descendingIterator()
                .forEachRemaining(serviceClass -> {
                    var service = injector.getInstance(serviceClass);
                    try {
                        service.unregister();
                        disableService(service);
                        logger.info("Service '{}' unloaded", service.getClass().getSimpleName());
                    } catch (Exception e) {
                        logger.error("Service '{}' error unloading", service.getClass().getSimpleName(), e);
                    }
                });
        compositeDisposable.close();
    }

    protected Stream<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotationClass) {
        return JarAccessUtils.findAnnotatedClasses(logger(), annotationClass, jars(), loader());
    }
}
