package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;

public class WrapperPlayClientJigsawGenerate extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.JIGSAW_GENERATE;

    public WrapperPlayClientJigsawGenerate() {
        super(TYPE);
    }

    public WrapperPlayClientJigsawGenerate(PacketContainer packet) {
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
     * Retrieves the value of field 'levels'
     *
     * @return 'levels'
     */
    public int getLevels() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'levels'
     *
     * @param value New value for field 'levels'
     */
    public void setLevels(int value) {
        this.handle.getIntegers().write(0, value);
    }

    /**
     * Retrieves the value of field 'keepJigsaws'
     *
     * @return 'keepJigsaws'
     */
    public boolean getKeepJigsaws() {
        return this.handle.getBooleans().read(0);
    }

    /**
     * Sets the value of field 'keepJigsaws'
     *
     * @param value New value for field 'keepJigsaws'
     */
    public void setKeepJigsaws(boolean value) {
        this.handle.getBooleans().write(0, value);
    }

}
