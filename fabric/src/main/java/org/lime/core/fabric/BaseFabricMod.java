package org.lime.core.fabric;

import dev.rollczi.litecommands.LiteCommandsBuilder;
import dev.rollczi.litecommands.fabric.LiteFabricFactory;
import dev.rollczi.litecommands.fabric.LiteFabricSettings;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.lime.core.common.Artifact;
import org.lime.core.common.BaseInstance;
import org.lime.core.common.api.commands.CommandConsumer;
import org.lime.core.fabric.commands.LiteCommandConsumerFactory;
import org.lime.core.fabric.commands.NativeCommandConsumerFactory;
import org.lime.core.fabric.tasks.FabricScheduleTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class BaseFabricMod
        extends BaseInstance<BaseFabricMod>
        implements DedicatedServerModInitializer {
    private ModMetadata metadata;
    private Logger logger;
    private File dataFolder;

    private final boolean isCore;
    protected MinecraftServer server;
    protected FabricScheduleTaskService scheduleTaskService;
    private Iterable<CommandConsumer.BaseRegister> commandRegisters;

    public BaseFabricMod() {
        this.isCore = this.getClass() == CoreFabricMod.class;
    }

    @Override
    public void onInitializeServer() {
        metadata = FabricLoader.getInstance()
                .getEntrypointContainers("main", DedicatedServerModInitializer.class)
                .stream()
                .filter(v -> v.getEntrypoint() == this)
                .findFirst()
                .map(v -> v.getProvider().getMetadata())
                .orElseThrow(() -> new IllegalArgumentException("Metadata of mod '"+this.getClass()+"' not found"));

        dataFolder = Paths.get("config", metadata.getId()).toFile();
        logger = LoggerFactory.getLogger(metadata.getId());

        scheduleTaskService = new FabricScheduleTaskService(logger());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            enable();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(v0 -> disable());

        ServerTickEvents.END_SERVER_TICK.register(v0 -> scheduleTaskService.serverTick());
    }

    @Override
    public void enable() {
        LiteCommandsBuilder<CommandSourceStack, LiteFabricSettings, ?> liteCommandsBuilder = LiteFabricFactory.server();
        commandRegisters = List.of(
                new LiteCommandConsumerFactory.LiteRegister(liteCommandsBuilder),
                new NativeCommandConsumerFactory.NativeRegister(server.getCommands().getDispatcher(), new ArrayList<>()));
        super.enable();
        commandRegisters.forEach(register -> compositeDisposable.add(register.apply()));
        server.getPlayerList()
                .getPlayers()
                .forEach(server.getCommands()::sendCommands);
    }

    @Override
    protected Iterable<CommandConsumer.BaseRegister> commandRegisters() {
        return commandRegisters;
    }

    @Override
    protected abstract BaseFabricInstanceModule createModule();

    @Override
    protected Logger logger() {
        return logger;
    }
    @Override
    protected ClassLoader loader() {
        return getClass().getClassLoader();
    }
    @Override
    protected File dataFolder() {
        return dataFolder;
    }
    @Override
    public String id() {
        return metadata.getId();
    }
    @Override
    public String name() {
        return metadata.getName();
    }
    @Override
    public Artifact artifact() {
        return Artifact.FABRIC;
    }
    @Override
    protected final boolean isCore() {
        return isCore;
    }
    @Override
    protected Stream<Path> jars() {
        return FabricLoader.getInstance()
                .getModContainer(metadata.getId())
                .orElseThrow()
                .getOrigin()
                .getPaths()
                .stream();
    }
}
