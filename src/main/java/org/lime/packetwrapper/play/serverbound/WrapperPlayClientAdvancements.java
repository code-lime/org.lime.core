package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.packetwrapper.util.UtilityMethod;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.MinecraftKey;

public class WrapperPlayClientAdvancements extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.ADVANCEMENTS;
    private static Class<?> ACTION_TYPE = null;

    public WrapperPlayClientAdvancements() {
        super(TYPE);
    }

    public WrapperPlayClientAdvancements(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'action'
     *
     * @return 'action'
     */
    public Action getAction() {
        return this.getActions().read(0);
    }

    /**
     * Sets the value of field 'action'
     *
     * @param value New value for field 'action'
     */
    public void setAction(Action value) {
        this.getActions().write(0, value);
    }

    /**
     * Retrieves the value of field 'tab'
     *
     * @return 'tab'
     */
    public MinecraftKey getTab() {
        return this.handle.getMinecraftKeys().read(0);
    }

    /**
     * Sets the value of field 'tab'
     *
     * @param value New value for field 'tab'
     */
    public void setTab(MinecraftKey value) {
        this.handle.getMinecraftKeys().write(0, value);
    }

    public enum Action {
        OPENED_TAB,
        CLOSED_SCREEN;
    }

    @UtilityMethod
    private StructureModifier<Action> getActions() {
        if(ACTION_TYPE == null) {
            ACTION_TYPE = handle.getHandle().getClass().getDeclaredClasses()[0];
        }
        return this.handle.getModifier().withType(ACTION_TYPE, new EnumWrappers.IndexedEnumConverter<>(Action.class, ACTION_TYPE));
    }

}
