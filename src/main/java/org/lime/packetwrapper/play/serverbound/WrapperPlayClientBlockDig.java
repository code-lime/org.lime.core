package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers.Direction;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerDigType;

public class WrapperPlayClientBlockDig extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.BLOCK_DIG;

    public WrapperPlayClientBlockDig() {
        super(TYPE);
    }

    public WrapperPlayClientBlockDig(PacketContainer packet) {
        super(packet, TYPE);
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

    /**
     * Retrieves the value of field 'direction'
     *
     * @return 'direction'
     */
    public Direction getDirection() {
        return this.handle.getDirections().read(0);
    }

    /**
     * Sets the value of field 'direction'
     *
     * @param value New value for field 'direction'
     */
    public void setDirection(Direction value) {
        this.handle.getDirections().write(0, value);
    }

    /**
     * Retrieves the value of field 'action'
     *
     * @return 'action'
     */
    public PlayerDigType getAction() {
        return this.handle.getPlayerDigTypes().read(0);
    }

    /**
     * Sets the value of field 'action'
     *
     * @param value New value for field 'action'
     */
    public void setAction(PlayerDigType value) {
        this.handle.getPlayerDigTypes().write(0, value);
    }

    /**
     * Retrieves the value of field 'sequence'
     *
     * @return 'sequence'
     */
    public int getSequence() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'sequence'
     *
     * @param value New value for field 'sequence'
     */
    public void setSequence(int value) {
        this.handle.getIntegers().write(0, value);
    }

}
