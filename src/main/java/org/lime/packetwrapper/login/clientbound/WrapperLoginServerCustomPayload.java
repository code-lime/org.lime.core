package org.lime.packetwrapper.login.clientbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.packetwrapper.util.TestExclusion;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.MinecraftKey;
import io.netty.buffer.ByteBuf;

public class WrapperLoginServerCustomPayload extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Login.Server.CUSTOM_PAYLOAD;

    public WrapperLoginServerCustomPayload() {
        super(TYPE);
    }

    public WrapperLoginServerCustomPayload(PacketContainer packet) {
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
     * Retrieves the value of field 'identifier'
     *
     * @return 'identifier'
     */
    public MinecraftKey getIdentifier() {
        return this.handle.getMinecraftKeys().read(0);
    }

    /**
     * Sets the value of field 'identifier'
     *
     * @param value New value for field 'identifier'
     */
    public void setIdentifier(MinecraftKey value) {
        this.handle.getMinecraftKeys().write(0, value);
    }

    /**
     * Retrieves the value of field 'data'
     *
     * @return 'data'
     */
    @TestExclusion
    public ByteBuf getData() {
        return this.handle.getStructures().read(1).getModifier().<ByteBuf>withType(ByteBuf.class).read(0);
    }

    /**
     * Sets the value of field 'data'
     *
     * @param value New value for field 'data'
     */
    public void setData(ByteBuf value) {
        InternalStructure structure = InternalStructure.getConverter().getSpecific(MinecraftReflection.getPacketDataSerializer(value));
        this.handle.getStructures().write(1, structure);
    }

}
