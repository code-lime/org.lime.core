package org.lime.packetwrapper.play.clientbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedLevelChunkData.ChunkData;
import com.comphenix.protocol.wrappers.WrappedLevelChunkData.LightData;

public class WrapperPlayServerMapChunk extends AbstractPacket {

    /**
     * The packet type that is wrapped by this wrapper.
     */
    public static final PacketType TYPE = PacketType.Play.Server.MAP_CHUNK;

    /**
     * Constructs a new wrapper and initialize it with a packet handle with default values
     */
    public WrapperPlayServerMapChunk() {
        super(TYPE);
    }

    /**
     * Constructors a new wrapper for the specified packet
     *
     * @param packet the packet to wrap
     */
    public WrapperPlayServerMapChunk(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'x'
     *
     * @return 'x'
     */
    public int getX() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'x'
     *
     * @param value New value for field 'x'
     */
    public void setX(int value) {
        this.handle.getIntegers().write(0, value);
    }

    /**
     * Retrieves the value of field 'z'
     *
     * @return 'z'
     */
    public int getZ() {
        return this.handle.getIntegers().read(1);
    }

    /**
     * Sets the value of field 'z'
     *
     * @param value New value for field 'z'
     */
    public void setZ(int value) {
        this.handle.getIntegers().write(1, value);
    }

    /**
     * Retrieves the value of field 'chunkData'
     *
     * @return 'chunkData'
     */
    public ChunkData getChunkData() {
        return this.handle.getLevelChunkData().read(0);
    }

    /**
     * Sets the value of field 'chunkData'
     *
     * @param value New value for field 'chunkData'
     */
    public void setChunkData(ChunkData value) {
        this.handle.getLevelChunkData().write(0, value);
    }

    /**
     * Retrieves the value of field 'lightData'
     *
     * @return 'lightData'
     */
    public LightData getLightData() {
        return this.handle.getLightUpdateData().read(0);
    }

    /**
     * Sets the value of field 'lightData'
     *
     * @param value New value for field 'lightData'
     */
    public void setLightData(LightData value) {
        this.handle.getLightUpdateData().write(0, value);
    }

}
