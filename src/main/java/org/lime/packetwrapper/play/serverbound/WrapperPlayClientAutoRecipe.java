package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.MinecraftKey;

public class WrapperPlayClientAutoRecipe extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.AUTO_RECIPE;

    public WrapperPlayClientAutoRecipe() {
        super(TYPE);
    }

    public WrapperPlayClientAutoRecipe(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'containerId'
     *
     * @return 'containerId'
     */
    public int getContainerId() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'containerId'
     *
     * @param value New value for field 'containerId'
     */
    public void setContainerId(int value) {
        this.handle.getIntegers().write(0, value);
    }

    /**
     * Retrieves the value of field 'recipe'
     *
     * @return 'recipe'
     */
    public MinecraftKey getRecipe() {
        return this.handle.getMinecraftKeys().read(0);
    }

    /**
     * Sets the value of field 'recipe'
     *
     * @param value New value for field 'recipe'
     */
    public void setRecipe(MinecraftKey value) {
        this.handle.getMinecraftKeys().write(0, value);
    }

    /**
     * Retrieves the value of field 'shiftDown'
     *
     * @return 'shiftDown'
     */
    public boolean getShiftDown() {
        return this.handle.getBooleans().read(0);
    }

    /**
     * Sets the value of field 'shiftDown'
     *
     * @param value New value for field 'shiftDown'
     */
    public void setShiftDown(boolean value) {
        this.handle.getBooleans().write(0, value);
    }

}
