package org.lime.plugin;

import com.google.common.collect.Lists;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public interface ILogger {
    String getLogPrefix();

    default void _logToFile(String key, String text) {
        final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        String time = formatter.format(calendar.getTime());

        if (text.contains("{time}"))
        {
            final DateFormat time_formatter = new SimpleDateFormat("HH:mm:ss");
            calendar.setTimeInMillis(System.currentTimeMillis());
            text = text.replace("{time}", time_formatter.format(calendar.getTime()));
        }

        try {
            String path = "logs/lime/" + key + "/";
            File logs = new File(path);
            if (!logs.exists()) logs.mkdirs();

            File myObj = new File(path + key + "-" + time + ".log");
            if (!myObj.exists()) myObj.createNewFile();

            FileWriter myWriter = new FileWriter(myObj, true);
            myWriter.write(text + "\r\n");
            myWriter.close();
        } catch (FileNotFoundException e) {
            _log("An error occurred.");
            _log(e.getMessage());
        } catch (Exception e)
        {
            _log(e.getMessage());
        }
    }
    default void _log(String log) {
        Bukkit.getLogger().warning("["+getLogPrefix()+"] " + log);
    }
    default void _logAdmin(String log) {
        _logToFile("log_admin", "[{time}] " + log);
        _log(log);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage("["+getLogPrefix()+"] " + log));
    }
    default void _logConsole(String log) {
        _logToFile("log_admin", "[{time}] " + log);
        _log(log);
    }
    default void _logOP(String log) {
        _logToFile("log_admin", "[{time}] " + log);
        _log(log);
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.isOp()) return;
            p.sendMessage(Component.text("["+getLogPrefix()+"] ").color(NamedTextColor.YELLOW).append(Component.text(log).color(NamedTextColor.WHITE)));
        });
    }
    default void _logOP(Component log) {
        String legacy_log = LegacyComponentSerializer.legacySection().serialize(log);
        _logToFile("log_admin", "[{time}] " + legacy_log);
        _log(legacy_log);
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.isOp()) return;
            p.sendMessage(Component.text("["+getLogPrefix()+"] ").color(NamedTextColor.YELLOW).append(Component.empty().append(log).color(NamedTextColor.WHITE)));
        });
    }
    default void _logWithoutPrefix(String log) {
        Bukkit.getLogger().warning(log);
    }

    private void _logStackTraceSub(List<StackTraceElement> stackTraceElements) {
        Component client = Component.empty();
        List<String> lines = new ArrayList<>();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            String line = stackTraceElement.toString();
            _log(line);
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp())
                player.sendMessage(client);
        }
    }
    default void _logStackTrace(StackTraceElement[] stackTraceElements) {
        if (stackTraceElements.length > 30) {
            Component client = Component.text("["+getLogPrefix()+"] ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(" StackTraceList:"));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp())
                    player.sendMessage(client);
            }
            for (List<StackTraceElement> lines : Lists.partition(Arrays.asList(stackTraceElements), 30)) {
                _logStackTraceSub(lines);
            }
        } else {
            Component client = Component.empty();
            List<String> lines = new ArrayList<>();
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                String line = stackTraceElement.toString();
                _log(line);
                lines.add(line);
                client = client.append(Component.text("  " + line));
            }
            client = Component.text("["+getLogPrefix()+"] ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text("[StackTrace]")
                            .hoverEvent(HoverEvent.showText(client))
                            .clickEvent(ClickEvent.copyToClipboard(String.join("\n", lines)))
                            .color(NamedTextColor.GREEN)
                    );
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp())
                    player.sendMessage(client);
            }
        }
    }
    default void _logStackTrace(Throwable exception) {
        Throwable target = exception;
        while (target != null) {
            _logOP(ChatColor.RED + target.getMessage());
            _logStackTrace(target.getStackTrace());
            target = target.getCause();
        }
    }
    default void _logStackTrace() {
        _logStackTrace(Thread.currentThread().getStackTrace());
    }
}

















