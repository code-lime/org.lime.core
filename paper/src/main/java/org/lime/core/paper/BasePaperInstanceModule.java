package org.lime.core.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.BaseInstanceModule;
import org.lime.core.paper.commands.LiteCommandConsumerFactory;
import org.lime.core.paper.commands.NativeCommandConsumerFactory;
import org.lime.core.paper.tasks.BukkitScheduleTaskService;
import patch.Patcher;

public class BasePaperInstanceModule<Instance extends BasePaperInstance<Instance>>
        extends BaseInstanceModule<Instance> {
    public BasePaperInstanceModule(Instance instance) {
        super(instance);
    }

    @Override
    protected UnsafeMappingsUtility mappings() {
        return PaperUnsafeMappingsUtility.instance();
    }
    @Override
    protected LiteCommandConsumerFactory liteCommandFactory() {
        return LiteCommandConsumerFactory.INSTANCE;
    }
    @Override
    protected NativeCommandConsumerFactory nativeCommandFactory() {
        return NativeCommandConsumerFactory.INSTANCE;
    }

    @Override
    protected void executeCore() {
        super.executeCore();
        Patcher.patch(instance.plugin.getLogger()::warning);
    }

    @Override
    protected void configure() {
        super.configure();

        bind(BasePaperInstance.class).toInstance(instance);
        bind(Plugin.class).toInstance(instance.plugin);;
        bind(PluginBase.class).toInstance(instance.plugin);
        bind(JavaPlugin.class).toInstance(instance.plugin);
        bind(BasePaperPlugin.class).toInstance(instance.plugin);

        bind(BukkitScheduler.class).toInstance(Bukkit.getScheduler());
        bind(ScheduleTaskService.class).to(BukkitScheduleTaskService.class);
        bind(LiteCommandConsumerFactory.class).toInstance(liteCommandFactory());
        bind(NativeCommandConsumerFactory.class).toInstance(nativeCommandFactory());
    }
}
