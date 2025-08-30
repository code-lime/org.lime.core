package org.lime.core.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import org.lime.core.common.BaseInstanceModule;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.velocity.commands.LiteCommandConsumerFactory;
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
    protected LiteCommandConsumerFactory liteCommandFactory() {
        return LiteCommandConsumerFactory.INSTANCE;
    }
    @Override
    protected NativeCommandConsumerFactory nativeCommandFactory() {
        return NativeCommandConsumerFactory.INSTANCE;
    }

    @Override
    protected void configure() {
        super.configure();

        bind(ProxyServer.class).toInstance(instance.server);
        bind(BaseVelocityPlugin.class).toInstance(instance);

        bind(ScheduleTaskService.class).toInstance(instance.taskService);
        bind(LiteCommandConsumerFactory.class).toInstance(liteCommandFactory());
        bind(NativeCommandConsumerFactory.class).toInstance(nativeCommandFactory());
    }
}
