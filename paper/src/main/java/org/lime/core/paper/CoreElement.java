package org.lime.core.paper;

import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.lime.core.common.api.elements.BaseCoreElement;

public final class CoreElement<T>
        extends BaseCoreElement<T, CoreCommand.Register, CoreInstancePlugin.CoreInstance, CoreElement<T>> {
    @Override
    protected CoreElement<T> self() {
        return this;
    }

    CoreElement(Class<T> tClass) {
        super(tClass);
    }

    @Override
    protected void $register(CoreInstancePlugin.CoreInstance instance) {
        JavaPlugin plugin = instance.plugin();
        if (this.instance != null) {
            if (this.instance instanceof Listener listener)
                Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
        PluginManager manager = Bukkit.getPluginManager();
        this.permissions.forEach(name -> {
            manager.removePermission(name);
            manager.addPermission(new Permission(name));
        });
    }

    @Override
    public CoreCommand.Register command(String cmd) {
        return CoreCommand.Register.create(cmd);
    }
}
