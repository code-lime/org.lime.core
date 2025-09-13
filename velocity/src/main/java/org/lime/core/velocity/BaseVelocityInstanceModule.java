package org.lime.core.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.lime.core.common.BaseInstanceModule;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.utils.adapters.CommonGsonTypeAdapters;
import org.lime.core.velocity.commands.NativeCommandConsumerFactory;
import org.lime.core.velocity.tasks.VelocityScheduleTaskService;

public class BaseVelocityInstanceModule
        extends BaseInstanceModule<BaseVelocityPlugin> {
    private static VelocityScheduleTaskService velocityTaskService;
    public BaseVelocityInstanceModule(BaseVelocityPlugin instance) {
        super(instance);
    }

    @Override
    protected UnsafeMappingsUtility mappings() {
        return VelocityUnsafeMappingsUtility.instance();
    }
    @Override
    protected NativeCommandConsumerFactory nativeCommandFactory() {
        return NativeCommandConsumerFactory.INSTANCE;
    }

    @Override
    protected MiniMessage.Builder miniMessage() {
        return super.miniMessage()
                .emitVirtuals(false);
    }

    @Override
    protected Class<? extends CommonGsonTypeAdapters> gsonTypeAdapters() {
        return CommonGsonTypeAdapters.class;
    }

    @Override
    protected void configure() {
        super.configure();

        bind(ProxyServer.class).toInstance(instance.server);
        bind(BaseVelocityPlugin.class).toInstance(instance);

        bind(ScheduleTaskService.class).toInstance(instance.taskService);
        bind(NativeCommandConsumerFactory.class).toInstance(nativeCommandFactory());
    }
}
