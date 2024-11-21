package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientTabComplete extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.TAB_COMPLETE;

    public WrapperPlayClientTabComplete() {
        super(TYPE);
    }

    public WrapperPlayClientTabComplete(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'id'
     *
     * @return 'id'
     */
    public int getId() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'id'
     *
     * @param value New value for field 'id'
     */
    public void setId(int value) {
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

}
