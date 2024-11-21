package org.lime.packetwrapper.data;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AutoWrapper;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import javax.annotation.Nullable;
import java.awt.image.ColorModel;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Lukas Alt
 * @since 17.05.2023
 */
public class WrappedBoundChatType {
    public static final Class<?> HANDLE_TYPE = MinecraftReflection.getMinecraftClass("network.chat.ChatType$BoundNetwork", "network.chat.ChatMessageType$b");
    public static final EquivalentConverter<WrappedBoundChatType> CONVERTER = new EquivalentConverter<>() {
        private static final ConstructorAccessor CONSTRUCTOR_ACCESSOR = Accessors.getConstructorAccessor(HANDLE_TYPE, int.class, MinecraftReflection.getIChatBaseComponentClass(), MinecraftReflection.getIChatBaseComponentClass());

        @Override
        public Object getGeneric(WrappedBoundChatType specific) {
            return CONSTRUCTOR_ACCESSOR.invoke(
                    specific.chatType,
                    BukkitConverters.getWrappedChatComponentConverter().getGeneric(specific.component),
                    BukkitConverters.getWrappedChatComponentConverter().getGeneric(specific.targetName)
            );
        }

        @Override
        public WrappedBoundChatType getSpecific(Object generic) {
            StructureModifier<?> modifier = new StructureModifier<>(HANDLE_TYPE).withTarget(generic);
            return new WrappedBoundChatType(
                    (Integer) modifier.withType(int.class).read(0),
                    modifier.withType(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter()).read(0),
                    modifier.withType(MinecraftReflection.getIChatBaseComponentClass(), BukkitConverters.getWrappedChatComponentConverter()).read(1)
            );
        }

        @Override
        public Class<WrappedBoundChatType> getSpecificType() {
            return WrappedBoundChatType.class;
        }
    };

    private int chatType;
    private WrappedChatComponent component;
    private WrappedChatComponent targetName;

    public WrappedBoundChatType(int chatType, WrappedChatComponent component, @Nullable WrappedChatComponent targetName) {
        this.chatType = chatType;
        this.component = component;
        this.targetName = targetName;
    }

    public WrappedBoundChatType() {
    }

    public int getChatType() {
        return chatType;
    }

    public void setChatType(int chatType) {
        this.chatType = chatType;
    }

    public WrappedChatComponent getComponent() {
        return component;
    }

    public void setComponent(WrappedChatComponent component) {
        this.component = component;
    }

    @Nullable
    public WrappedChatComponent getTargetName() {
        return targetName;
    }

    public void setTargetName(@Nullable WrappedChatComponent targetName) {
        this.targetName = targetName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WrappedBoundChatType that = (WrappedBoundChatType) o;

        if (chatType != that.chatType) return false;
        if (!Objects.equals(component, that.component)) return false;
        return Objects.equals(targetName, that.targetName);
    }

    @Override
    public int hashCode() {
        int result = chatType;
        result = 31 * result + (component != null ? component.hashCode() : 0);
        result = 31 * result + (targetName != null ? targetName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BoundChatType{" +
                "chatType=" + chatType +
                ", component=" + component +
                ", targetName=" + targetName +
                '}';
    }
}
