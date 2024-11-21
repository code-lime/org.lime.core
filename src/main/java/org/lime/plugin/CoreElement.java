package org.lime.plugin;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.lime.LimeCore;
import org.lime.system.execute.Action0;
import org.lime.system.execute.Action1;
import org.lime.system.execute.Func0;
import org.lime.system.execute.Func1;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("all")
public final class CoreElement {
    public enum SortType {
        First(Double.NEGATIVE_INFINITY),
        Default(0),
        Last(Double.POSITIVE_INFINITY);

        private final double value;

        SortType(double value) { this.value = value; }
        public double getValue() { return value; }
    }

    public final Class<?> tClass;
    public final String name;
    public final Object instance;
    public final Action1<LimeCore> init;
    public final Action0 uninit;
    public final SortType sortType;
    public final boolean disable;

    public final List<Func0<CoreCommand<?>>> commands;
    public final List<CoreData<?>> config;
    public final List<Permission> permissions;

    private CoreElement(Class<?> tClass, String name, Object instance, Action1<LimeCore> init, Action0 uninit, List<CoreData<?>> config, List<Func0<CoreCommand<?>>> commands, List<Permission> permissions, SortType sortType, boolean disable) {
        this.tClass = tClass;
        this.name = name;
        this.instance = instance;
        this.init = init;
        this.uninit = uninit;
        this.config = config;
        this.commands = commands;
        this.permissions = permissions;
        this.sortType = sortType;
        this.disable = disable;
    }

    public static CoreElement create(Class<?> tClass) { return new CoreElement(tClass, tClass.getSimpleName(), null, null, null, ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), SortType.Default, false); }

    public CoreElement withInstance() { try { return withInstance(tClass.getDeclaredConstructor().newInstance()); } catch (Exception e) { throw new IllegalArgumentException(e); } }
    public CoreElement withInstance(Object instance) { return new CoreElement(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable); }
    public CoreElement withInit(Action0 init) { return withInit(v -> init.invoke()); }
    public CoreElement withInit(Action1<LimeCore> init) { return new CoreElement(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable); }
    public CoreElement addPermission(String permission) { return addPermission(new Permission(permission)); }
    public CoreElement addPermission(Permission permission) { return addPermissions(permission); }
    public CoreElement addPermissions(String... permissions) { return addPermissions(Arrays.stream(permissions).map(Permission::new).toArray(Permission[]::new)); }
    public CoreElement addPermissions(Permission... permissions) {
        return new CoreElement(tClass, name, instance, init, uninit, config, commands, ImmutableList.<Permission>builder().addAll(this.permissions).add(permissions).build(), sortType, disable);
    }
    public CoreElement addCommand(Func0<CoreCommand<?>> command) { return addCommands(command); }
    public CoreElement addCommand(String cmd, Func1<CoreCommand<CommandSender>, CoreCommand<?>> command) { return addCommands(() -> command.invoke(CoreCommand.create(cmd))); }
    public CoreElement addCommands(Func0<CoreCommand<?>>... commands) {
        return new CoreElement(tClass, name, instance, init, uninit, config, ImmutableList.<Func0<CoreCommand<?>>>builder().addAll(this.commands).add(commands).build(), permissions, sortType, disable);
    }
    public CoreElement withUninit(Action0 uninit) { return new CoreElement(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable); }
    public CoreElement sortType(SortType sortType) { return new CoreElement(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable); }
    public CoreElement disable() { return new CoreElement(tClass, name, instance, init, uninit, config, commands, permissions, sortType, true); }
    public CoreElement addText(String file, Func1<CoreData<String>, CoreData<String>> builder) { return addFile(file, file, builder.invoke(CoreData.text())); }
    public <T extends JsonElement> CoreElement addConfig(String config, Func1<CoreData<T>, CoreData<T>> builder) { return addFile(config + ".json", config, builder.invoke(CoreData.json())); }
    public CoreElement addFile(String file, String name, CoreData<?> data) {
        return new CoreElement(tClass, this.name, instance, init, uninit, ImmutableList.<CoreData<?>>builder().addAll(this.config).add(data.withFile(file).withName(name)).build(), commands, permissions, sortType, disable);
    }
    public CoreElement addEmpty(String key, Action0 callback) { return addFile("", key, CoreData.none().withInvoke(v -> callback.invoke())); }
    public CoreElement addEmptyInit(String key, Action0 callback) { return addFile("", key, CoreData.init().withInvoke(v -> callback.invoke())); }
}
