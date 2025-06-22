package org.lime.core.common;

import org.lime.core.common.api.*;
import org.lime.core.common.api.commands.BaseCoreCommandRegister;
import org.lime.core.common.api.elements.BaseCoreElement;
import org.lime.core.common.system.Lazy;

import java.util.*;
import java.util.stream.Stream;

public abstract class BaseCoreInstance<
        Command extends BaseCoreCommandRegister<Self, Command>,
        Self extends BaseCoreInstance<Command, Self>>
        extends BaseCoreLoader<Command, Self> {
    public static BaseGlobal global;

    @Override
    protected void setBaseCore(Self baseCore) {
        global = baseCore;
    }

    private final Lazy<String> logPrefixLazy = Lazy.of(() -> {
        String[] args = this.name().toUpperCase().split("[ ./]");
        if (args.length == 1)
            return args[0];

        StringBuilder result = new StringBuilder();

        int lastIndex = args.length - 1;
        for (int i = 0; i < lastIndex; i++)
            if (!args[i].isEmpty())
                result.append(args[i].charAt(0));

        return result
                .append(":")
                .append(args[lastIndex])
                .toString();
    });
    @Override public String logPrefix() { return logPrefixLazy.value(); }
    @Override public String configFile() { return "config/" + this.name().toLowerCase() + "/"; }

    @Override
    public <T, Element extends BaseCoreElement<T, Command, Self, Element>> CoreElementLoaded<T, Element> addElement(Element element) {
        if (element.disable)
            return CoreElementLoaded.disabled(element);
        elements.add(element);
        if (element.instance instanceof ElementInstance instance)
            instance.core(this);
        return new CoreElementLoaded<>() {
            @Override public void cancel() { if (elements.remove(element) && element.uninit != null) element.uninit.invoke(); }
            @Override public Optional<Element> element() { return Optional.of(element); }
            @Override public String name() { return element.name; }
            @Override public Class<T> type() { return element.tClass; }
        };
    }
    @Override
    public void addCommand(Command command) {
        commands.put(command.cmd(), command);
    }
    @Override
    public Stream<BaseCoreElement<?, Command, Self, ?>> elements() {
        return this.elements.stream();
    }
    @Override
    public Map<String, Command> commands() {
        return this.commands;
    }

    @Override protected Optional<ElementUpdateConfigInstance> config() {
        return Stream.concat(Stream.of(this), elements.stream().map(v -> v.instance))
                .map(v -> v instanceof ElementUpdateConfigInstance config ? config : null)
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Override protected void init() {}
}
