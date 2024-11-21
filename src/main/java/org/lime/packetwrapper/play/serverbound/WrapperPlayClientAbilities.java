package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientAbilities extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.ABILITIES;

    public WrapperPlayClientAbilities() {
        super(TYPE);
    }

    public WrapperPlayClientAbilities(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'isFlying'
     *
     * @return 'isFlying'
     */
    public boolean getIsFlying() {
        return this.handle.getBooleans().read(0);
    }

    /**
     * Sets the value of field 'isFlying'
     *
     * @param value New value for field 'isFlying'
     */
    public void setIsFlying(boolean value) {
        this.handle.getBooleans().write(0, value);
    }

}
