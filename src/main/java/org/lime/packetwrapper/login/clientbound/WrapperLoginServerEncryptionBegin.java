package org.lime.packetwrapper.login.clientbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperLoginServerEncryptionBegin extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Login.Server.ENCRYPTION_BEGIN;

    public WrapperLoginServerEncryptionBegin() {
        super(TYPE);
    }

    public WrapperLoginServerEncryptionBegin(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'serverId'
     *
     * @return 'serverId'
     */
    public String getServerId() {
        return this.handle.getStrings().read(0);
    }

    /**
     * Sets the value of field 'serverId'
     *
     * @param value New value for field 'serverId'
     */
    public void setServerId(String value) {
        this.handle.getStrings().write(0, value);
    }

    /**
     * Retrieves the value of field 'publicKey'
     *
     * @return 'publicKey'
     */
    public byte[] getPublicKey() {
        return this.handle.getByteArrays().read(0);
    }

    /**
     * Sets the value of field 'publicKey'
     *
     * @param value New value for field 'publicKey'
     */
    public void setPublicKey(byte[] value) {
        this.handle.getByteArrays().write(0, value);
    }

    /**
     * Retrieves the value of field 'challenge'
     *
     * @return 'challenge'
     */
    public byte[] getChallenge() {
        return this.handle.getByteArrays().read(1);
    }

    /**
     * Sets the value of field 'challenge'
     *
     * @param value New value for field 'challenge'
     */
    public void setChallenge(byte[] value) {
        this.handle.getByteArrays().write(1, value);
    }

}
