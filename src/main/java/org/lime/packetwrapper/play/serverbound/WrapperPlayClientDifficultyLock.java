package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientDifficultyLock extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.DIFFICULTY_LOCK;

    public WrapperPlayClientDifficultyLock() {
        super(TYPE);
    }

    public WrapperPlayClientDifficultyLock(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'locked'
     *
     * @return 'locked'
     */
    public boolean getLocked() {
        return this.handle.getBooleans().read(0);
    }

    /**
     * Sets the value of field 'locked'
     *
     * @param value New value for field 'locked'
     */
    public void setLocked(boolean value) {
        this.handle.getBooleans().write(0, value);
    }

}
