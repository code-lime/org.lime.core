package org.lime.packetwrapper.login.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperLoginClientEncryptionBegin extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Login.Client.ENCRYPTION_BEGIN;

    public WrapperLoginClientEncryptionBegin() {
        super(TYPE);
    }

    public WrapperLoginClientEncryptionBegin(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'keybytes'
     *
     * @return 'keybytes'
     */
    public byte[] getKeyBytes() {
        return this.handle.getByteArrays().read(0);
    }

    /**
     * Sets the value of field 'keybytes'
     *
     * @param value New value for field 'keybytes'
     */
    public void setKeyBytes(byte[] value) {
        this.handle.getByteArrays().write(0, value);
    }

    /**
     * Retrieves the value of field 'encryptedChallenge'
     *
     * @return 'encryptedChallenge'
     */
    public byte[] getEncryptedChallenge() {
        return this.handle.getByteArrays().read(1);
    }

    /**
     * Sets the value of field 'encryptedChallenge'
     *
     * @param value New value for field 'encryptedChallenge'
     */
    public void setEncryptedChallenge(byte[] value) {
        this.handle.getByteArrays().write(1, value);
    }

}
