package org.lime.packetwrapper.status.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperStatusClientStart extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Status.Client.START;

    public WrapperStatusClientStart() {
        super(TYPE);
    }

    public WrapperStatusClientStart(PacketContainer packet) {
        super(packet, TYPE);
    }


}
