package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;

public class WrapperPlayClientTileNbtQuery extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.TILE_NBT_QUERY;

    public WrapperPlayClientTileNbtQuery() {
        super(TYPE);
    }

    public WrapperPlayClientTileNbtQuery(PacketContainer packet) {
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
     * Retrieves the value of field 'pos'
     *
     * @return 'pos'
     */
    public BlockPosition getPos() {
        return this.handle.getBlockPositionModifier().read(0);
    }

    /**
     * Sets the value of field 'pos'
     *
     * @param value New value for field 'pos'
     */
    public void setPos(BlockPosition value) {
        this.handle.getBlockPositionModifier().write(0, value);
    }

}
