package org.lime.core.fabric;

import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.scores.Scoreboard;
import org.lime.core.common.BaseInstanceModule;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.utils.Lazy;
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
        bindMappedCast(FabricServerAudiences.class, AudienceProvider.class, MinecraftServer.class, FabricServerAudiences::of);
        bindMapped(PlayerList.class, MinecraftServer.class, MinecraftServer::getPlayerList);
        bindMappedCast(ServerLevel.class, Level.class, MinecraftServer.class, MinecraftServer::overworld);
        bindMappedCast(ServerScoreboard.class, Scoreboard.class, MinecraftServer.class, MinecraftServer::getScoreboard);
        bindMapped(RegistryAccess.class, MinecraftServer.class, MinecraftServer::registryAccess);
        bindMapped(Commands.class, MinecraftServer.class, MinecraftServer::getCommands);
        bindMapped(ServerAdvancementManager.class, MinecraftServer.class, MinecraftServer::getAdvancements);
        bindMapped(RecipeManager.class, MinecraftServer.class, MinecraftServer::getRecipeManager);
        bindMapped(ResourceManager.class, MinecraftServer.class, MinecraftServer::getResourceManager);
        bindMapped(StructureTemplateManager.class, MinecraftServer.class, MinecraftServer::getStructureManager);

        bind(ScheduleTaskService.class).toInstance(instance.scheduleTaskService);
        bind(NativeCommandConsumerFactory.class).toInstance(nativeCommandFactory());
    }
}
