package net.minecraft.unsafe;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

public class GlobalConfigure {
    public static void configure() {
        Thread.setDefaultUncaughtExceptionHandler((t,s) -> printFullStackTrace(t,s,System.err));
    }

    public static void printFullStackTrace(Thread thread, Throwable throwable, PrintStream writer) {
        printFullStackTrace(thread, throwable, writer, new HashSet<>());
    }

    private static void printFullStackTrace(Thread thread, Throwable throwable, PrintStream writer, Set<Throwable> dejaVu) {
        if (dejaVu.contains(throwable)) {
            writer.println("\t[CIRCULAR REFERENCE: " + throwable + "]");
            return;
        }
        if (dejaVu.isEmpty())
            writer.println("Exception in thread \"" + thread.getName() + "\" " + throwable.toString());
        else
            writer.println(throwable.toString());

        dejaVu.add(throwable);

        for (StackTraceElement element : throwable.getStackTrace()) {
            writer.println("\tat " + element);
        }

        Throwable cause = throwable.getCause();
        if (cause != null) {
            writer.println("Caused by:");
            printFullStackTrace(thread, cause, writer, dejaVu);
        }
    }
}
