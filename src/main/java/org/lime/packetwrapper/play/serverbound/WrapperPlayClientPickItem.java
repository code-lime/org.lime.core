package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientPickItem extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.PICK_ITEM;

    public WrapperPlayClientPickItem() {
        super(TYPE);
    }

    public WrapperPlayClientPickItem(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'slot'
     *
     * @return 'slot'
     */
    public int getSlot() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'slot'
     *
     * @param value New value for field 'slot'
     */
    public void setSlot(int value) {
        this.handle.getIntegers().write(0, value);
    }

}
