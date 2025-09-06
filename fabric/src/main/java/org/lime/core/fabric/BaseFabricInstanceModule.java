package org.lime.core.fabric;

import net.minecraft.server.MinecraftServer;
import org.lime.core.common.BaseInstanceModule;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.utils.system.Lazy;
import org.lime.core.fabric.commands.LiteCommandConsumerFactory;
import org.lime.core.fabric.commands.NativeCommandConsumerFactory;
import org.lime.core.fabric.tasks.FabricScheduleTaskService;

public class BaseFabricInstanceModule
        extends BaseInstanceModule<BaseFabricMod> {
    private final Lazy<NativeCommandConsumerFactory> nativeLazy;

    public BaseFabricInstanceModule(BaseFabricMod instance) {
        super(instance);
        nativeLazy = Lazy.of(() -> new NativeCommandConsumerFactory(instance.server));
    }

    @Override
    protected UnsafeMappingsUtility mappings() {
        return FabricUnsafeMappingsUtility.instance();
    }
    @Override
    protected LiteCommandConsumerFactory liteCommandFactory() {
        return LiteCommandConsumerFactory.INSTANCE;
    }
    @Override
    protected NativeCommandConsumerFactory nativeCommandFactory() {
        return nativeLazy.value();
    }

    @Override
    protected void configure() {
        super.configure();

        bind(MinecraftServer.class).toInstance(instance.server);
        bind(BaseFabricMod.class).toInstance(instance);

        bind(ScheduleTaskService.class).toInstance(instance.scheduleTaskService);
        bind(LiteCommandConsumerFactory.class).toInstance(liteCommandFactory());
        bind(NativeCommandConsumerFactory.class).toInstance(nativeCommandFactory());
    }
}
