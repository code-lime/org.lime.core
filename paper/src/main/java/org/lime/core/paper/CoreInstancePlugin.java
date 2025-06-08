package org.lime.core.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.lime.core.common.BaseCoreInstance;
import org.lime.core.common.UnsafeMappings;
import org.lime.core.common.agent.Agents;
import org.lime.core.common.api.tasks.ScheduleTaskService;
import org.lime.core.paper.tasks.BukkitScheduleTaskService;
import patch.Patcher;
import patch.core.MutatePatcher;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class CoreInstancePlugin extends JavaPlugin {
    static {
        MutatePatcher.register();
        Agents.load();
    }

    protected final CoreInstance loader;

    public CoreInstancePlugin() {
        loader = new CoreInstance(this);
    }

    public CoreInstance loader() {
        return loader;
    }

    public @Nullable String logPrefix() {
        return null;
    }
    public @Nullable String configFile() {
        return null;
    }

    @Override
    public void onEnable() {
        loader.onEnable();
    }
    @Override
    public void onDisable() {
        loader.onDisable();
    }

    protected void init() {}

    public static final class CoreInstance
            extends BaseCoreInstance<CoreCommand.Register, CoreInstance>
            implements PaperState, PaperLogger, PaperElementAccess, PaperCommandAccess {
        public static CoreInstance core;

        private final CoreInstancePlugin plugin;

        public CoreInstance(CoreInstancePlugin plugin) {
            this.plugin = plugin;
        }

        private ScheduleTaskService taskService;

        @Override
        public CoreCommand.Register createCommand(String cmd) {
            return CoreCommand.Register.create(cmd);
        }

        @Override public String logPrefix() {
            return Objects.requireNonNullElseGet(plugin().logPrefix(), super::logPrefix);
        }
        @Override public String configFile() {
            return Objects.requireNonNullElseGet(plugin().configFile(), super::configFile);
        }

        @Override
        public CoreInstancePlugin plugin() {
            return plugin;
        }
        @Override
        public ClassLoader classLoader() {
            return plugin.getClassLoader();
        }
        @Override
        public File pluginFile() {
            return plugin.getFile();
        }
        @Override
        public ScheduleTaskService taskService() {
            return taskService;
        }
        @Override
        public String name() {
            return plugin.getName();
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

        public void onEnable() {
            super.enableInstance();
        }

        @Override
        public void init() {
            plugin.init();
        }

        @Override
        protected void preInitCore() {
            Patcher.patch(plugin().getLogger()::warning);
            taskService = new BukkitScheduleTaskService(plugin);
            super.preInitCore();
        }
        @Override
        protected void preInitInstance() {
            try { plugin().getClass().getDeclaredField("instance").set(null, plugin()); } catch (Exception _) { }
            taskService = CoreInstance.core.taskService();
        }

        @Override
        protected Stream<CoreInstance> globalInstances() {
            return Arrays.stream(Bukkit.getPluginManager().getPlugins())
                    .filter(CoreInstancePlugin.class::isInstance)
                    .map(CoreInstancePlugin.class::cast)
                    .map(CoreInstancePlugin::loader);
        }

        @Override
        protected UnsafeMappings mappings() {
            return PaperUnsafeMappings.instance();
        }

        public void onDisable() {
            super.disableInstance();

            if (core == this)
                Agents.unload();
        }
    }
}
