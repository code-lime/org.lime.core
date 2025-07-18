package org.lime.core.common.api.commands.brigadier.exceptions;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.lime.core.common.system.execute.Func3;

// Generated by JavaScript (c) Lime
public record Generic3CommandExceptionType<T0, T1, T2>(
        Func3<T0, T1, T2, Message> function)
        implements CommandExceptionType {
    public CommandSyntaxException create(T0 val0, T1 val1, T2 val2) {
        return new CommandSyntaxException(this, function.invoke(val0, val1, val2));
    }
    public CommandSyntaxException createWithContext(T0 val0, T1 val1, T2 val2, ImmutableStringReader reader) {
        return new CommandSyntaxException(this, function.invoke(val0, val1, val2), reader.getString(), reader.getCursor());
    }
}