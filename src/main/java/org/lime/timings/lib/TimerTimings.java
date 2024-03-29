package org.lime.timings.lib;

import org.bukkit.scheduler.BukkitTask;
import org.lime.plugin.Timers;
import org.lime.system.toast.Toast2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class TimerTimings {
    private static final ConcurrentHashMap<Toast2<Boolean, StackTraceElement>, Toast2<Integer, Long>> timings = new ConcurrentHashMap<>();
    /*private static class TimingsInvocationHandler implements InvocationHandler {
        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return invokeProxy((Timing)proxy, method, args);
        }
        public Object invokeProxy(Timing base, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "startTiming": return startTiming(base);
                case "stopTiming": stopTiming(base); return null;
                case "startTimingIfSync": return startTimingIfSync(base);
                case "stopTimingIfSync": stopTimingIfSync(base); return null;
                case "abort": abort(base); return null;
                case "getTimingHandler": return getTimingHandler(base);
                case "close": close(base); return null;
                default: return null;
            }
        }

        public final boolean async;
        public final StackTraceElement element;
        public TimingsInvocationHandler(boolean async, StackTraceElement element) {
            this.async = async;
            this.element = element;
        }

        public @Nonnull Timing startTimingIfSync(Timing base) { return startTiming(base); }
        public void stopTimingIfSync(Timing base) { stopTiming(base); }
        public void close(Timing base) { stopTiming(base); }

        public @Nullable Object getTimingHandler(Timing base) { return null; }
        @Deprecated public void abort(Timing base) { }

        private long startTime = 0L;
        public @Nonnull Timing startTiming(Timing base) {
            startTime = System.currentTimeMillis();
            return base;
        }
        public void stopTiming(Timing base) {
            if (startTime == 0L) return;
            long delta = System.currentTimeMillis() - startTime;
            if (delta < 0) return;
            timings.compute(system.toast(async, element), (key, value) -> value == null ? system.toast(1, delta) : system.toast(value.val0 + 1, value.val1 + delta));
        }
    }
    private static Optional<Timing> createInstance(boolean async, core.ITimers.TimerType type) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int index = switch (type) {
            case StaticCore -> 6;
            case TimerBuilder -> 7;
        };
        if (index >= stackTrace.length) return Optional.empty();
        return Optional.of((Timing)Proxy.newProxyInstance(Timing.class.getClassLoader(), new Class[] { Timing.class }, new TimingsInvocationHandler(async, stackTrace[index])));
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int index = 1;
        int length = stackTrace.length;
        String className = null;
        String loaderName = stackTrace[1].getClassLoaderName();
        int func = 0;
        for (; index < length; index++) {
            StackTraceElement element = stackTrace[index];
            switch (func) {
                case 0: {
                    if (Objects.equals(loaderName, element.getClassLoaderName())) continue;
                    loaderName = element.getClassLoaderName();
                    className = element.getClassName();
                    func = 1;
                    break;
                }
                case 1: {
                    if (Objects.equals(className, element.getClassName()) && Objects.equals(loaderName, element.getClassLoaderName())) continue;
                    return Optional.of(proxyConstructor.newInstance(new TimingsInvocationHandler(async, element)));
                }
            }
        }
        return Optional.empty();
    }

    private static final system.LockToast1<reflection.field<Timing>> timings_CraftTask = system.<reflection.field<Timing>>toast(null).lock();
    */
    public static BukkitTask of(BukkitTask task, Timers.TimerType type) {
        /*if (task.getClass().getSimpleName().equals("CraftTask")) {
            createInstance(!task.isSync(), type)
                .ifPresent(timing -> {
                    timings_CraftTask.invoke(v -> {
                        if (v.val0 == null) v.val0 = reflection.field.of(task.getClass(), "timings");
                        v.val0.set(type, timing); craftTask.timings = timing;
                    });
                });
        }
        else core.instance._logStackTrace(new ClassCastException("BukkitTask("+task.getClass().getSimpleName()+") is not CraftTask! Ignore timings..."));
        */
        return task;
    }

    public static Stream<Map.Entry<Toast2<Boolean, StackTraceElement>, Toast2<Integer, Long>>> timings() {
        return timings.entrySet().stream();
    }
}



















