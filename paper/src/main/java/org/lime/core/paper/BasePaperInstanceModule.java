package org.lime.core.paper;

import com.google.inject.TypeLiteral;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboard;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;
import org.lime.core.common.services.UnsafeMappingsUtility;
import org.lime.core.common.services.ScheduleTaskService;
import org.lime.core.common.BaseInstanceModule;
import org.lime.core.common.utils.adapters.CommonGsonTypeAdapters;
import org.lime.core.common.utils.adapters.GsonTypeAdapters;
import org.lime.core.paper.commands.LiteCommandConsumerFactory;
import org.lime.core.paper.commands.NativeCommandConsumerFactory;
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
    protected LiteCommandConsumerFactory liteCommandFactory() {
        return LiteCommandConsumerFactory.INSTANCE;
    }
    @Override
    protected NativeCommandConsumerFactory nativeCommandFactory() {
        return NativeCommandConsumerFactory.INSTANCE;
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
        bind(Plugin.class).toInstance(instance.plugin);;
        bind(PluginBase.class).toInstance(instance.plugin);
        bind(JavaPlugin.class).toInstance(instance.plugin);
        bind(BasePaperPlugin.class).toInstance(instance.plugin);

        bind(MinecraftServer.class).toInstance(MinecraftServer.getServer());
        bindMapped(PlayerList.class, MinecraftServer.class, MinecraftServer::getPlayerList);
        bindMapped(ServerLevel.class, MinecraftServer.class, MinecraftServer::overworld);
        bindMapped(ServerScoreboard.class, MinecraftServer.class, MinecraftServer::getScoreboard);
        bindCast(Scoreboard.class, ServerScoreboard.class);
        bindCast(Level.class, ServerLevel.class);

        bind(CraftWorld.class).toInstance((CraftWorld) Bukkit.getWorlds().getFirst());
        bindCast(World.class, CraftWorld.class);
        bind(CraftScoreboardManager.class).toInstance((CraftScoreboardManager) Bukkit.getScoreboardManager());
        bindCast(ScoreboardManager.class, CraftScoreboardManager.class);
        bindMapped(CraftScoreboard.class, CraftScoreboardManager.class, CraftScoreboardManager::getMainScoreboard);
        bindCast(org.bukkit.scoreboard.Scoreboard.class, CraftScoreboard.class);

        bind(PluginManager.class).toInstance(Bukkit.getPluginManager());

        bind(new TypeLiteral<BasePaperInstance<?>>(){}).toInstance(instance);

        bind(BukkitScheduler.class).toInstance(Bukkit.getScheduler());
        bind(ScheduleTaskService.class).to(BukkitScheduleTaskService.class);
        bind(LiteCommandConsumerFactory.class).toInstance(liteCommandFactory());
        bind(NativeCommandConsumerFactory.class).toInstance(nativeCommandFactory());
    }
}
