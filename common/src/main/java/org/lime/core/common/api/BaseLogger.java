package org.lime.core.common.api;

import com.google.common.collect.Lists;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public interface BaseLogger extends BaseState {
    String logPrefix();

    Audience consoleAudiences();
    Audience playersAudiences(boolean operatorsOnly);

    default void $logToFile(String key, String text) {
        final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        String time = formatter.format(calendar.getTime());

        if (text.contains("{time}")) {
            final DateFormat time_formatter = new SimpleDateFormat("HH:mm:ss");
            calendar.setTimeInMillis(System.currentTimeMillis());
            text = text.replace("{time}", time_formatter.format(calendar.getTime()));
        }

        try {
            String path = "logs/lime/" + key + "/";
            File logs = new File(path);
            if (!logs.exists()) logs.mkdirs();

            key = key.split("/", 2)[0];

            File myObj = new File(path + key + "-" + time + ".log");
            if (!myObj.exists()) myObj.createNewFile();

            FileWriter myWriter = new FileWriter(myObj, true);
            myWriter.write(text + "\r\n");
            myWriter.close();
        } catch (FileNotFoundException e) {
            $log("An error occurred.");
            $log(e.getMessage());
        } catch (Exception e)
        {
            $log(e.getMessage());
        }
    }
    default void $log(String log) {
        consoleAudiences()
                .sendMessage(Component.text("["+ logPrefix()+"] " + log));
    }
    default void $logBroadcast(String log) {
        $logToFile("log_admin", "[{time}] " + log);
        $log(log);
        playersAudiences(false)
                .sendMessage(Component.text("["+ logPrefix()+"] " + log));
    }
    default void $logConsole(String log) {
        $logToFile("log_admin", "[{time}] " + log);
        $log(log);
    }
    default void $logOP(String log) {
        $logToFile("log_admin", "[{time}] " + log);
        $log(log);
        playersAudiences(true)
                .sendMessage(Component.text("["+ logPrefix()+"] ").color(NamedTextColor.YELLOW).append(Component.text(log).color(NamedTextColor.WHITE)));
    }
    default void $logOP(Component log) {
        String plain_log = PlainTextComponentSerializer.plainText().serialize(log);
        $logToFile("log_admin", "[{time}] " + plain_log);
        $log(plain_log);
        playersAudiences(true)
                .sendMessage(Component.text("["+ logPrefix()+"] ").color(NamedTextColor.YELLOW).append(Component.empty().append(log).color(NamedTextColor.WHITE)));
    }
    default void $logWithoutPrefix(String log) {
        consoleAudiences().sendMessage(Component.text(log));
    }

    private void $logStackTraceSub(List<StackTraceElement> stackTraceElements) {
        Component client = Component.empty();
        List<String> lines = new ArrayList<>();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            String line = stackTraceElement.toString();
            $log(line);
            lines.add(line);
            client = client.append(Component.text("  " + line));
        }
        client = Component.text(" - ")
                .color(NamedTextColor.AQUA)
                .append(Component.text("[StackTracePart]")
                        .hoverEvent(HoverEvent.showText(client))
                        .clickEvent(ClickEvent.copyToClipboard(String.join("\n", lines)))
                        .color(NamedTextColor.GREEN)
                );
        playersAudiences(true).sendMessage(client);
    }
    default void $logStackTrace(StackTraceElement[] stackTraceElements) {
        if (stackTraceElements.length > 30) {
            Component client = Component.text("["+ logPrefix()+"] ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(" StackTraceList:"));
            playersAudiences(true).sendMessage(client);
            for (List<StackTraceElement> lines : Lists.partition(Arrays.asList(stackTraceElements), 30)) {
                $logStackTraceSub(lines);
            }
        } else {
            Component client = Component.empty();
            List<String> lines = new ArrayList<>();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                String line = stackTraceElement.toString();
                $log(line);
                lines.add(line);
                client = client.append(Component.text("  " + line));
            }
            client = Component.text("["+ logPrefix()+"] ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("[StackTrace]")
                            .hoverEvent(HoverEvent.showText(client))
                            .clickEvent(ClickEvent.copyToClipboard(String.join("\n", lines)))
                            .color(NamedTextColor.GREEN)
                    );
            playersAudiences(true).sendMessage(client);
        }
    }
    default void $logStackTrace(Throwable exception) {
        Throwable target = exception;
        while (target != null) {
            $logOP(Component.text(target.getClass().getName() + ": "  + target.getMessage()).color(NamedTextColor.RED));
            $logStackTrace(target.getStackTrace());
            target = target.getCause();
        }
    }
    default void $logStackTrace() {
        $logStackTrace(Thread.currentThread().getStackTrace());
    }
}
