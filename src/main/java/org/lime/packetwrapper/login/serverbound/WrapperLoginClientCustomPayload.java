package org.lime.packetwrapper.login.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.packetwrapper.util.TestExclusion;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import io.netty.buffer.ByteBuf;

public class WrapperLoginClientCustomPayload extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Login.Client.CUSTOM_PAYLOAD;

    public WrapperLoginClientCustomPayload() {
        super(TYPE);
    }

    public WrapperLoginClientCustomPayload(PacketContainer packet) {
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
     * Retrieves the value of field 'data'
     *
     * @return 'data'
     */
    @TestExclusion
    public ByteBuf getData() {
        return this.handle.getStructures().read(0).getModifier().<ByteBuf>withType(ByteBuf.class).read(0);
    }

    /**
     * Sets the value of field 'data'
     *
     * @param value New value for field 'data'
     */
    public void setData(ByteBuf value) {
        InternalStructure structure = InternalStructure.getConverter().getSpecific(MinecraftReflection.getPacketDataSerializer(value));
        this.handle.getStructures().write(0, structure);
    }

}
