package org.lime.core.common.api;

import net.minecraft.unsafe.GlobalConfigure;
import org.lime.core.common.*;
import org.lime.core.common.api.commands.*;
import org.lime.core.common.api.elements.BaseCoreElement;
import org.lime.core.common.invokable.BaseInvokable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseCoreLoader<
        Command extends BaseCoreCommandRegister<Self, Command>,
        Self extends BaseCoreLoader<Command, Self>>
        implements BaseGlobal, BaseCoreJarAccess, BaseCoreCommandAccess<Command, Self>, BaseCoreElementAccess<Command, Self>, BaseInvokeProgress {
    protected abstract void setBaseCore(Self baseCore);

    @Override public TimerBuilder $timer() { return TimerBuilder.create(this); }
    @Override public void $invokable(BaseInvokable invokable) { tickCalls.add(invokable); }

    protected final List<BaseCoreElement<?, Command, Self, ?>> elements = new ArrayList<>();
    protected final HashMap<String, Command> commands = new HashMap<>();
    protected final ConcurrentLinkedQueue<BaseInvokable> tickCalls = new ConcurrentLinkedQueue<>();

    protected abstract Optional<ElementUpdateConfigInstance> config();

    public abstract Self self();

    protected void enableInstance() {
        Self self = self();
        if (BaseCoreInstance.global == null) {
            try {
                setBaseCore(self);
                preInitCore();
            } catch (Exception e) {
                $logStackTrace(e);
                throw e;
            }
            return;
        }

        preInitInstance();

        try { getClass().getDeclaredField("instance").set(null, this); } catch (Exception _) { }

        File dir = $configFile("");
        if (!dir.exists()) dir.mkdir();

        try {
            init();
            config().ifPresent(ElementUpdateConfigInstance::updateConfigSync);

            $logOP("[............] Start load elements...");
            invokeList(elements().sorted(Comparator.comparingDouble(v -> v.sortType.getValue())).collect(Collectors.toList()), (element, prefix) -> {
                $logOP(prefix + " Load " + element.name + "...");
                element.register(self);
            });
            $logOP("[100%] Loaded!");

            $logOP("[............] Start load commands...");
            invokeList(commands(), (name, cmd, prefix) -> {
                $logOP(prefix + " Load command " + name + "...");
                cmd.register(self);
            });
            flushCommands();
            $logOP("[100%] Loaded!");

            $repeatTicks(this::invokableTick, 1);
        } catch (Exception e) {
            $logStackTrace(e);
            onErrorInit();
        }
    }

    protected abstract Stream<Self> globalInstances();

    protected abstract UnsafeMappings mappings();
    protected void preInitCore() {
        Unsafe.MAPPINGS = mappings();
        GlobalConfigure.configure();

        addCommand("update.data", cmd -> {
            if (cmd instanceof CoreCommandSimple<?> simple)
                simple
                        .addOperatorOnly()
                        .withTabSimple(args -> switch (args.length) {
                            case 1 -> globalInstances().map(BaseCoreLoader::name).toList();
                            default -> globalInstances()
                                    .filter(corePlugin -> args[0].equals(corePlugin.name()))
                                    .findFirst()
                                    .map(corePlugin -> corePlugin
                                            .elements()
                                            .flatMap(v -> v.resources
                                                    .stream()
                                                    .flatMap(_v -> _v.getFiles().stream()))
                                            .toList())
                                    .orElseGet(Collections::emptyList);
                        })
                        .withExecutorSimple(args -> {
                            if (args.length < 2) return false;
                            Self corePlugin = globalInstances()
                                    .filter(plugin -> args[0].equals(plugin.name()))
                                    .findFirst()
                                    .orElse(null);
                            if (corePlugin == null) return false;
                            Collection<String> files = Arrays.stream(args).skip(1).toList();
                            Set<String> _files = new HashSet<>();
                            files.forEach(file -> corePlugin.elements().forEach(element -> element.resources.forEach(data -> {
                                if (!data.getFiles().contains(file)) return;
                                _files.addAll(data.getFiles());
                            })));
                            corePlugin.$logOP("Update files: " + String.join(" & ", _files));
                            try {
                                corePlugin.config().ifPresent(config -> config.updateConfigAsync(_files, () -> {
                                    try {
                                        files.forEach(file -> corePlugin.elements().forEach(element -> element.resources.forEach(data -> {
                                            if (!data.getFiles().contains(file)) return;
                                            data.read(corePlugin, true);
                                        })));
                                    } catch (Exception e) {
                                        corePlugin.$logStackTrace(e);
                                    }
                                }));
                            } catch (Exception e) {
                                corePlugin.$logStackTrace(e);
                                return true;
                            }
                            corePlugin.$logOP("Updated!");
                            return true;
                        });
        });

        Self self = self();

        commands().values().forEach(cmd -> cmd.register(self));
        flushCommands();
    }
    protected abstract void preInitInstance();

    protected abstract void init();
    protected void onErrorInit() {}

    protected void invokableTick() {
        tickCalls.removeIf(v -> {
            try { return v.tryRemoveInvoke(); }
            catch (Throwable e) { $logStackTrace(e); }
            return true;
        });
    }

    protected void disableInstance() {
        elements()
                .forEach(item -> {
                    if (item.uninit != null)
                        item.uninit.invoke();
                });
    }
}
