package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientBoatMove extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.BOAT_MOVE;

    public WrapperPlayClientBoatMove() {
        super(TYPE);
    }

    public WrapperPlayClientBoatMove(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'left'
     *
     * @return 'left'
     */
    public boolean getLeft() {
        return this.handle.getBooleans().read(0);
    }

    /**
     * Sets the value of field 'left'
     *
     * @param value New value for field 'left'
     */
    public void setLeft(boolean value) {
        this.handle.getBooleans().write(0, value);
    }

    /**
     * Retrieves the value of field 'right'
     *
     * @return 'right'
     */
    public boolean getRight() {
        return this.handle.getBooleans().read(1);
    }

    /**
     * Sets the value of field 'right'
     *
     * @param value New value for field 'right'
     */
    public void setRight(boolean value) {
        this.handle.getBooleans().write(1, value);
    }

}
