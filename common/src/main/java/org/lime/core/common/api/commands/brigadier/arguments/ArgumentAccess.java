package org.lime.core.common.api.commands.brigadier.arguments;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.lime.core.common.reflection.Reflection;
import org.lime.core.common.utils.execute.Func1;

public interface ArgumentAccess<T> {
    T get() throws CommandSyntaxException;
    default T throwableGet() {
        try {
            return get();
        } catch (CommandSyntaxException e) {
            throw Reflection.sneakyThrow(e);
        }
    }

    default  <J>ArgumentAccess<J> map(Func1<T, J> mapper) {
        return () -> mapper.invoke(ArgumentAccess.this.get());
    }
}
