package org.lime.packetwrapper.login.clientbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperLoginServerSetCompression extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Login.Server.SET_COMPRESSION;

    public WrapperLoginServerSetCompression() {
        super(TYPE);
    }

    public WrapperLoginServerSetCompression(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'compressionThreshold'
     *
     * @return 'compressionThreshold'
     */
    public int getCompressionThreshold() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'compressionThreshold'
     *
     * @param value New value for field 'compressionThreshold'
     */
    public void setCompressionThreshold(int value) {
        this.handle.getIntegers().write(0, value);
    }

}
