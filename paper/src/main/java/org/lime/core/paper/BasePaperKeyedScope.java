package org.lime.core.paper;

import com.google.inject.Key;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.lime.core.common.api.scope.BaseKeyedScope;

public abstract class BasePaperKeyedScope<TKey>
        extends BaseKeyedScope<BasePaperPlugin.Instance, TKey> {
    protected final BasePaperPlugin plugin;

    protected BasePaperKeyedScope(BasePaperPlugin plugin) {
        super(plugin.instance());
        this.plugin = plugin;
    }

    @Override
    protected <T> Composite register(Key<T> elementKey, T element) {
        Composite composite = super.register(elementKey, element);
        if (element instanceof Listener listener) {
            this.plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            composite.add(() -> HandlerList.unregisterAll(listener));
        }
        return composite;
    }
}
