package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.packetwrapper.util.TestExclusion;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedRemoteChatSessionData;

public class WrapperPlayClientChatSessionUpdate extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.CHAT_SESSION_UPDATE;

    public WrapperPlayClientChatSessionUpdate() {
        super(TYPE);
    }

    public WrapperPlayClientChatSessionUpdate(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'chatSession'
     *
     * @return 'chatSession'
     */
    public WrappedRemoteChatSessionData getChatSession() {
        return this.handle.getRemoteChatSessionData().read(0);
    }

    /**
     * Sets the value of field 'chatSession'
     *
     * @param value New value for field 'chatSession'
     */
    @TestExclusion
    public void setChatSession(WrappedRemoteChatSessionData value) {
        this.handle.getRemoteChatSessionData().write(0, value);
    }

}
