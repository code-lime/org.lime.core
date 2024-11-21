package org.lime.packetwrapper.status.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperStatusClientPing extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Status.Client.PING;

    public WrapperStatusClientPing() {
        super(TYPE);
    }

    public WrapperStatusClientPing(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'time'
     *
     * @return 'time'
     */
    public long getTime() {
        return this.handle.getLongs().read(0);
    }

    /**
     * Sets the value of field 'time'
     *
     * @param value New value for field 'time'
     */
    public void setTime(long value) {
        this.handle.getLongs().write(0, value);
    }

}
