package org.lime.packetwrapper.play.clientbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

/**
 * @deprecated Unused packet
 */
@Deprecated
public class WrapperPlayServerPlayerCombatEnter extends AbstractPacket {

    /**
     * The packet type that is wrapped by this wrapper.
     */
    public static final PacketType TYPE = PacketType.Play.Server.PLAYER_COMBAT_ENTER;

    /**
     * Constructs a new wrapper and initialize it with a packet handle with default values
     */
    public WrapperPlayServerPlayerCombatEnter() {
        super(TYPE);
    }

    public WrapperPlayServerPlayerCombatEnter(PacketContainer packet) {
        super(packet, TYPE);
    }


}
