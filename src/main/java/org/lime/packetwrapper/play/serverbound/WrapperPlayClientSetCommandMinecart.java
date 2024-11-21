package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientSetCommandMinecart extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.SET_COMMAND_MINECART;

    public WrapperPlayClientSetCommandMinecart() {
        super(TYPE);
    }

    public WrapperPlayClientSetCommandMinecart(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'entity'
     *
     * @return 'entity'
     */
    public int getEntity() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'entity'
     *
     * @param value New value for field 'entity'
     */
    public void setEntity(int value) {
        this.handle.getIntegers().write(0, value);
    }

    /**
     * Retrieves the value of field 'command'
     *
     * @return 'command'
     */
    public String getCommand() {
        return this.handle.getStrings().read(0);
    }

    /**
     * Sets the value of field 'command'
     *
     * @param value New value for field 'command'
     */
    public void setCommand(String value) {
        this.handle.getStrings().write(0, value);
    }

    /**
     * Retrieves the value of field 'trackOutput'
     *
     * @return 'trackOutput'
     */
    public boolean getTrackOutput() {
        return this.handle.getBooleans().read(0);
    }

    /**
     * Sets the value of field 'trackOutput'
     *
     * @param value New value for field 'trackOutput'
     */
    public void setTrackOutput(boolean value) {
        this.handle.getBooleans().write(0, value);
    }

}
