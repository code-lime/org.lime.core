package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientTrSel extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.TR_SEL;

    public WrapperPlayClientTrSel() {
        super(TYPE);
    }

    public WrapperPlayClientTrSel(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'item'
     *
     * @return 'item'
     */
    public int getItem() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'item'
     *
     * @param value New value for field 'item'
     */
    public void setItem(int value) {
        this.handle.getIntegers().write(0, value);
    }

}
