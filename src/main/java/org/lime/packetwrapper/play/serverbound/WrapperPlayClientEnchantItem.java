package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientEnchantItem extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.ENCHANT_ITEM;

    public WrapperPlayClientEnchantItem() {
        super(TYPE);
    }

    public WrapperPlayClientEnchantItem(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'containerId'
     *
     * @return 'containerId'
     */
    public int getContainerId() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'containerId'
     *
     * @param value New value for field 'containerId'
     */
    public void setContainerId(int value) {
        this.handle.getIntegers().write(0, value);
    }

    /**
     * Retrieves the value of field 'buttonId'
     *
     * @return 'buttonId'
     */
    public int getButtonId() {
        return this.handle.getIntegers().read(1);
    }

    /**
     * Sets the value of field 'buttonId'
     *
     * @param value New value for field 'buttonId'
     */
    public void setButtonId(int value) {
        this.handle.getIntegers().write(1, value);
    }

}
