package org.lime.core.common.api.commands.brigadier.exceptions;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

public interface SyntaxPredicate<S> {
    boolean test(S s) throws CommandSyntaxException;
}
