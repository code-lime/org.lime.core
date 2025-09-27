package org.lime.core.paper;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import io.papermc.paper.datapack.DatapackManager;
import io.papermc.paper.datapack.PaperDatapackManager;
import io.papermc.paper.registry.PaperRegistryAccess;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.ServerTickManager;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboard;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager;
import org.bukkit.craftbukkit.structure.CraftStructureManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.structure.StructureManager;
import org.lime.core.common.services.InstancesUtility;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.BaseInstanceModule;
import org.lime.core.common.utils.adapters.CommonGsonTypeAdapters;
import org.lime.core.paper.commands.NativeCommandConsumerFactory;
import org.lime.core.paper.services.buffers.EntityBufferStorage;
import org.lime.core.paper.services.debug.DebugService;
import org.lime.core.paper.tasks.BukkitScheduleTaskService;
import org.lime.core.paper.utils.adapters.PaperGsonTypeAdapters;
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
    protected NativeCommandConsumerFactory nativeCommandFactory() {
        return NativeCommandConsumerFactory.INSTANCE;
    }

    @Override
    protected MiniMessage.Builder miniMessage() {
        return super.miniMessage()
                .emitVirtuals(false);
    }

    @Override
    protected Class<? extends InstancesUtility> instancesUtility() {
        return PaperInstancesUtility.class;
    }

    @Override
    protected Class<? extends CommonGsonTypeAdapters> gsonTypeAdapters() {
        return PaperGsonTypeAdapters.class;
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
        bind(Plugin.class).toInstance(instance.plugin);
        bind(PluginBase.class).toInstance(instance.plugin);
        bind(JavaPlugin.class).toInstance(instance.plugin);
        bind(BasePaperPlugin.class).toInstance(instance.plugin);

        bind(MinecraftServer.class).toInstance(MinecraftServer.getServer());
        bindMapped(PlayerList.class, MinecraftServer.class, MinecraftServer::getPlayerList);
        bindMappedCast(ServerLevel.class, Level.class, MinecraftServer.class, MinecraftServer::overworld);
        bindMappedCast(ServerScoreboard.class, Scoreboard.class, MinecraftServer.class, MinecraftServer::getScoreboard);
        bindMapped(RegistryAccess.class, MinecraftServer.class, MinecraftServer::registryAccess);
        bindMapped(Commands.class, MinecraftServer.class, MinecraftServer::getCommands);
        bindMapped(ServerAdvancementManager.class, MinecraftServer.class, MinecraftServer::getAdvancements);
        bindMappedCast(RecipeManager.class, RecipeAccess.class, MinecraftServer.class, MinecraftServer::getRecipeManager);
        bindMapped(ResourceManager.class, MinecraftServer.class, MinecraftServer::getResourceManager);
        bindMapped(StructureTemplateManager.class, MinecraftServer.class, MinecraftServer::getStructureManager);

        bind(CraftServer.class).toInstance((CraftServer) Bukkit.getServer());
        bindCast(Server.class, CraftServer.class);
        bindMappedCast(CraftWorld.class, World.class, CraftServer.class, v -> (CraftWorld) v.getWorlds().getFirst());
        bindMappedCast(CraftScoreboardManager.class, ScoreboardManager.class, CraftServer.class, CraftServer::getScoreboardManager);
        bindMappedCast(CraftScoreboard.class, org.bukkit.scoreboard.Scoreboard.class, CraftScoreboardManager.class, CraftScoreboardManager::getMainScoreboard);
        bindMappedCast(CraftStructureManager.class, StructureManager.class, CraftServer.class, v -> (CraftStructureManager)v.getStructureManager());
        bindMappedCast(PaperDatapackManager.class, DatapackManager.class, CraftServer.class, CraftServer::getDatapackManager);
        bindMapped(ServerTickManager.class, CraftServer.class, CraftServer::getServerTickManager);
        bind(PaperRegistryAccess.class).toInstance(PaperRegistryAccess.instance());
        bindCast(io.papermc.paper.registry.RegistryAccess.class, PaperRegistryAccess.class);

        bind(PluginManager.class).toInstance(Bukkit.getPluginManager());

        bind(new TypeLiteral<BasePaperInstance<?>>(){}).toInstance(instance);

        bind(BukkitScheduler.class).toInstance(Bukkit.getScheduler());
        bind(ScheduleTaskService.class).to(BukkitScheduleTaskService.class);
        bind(NativeCommandConsumerFactory.class).toInstance(nativeCommandFactory());

        if (!instance.isCore()) {
            bind(EntityBufferStorage.class)
                    .toProvider(new Provider<>() {
                        @Inject InstancesUtility instances;

                        @Override
                        public EntityBufferStorage get() {
                            return instances.core().injector().getInstance(EntityBufferStorage.class);
                        }
                    })
                    .asEagerSingleton();
            bind(DebugService.class)
                    .toProvider(new Provider<>() {
                        @Inject InstancesUtility instances;

                        @Override
                        public DebugService get() {
                            return instances.core().injector().getInstance(DebugService.class);
                        }
                    })
                    .asEagerSingleton();
        }
    }
}
