package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerAction;

public class WrapperPlayClientEntityAction extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.ENTITY_ACTION;

    public WrapperPlayClientEntityAction() {
        super(TYPE);
    }

    public WrapperPlayClientEntityAction(PacketContainer packet) {
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
     * Retrieves the value of field 'action'
     *
     * @return 'action'
     */
    public PlayerAction getAction() {
        return this.handle.getPlayerActions().read(0);
    }

    /**
     * Sets the value of field 'action'
     *
     * @param value New value for field 'action'
     */
    public void setAction(PlayerAction value) {
        this.handle.getPlayerActions().write(0, value);
    }

    /**
     * Retrieves the value of field 'data'
     *
     * @return 'data'
     */
    public int getData() {
        return this.handle.getIntegers().read(1);
    }

    /**
     * Sets the value of field 'data'
     *
     * @param value New value for field 'data'
     */
    public void setData(int value) {
        this.handle.getIntegers().write(1, value);
    }

}
