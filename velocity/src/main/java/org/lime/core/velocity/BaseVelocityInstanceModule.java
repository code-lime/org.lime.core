package org.lime.core.velocity;

import com.google.inject.TypeLiteral;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.lime.core.common.BaseInstanceModule;
import org.lime.core.common.services.InstancesUtility;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.services.memories.BaseConnectionStorageService;
import org.lime.core.common.services.skins.BaseSkinsCache;
import org.lime.core.common.utils.Lazy;
import org.lime.core.velocity.commands.NativeCommandConsumerFactory;
import org.lime.core.velocity.services.ConnectionStorageService;
import org.lime.core.velocity.services.SkinsCache;
import org.lime.core.velocity.utils.adapters.VelocityGsonTypeAdapters;

public class BaseVelocityInstanceModule
        extends BaseInstanceModule<BaseVelocityPlugin> {
    private final Lazy<NativeCommandConsumerFactory> nativeLazy;

    public BaseVelocityInstanceModule(BaseVelocityPlugin instance) {
        super(instance);
        nativeLazy = Lazy.of(() -> new NativeCommandConsumerFactory(instance.server));
    }

    @Override
    protected UnsafeMappingsUtility mappings() {
        return VelocityUnsafeMappingsUtility.instance();
    }
    @Override
    protected NativeCommandConsumerFactory nativeCommandFactory() {
        return nativeLazy.value();
    }

    @Override
    protected MiniMessage.Builder miniMessage() {
        return super.miniMessage()
                .emitVirtuals(false);
    }

    @Override
    protected Class<? extends InstancesUtility> instancesUtility() {
        return VelocityInstancesUtility.class;
    }

    @Override
    protected Class<? extends VelocityGsonTypeAdapters> gsonTypeAdapters() {
        return VelocityGsonTypeAdapters.class;
    }

    @Override
    protected void configure() {
        super.configure();

        bind(ProxyServer.class).toInstance(instance.server);
        bind(BaseVelocityPlugin.class).toInstance(instance);

        bind(ScheduleTaskService.class).toInstance(instance.taskService);
        bind(NativeCommandConsumerFactory.class).toInstance(nativeCommandFactory());

        bindCast(new TypeLiteral<BaseSkinsCache<?,?>>() {}, SkinsCache.class);
        bindCast(BaseConnectionStorageService.class, ConnectionStorageService.class);

        if (!instance.isCore()) {
            bindFromCore(SkinsCache.class);
            bindFromCore(ConnectionStorageService.class);
        }
    }
}
