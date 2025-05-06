package org.lime.core.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.lime.core.common.BaseCoreInstance;
import org.lime.core.common.UnsafeMappings;
import org.lime.core.common.api.tasks.ScheduleTaskService;
import org.lime.core.paper.tasks.BukkitScheduleTaskService;
import patch.Patcher;
import patch.core.MutatePatcher;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

public class CoreInstancePlugin extends JavaPlugin {
    static {
        MutatePatcher.register();
    }

    protected final CoreInstance instance = new CoreInstance();

    @Override
    public void onEnable() {
        instance.onEnable();
    }
    @Override
    public void onDisable() {
        instance.onDisable();
    }

    public void init() {}

    public final class CoreInstance
            extends BaseCoreInstance<CoreCommand.Register, CoreInstance>
            implements PaperState, PaperLogger, PaperElementAccess, PaperCommandAccess {
        public static CoreInstance core;

        private ScheduleTaskService taskService;

        @Override
        public CoreCommand.Register createCommand(String cmd) {
            return CoreCommand.Register.create(cmd);
        }

        @Override
        public JavaPlugin plugin() {
            return CoreInstancePlugin.this;
        }
        @Override
        public ClassLoader classLoader() {
            return CoreInstancePlugin.this.getClassLoader();
        }
        @Override
        public File pluginFile() {
            return CoreInstancePlugin.this.getFile();
        }
        @Override
        public ScheduleTaskService taskService() {
            return taskService;
        }
        @Override
        public String name() {
            return CoreInstancePlugin.this.getName();
        }
        @Override
        public CoreInstance self() {
            return this;
        }

        @Override
        protected void setBaseCore(CoreInstance core) {
            CoreInstance.core = core;
        }

        public void onEnable() {
            super.enableInstance();
        }

        @Override
        public void init() {
            CoreInstancePlugin.this.init();
        }

        @Override
        protected void preInitCore() {
            Patcher.patch(plugin().getLogger()::warning);
            taskService = new BukkitScheduleTaskService(CoreInstancePlugin.this);
            super.preInitCore();
        }
        @Override
        protected void preInitInstance() {
            taskService = CoreInstance.core.taskService();
        }

        @Override
        protected Stream<CoreInstance> globalInstances() {
            return Arrays.stream(Bukkit.getPluginManager()
                            .getPlugins())
                    .flatMap(v -> v instanceof CoreInstancePlugin plugin ? Stream.of(plugin.instance) : Stream.empty());
        }

        @Override
        protected UnsafeMappings mappings() {
            return PaperUnsafeMappings.instance();
        }

        public void onDisable() {
            super.disableInstance();
        }
    }
}
