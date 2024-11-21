package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientChatAck extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.CHAT_ACK;

    public WrapperPlayClientChatAck() {
        super(TYPE);
    }

    public WrapperPlayClientChatAck(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'offset'
     *
     * @return 'offset'
     */
    public int getOffset() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'offset'
     *
     * @param value New value for field 'offset'
     */
    public void setOffset(int value) {
        this.handle.getIntegers().write(0, value);
    }

}
