package org.lime.core.paper.commands;

import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.minecraft.commands.arguments.NbtTagArgument;
import org.junit.jupiter.api.Test;
import org.lime.core.common.api.commands.brigadier.arguments.JsonInput;
import org.lime.core.common.reflection.ReflectionMethod;
import org.lime.core.common.utils.execute.Func1;

import static org.junit.jupiter.api.Assertions.*;

class SnbtJsonArgumentTest {
    @Test
    void parsesOneVanillaSnbtValueAndLeavesFollowingLiteral() throws Exception {
        ArgumentType<JsonElement> argument = NativeCommandConsumerFactory.INSTANCE.json(JsonInput.raw(), new Gson().getAdapter(JsonElement.class));
        CustomArgumentType<?, ?> customArgument = assertInstanceOf(CustomArgumentType.class, argument);
        ArgumentType<?> nativeType = customArgument.getNativeType();
        assertEquals("io.papermc.paper.command.brigadier.argument.VanillaArgumentProviderImpl$NativeWrapperArgumentType", nativeType.getClass().getName());
        Func1<Object, ArgumentType<?>> nativeTypeAccess = ReflectionMethod.of(nativeType.getClass(), "nativeNmsArgumentType").lambda(Func1.class);
        ArgumentType<?> nmsType = nativeTypeAccess.invoke(nativeType);
        assertInstanceOf(NbtTagArgument.class, nmsType);
        StringReader reader = new StringReader("{enabled:true} apply");

        JsonElement value = argument.parse(reader);

        assertEquals(1, value.getAsJsonObject().get("enabled").getAsInt());
        assertEquals(" apply", reader.getRemaining());
    }
}
