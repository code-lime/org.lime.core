package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.Hand;

public class WrapperPlayClientArmAnimation extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.ARM_ANIMATION;

    public WrapperPlayClientArmAnimation() {
        super(TYPE);
    }

    public WrapperPlayClientArmAnimation(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'hand'
     *
     * @return 'hand'
     */
    public Hand getHand() {
        return this.handle.getHands().read(0);
    }

    /**
     * Sets the value of field 'hand'
     *
     * @param value New value for field 'hand'
     */
    public void setHand(Hand value) {
        this.handle.getHands().write(0, value);
    }

}
