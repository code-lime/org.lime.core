package org.lime.core.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.BaseCoreInstance;
import org.lime.core.common.UnsafeMappings;
import org.lime.core.common.agent.Agents;
import org.lime.core.common.api.tasks.ScheduleTaskService;
import org.lime.core.fabric.tasks.FabricScheduleTaskService;

import java.util.stream.Stream;

public class CoreInstance
        extends BaseCoreInstance<CoreCommand.Register, CoreInstance>
        implements ModInitializer, FabricState, FabricLogger, FabricElementAccess, FabricCommandAccess {
    static {
        Agents.load();
    }

    public static CoreInstance core;

    private ScheduleTaskService taskService;
    private MinecraftServer server;
    private FabricIdentity identity;

    @Override
    public CoreCommand.Register createCommand(String cmd) {
        return CoreCommand.Register.create(cmd);
    }

    @Override
    public ClassLoader classLoader() {
        return this.getClass().getClassLoader();
    }
    @Override
    public MinecraftServer server() {
        return server;
    }
    @Override
    public ScheduleTaskService taskService() {
        return taskService;
    }
    public String modId() {
        return identity.modId();
    }
    @Override
    public String name() {
        return identity.name();
    }
    @Override
    public CoreInstance self() {
        return this;
    }

    @Override
    protected void setBaseCore(CoreInstance core) {
        super.setBaseCore(core);
        CoreInstance.core = core;
    }

    @Override
    public void onInitialize() {
        identity = FabricIdentity.of(this)
                .orElseThrow(() -> new IllegalArgumentException("Mod '"+this.getClass()+"' not found"));
        onLoad();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            enableInstance();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(v0 -> disableInstance());
    }

    public void onLoad() {}

    @Override
    protected void preInitCore() {
        FabricScheduleTaskService fabricTaskService = new FabricScheduleTaskService(this);
        taskService = fabricTaskService;
        ServerTickEvents.END_SERVER_TICK.register(v0 -> fabricTaskService.serverTick());
        super.preInitCore();
    }
    @Override
    protected void preInitInstance() {
        taskService = CoreInstance.core.taskService();
    }

    @Override
    protected Stream<CoreInstance> globalInstances() {
        return FabricLoaderImpl.INSTANCE.getEntrypoints("main", ModInitializer.class)
                .stream()
                .filter(CoreInstance.class::isInstance)
                .map(CoreInstance.class::cast);
    }

    @Override
    protected UnsafeMappings mappings() {
        return FabricUnsafeMappings.instance();
    }
    @Override
    protected @Nullable String coreCommandsPostfix() {
        return null;
    }
}
