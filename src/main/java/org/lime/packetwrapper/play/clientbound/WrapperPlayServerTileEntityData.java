package org.lime.packetwrapper.play.clientbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedRegistrable;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;

public class WrapperPlayServerTileEntityData extends AbstractPacket {

    /**
     * The packet type that is wrapped by this wrapper.
     */
    public static final PacketType TYPE = PacketType.Play.Server.TILE_ENTITY_DATA;

    /**
     * Constructs a new wrapper and initialize it with a packet handle with default values
     */
    public WrapperPlayServerTileEntityData() {
        super(TYPE);
    }

    /**
     * Constructors a new wrapper for the specified packet
     *
     * @param packet the packet to wrap
     */
    public WrapperPlayServerTileEntityData(PacketContainer packet) {
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
     * Retrieves the value of field 'type'
     *
     * @return 'type'
     */
    public WrappedRegistrable getType() {
        return this.handle.getBlockEntityTypeModifier().read(0);
    }

    /**
     * Sets the value of field 'type'
     *
     * @param value New value for field 'type'
     */
    public void setType(WrappedRegistrable value) {
        this.handle.getBlockEntityTypeModifier().write(0, value);
    }

    /**
     * Retrieves the value of field 'tag'
     *
     * @return 'tag'
     */
    public NbtCompound getTag() {
        return (NbtCompound) this.handle.getNbtModifier().read(0);
    }

    /**
     * Sets the value of field 'tag'
     *
     * @param value New value for field 'tag'
     */
    public void setTag(NbtCompound value) {
        this.handle.getNbtModifier().write(0, value);
    }

}
