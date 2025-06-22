package org.lime.core.common.api.elements;

import org.jetbrains.annotations.Nullable;
import org.lime.core.common.BaseCoreCommandAccess;
import org.lime.core.common.api.BaseConfig;
import org.lime.core.common.api.commands.BaseCoreCommandRegister;
import org.lime.core.common.system.execute.Action0;
import org.lime.core.common.system.execute.Action1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseCoreElement<T, Command extends BaseCoreCommandRegister<Owner>, Owner extends BaseCoreCommandAccess<Command, Owner> & BaseConfig, Self extends BaseCoreElement<T, Command, Owner, Self>>
        implements CoreElementIdentity<T>,
        CoreElementInstance<T, Self>,
        CoreElementInit<Command, Owner, Self>,
        CoreElementPermission<Self>,
        CoreElementCommand<Command, Owner, Self>,
        CoreElementConfig<Self> {
    public final Class<T> tClass;
    public final String name;
    public @Nullable T instance;
    public @Nullable Action1<Owner> init;
    public @Nullable Action0 uninit;
    public SortType sortType = SortType.Default;
    public boolean disable = false;

    public final List<Command> commands = new ArrayList<>();
    public final List<CoreResource<?>> resources = new ArrayList<>();
    public final List<String> permissions = new ArrayList<>();

    protected abstract Self self();

    protected BaseCoreElement(Class<T> tClass) {
        this(tClass, tClass.getSimpleName());
    }
    protected BaseCoreElement(Class<T> tClass, String name) {
        this.tClass = tClass;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }
    @Override
    public Class<T> elementClass() {
        return tClass;
    }

    public Self withInstance(T instance) {
        this.instance = instance;
        return self();
    }
    public Self withInit(Action1<Owner> init) {
        this.init = init;
        return self();
    }
    public Self addPermissions(String... permissions) {
        Collections.addAll(this.permissions, permissions);
        return self();
    }
    public Self addCommands(Command... commands) {
        Collections.addAll(this.commands, commands);
        return self();
    }
    public Self withUninit(Action0 uninit) {
        this.uninit = uninit;
        return self();
    }
    public Self sortType(SortType sortType) {
        this.sortType = sortType;
        return self();
    }
    public Self disable() {
        this.disable = true;
        return self();
    }
    public Self addFile(String file, String name, CoreResource<?> data) {
        this.resources.add(data.withFile(file).withName(name));
        return self();
    }

    protected abstract void $register(Owner owner);

    public void register(Owner owner) {
        if (init != null)
            init.invoke(owner);
        resources.forEach(data -> data.read(owner, false));
        commands.forEach(owner::addCommand);
        $register(owner);
    }
}