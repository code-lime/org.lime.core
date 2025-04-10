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

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("all")
public final class CoreElement<T> {
    public enum SortType {
        First(Double.NEGATIVE_INFINITY),
        Default(0),
        Last(Double.POSITIVE_INFINITY);

        private final double value;

        SortType(double value) { this.value = value; }
        public double getValue() { return value; }
    }

    public final Class<T> tClass;
    public final String name;
    public final @Nullable T instance;
    public final Action1<LimeCore> init;
    public final Action0 uninit;
    public final SortType sortType;
    public final boolean disable;

    public final List<Func0<CoreCommand<?>>> commands;
    public final List<CoreData<?>> config;
    public final List<Permission> permissions;

    private CoreElement(Class<T> tClass, String name, @Nullable T instance, Action1<LimeCore> init, Action0 uninit, List<CoreData<?>> config, List<Func0<CoreCommand<?>>> commands, List<Permission> permissions, SortType sortType, boolean disable) {
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

    public static <T> CoreElement<T> create(Class<T> tClass) { return new CoreElement<T>(tClass, tClass.getSimpleName(), null, null, null, ImmutableList.of(), ImmutableList.of(), ImmutableList.of(), SortType.Default, false); }

    public CoreElement<T> withInstance() { try { return withInstance(tClass.getDeclaredConstructor().newInstance()); } catch (Exception e) { throw new IllegalArgumentException(e); } }
    public CoreElement<T> withInstance(T instance) { return new CoreElement(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable); }
    public CoreElement<T> withInit(Action0 init) { return withInit(v -> init.invoke()); }
    public CoreElement<T> withInit(Action1<LimeCore> init) { return new CoreElement<>(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable); }
    public CoreElement<T> addPermission(String permission) { return addPermission(new Permission(permission)); }
    public CoreElement<T> addPermission(Permission permission) { return addPermissions(permission); }
    public CoreElement<T> addPermissions(String... permissions) { return addPermissions(Arrays.stream(permissions).map(Permission::new).toArray(Permission[]::new)); }
    public CoreElement<T> addPermissions(Permission... permissions) {
        return new CoreElement<>(tClass, name, instance, init, uninit, config, commands, ImmutableList.<Permission>builder().addAll(this.permissions).add(permissions).build(), sortType, disable);
    }
    public CoreElement<T> addCommand(Func0<CoreCommand<?>> command) { return addCommands(command); }
    public CoreElement<T> addCommand(String cmd, Func1<CoreCommand<CommandSender>, CoreCommand<?>> command) { return addCommands(() -> command.invoke(CoreCommand.create(cmd))); }
    public CoreElement<T> addCommands(Func0<CoreCommand<?>>... commands) {
        return new CoreElement<>(tClass, name, instance, init, uninit, config, ImmutableList.<Func0<CoreCommand<?>>>builder().addAll(this.commands).add(commands).build(), permissions, sortType, disable);
    }
    public CoreElement<T> withUninit(Action0 uninit) { return new CoreElement<>(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable); }
    public CoreElement<T> sortType(SortType sortType) { return new CoreElement<>(tClass, name, instance, init, uninit, config, commands, permissions, sortType, disable); }
    public CoreElement<T> disable() { return new CoreElement<>(tClass, name, instance, init, uninit, config, commands, permissions, sortType, true); }
    public CoreElement<T> addText(String file, Func1<CoreData<String>, CoreData<String>> builder) { return addFile(file, file, builder.invoke(CoreData.text())); }
    public <J extends JsonElement> CoreElement<T> addConfig(String config, Func1<CoreData<J>, CoreData<J>> builder) { return addFile(config + ".json", config, builder.invoke(CoreData.json())); }
    public CoreElement<T> addFile(String file, String name, CoreData<?> data) {
        return new CoreElement<>(tClass, this.name, instance, init, uninit, ImmutableList.<CoreData<?>>builder().addAll(this.config).add(data.withFile(file).withName(name)).build(), commands, permissions, sortType, disable);
    }
    public CoreElement<T> addEmpty(String key, Action0 callback) { return addFile("", key, CoreData.none().withInvoke(v -> callback.invoke())); }
    public CoreElement<T> addEmptyInit(String key, Action0 callback) { return addFile("", key, CoreData.init().withInvoke(v -> callback.invoke())); }
}
