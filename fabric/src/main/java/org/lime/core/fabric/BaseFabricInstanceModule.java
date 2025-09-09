package org.lime.core.fabric;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Scoreboard;
import org.lime.core.common.BaseInstanceModule;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.utils.system.Lazy;
import org.lime.core.fabric.commands.LiteCommandConsumerFactory;
import org.lime.core.fabric.commands.NativeCommandConsumerFactory;
import org.lime.core.fabric.utils.adapters.FabricGsonTypeAdapters;

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
    protected Class<? extends FabricGsonTypeAdapters> gsonTypeAdapters() {
        return FabricGsonTypeAdapters.class;
    }

    @Override
    protected void configure() {
        super.configure();

        bind(BaseFabricMod.class).toInstance(instance);

        bind(MinecraftServer.class).toInstance(instance.server);
        bindMapped(PlayerList.class, MinecraftServer.class, MinecraftServer::getPlayerList);
        bindMapped(ServerLevel.class, MinecraftServer.class, MinecraftServer::overworld);
        bindMapped(ServerScoreboard.class, MinecraftServer.class, MinecraftServer::getScoreboard);
        bindCast(Scoreboard.class, ServerScoreboard.class);
        bindCast(Level.class, ServerLevel.class);

        bind(ScheduleTaskService.class).toInstance(instance.scheduleTaskService);
        bind(LiteCommandConsumerFactory.class).toInstance(liteCommandFactory());
        bind(NativeCommandConsumerFactory.class).toInstance(nativeCommandFactory());
    }
}
