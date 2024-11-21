package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientEntityNbtQuery extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.ENTITY_NBT_QUERY;

    public WrapperPlayClientEntityNbtQuery() {
        super(TYPE);
    }

    public WrapperPlayClientEntityNbtQuery(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'transactionId'
     *
     * @return 'transactionId'
     */
    public int getTransactionId() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'transactionId'
     *
     * @param value New value for field 'transactionId'
     */
    public void setTransactionId(int value) {
        this.handle.getIntegers().write(0, value);
    }

    /**
     * Retrieves the value of field 'entityId'
     *
     * @return 'entityId'
     */
    public int getEntityId() {
        return this.handle.getIntegers().read(1);
    }

    /**
     * Sets the value of field 'entityId'
     *
     * @param value New value for field 'entityId'
     */
    public void setEntityId(int value) {
        this.handle.getIntegers().write(1, value);
    }

}
