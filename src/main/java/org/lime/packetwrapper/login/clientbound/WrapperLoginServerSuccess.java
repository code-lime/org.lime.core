package org.lime.packetwrapper.login.clientbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedGameProfile;

public class WrapperLoginServerSuccess extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Login.Server.SUCCESS;

    public WrapperLoginServerSuccess() {
        super(TYPE);
    }

    public WrapperLoginServerSuccess(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'gameProfile'
     *
     * @return 'gameProfile'
     */
    public WrappedGameProfile getGameProfile() {
        return this.handle.getGameProfiles().read(0);
    }

    /**
     * Sets the value of field 'gameProfile'
     *
     * @param value New value for field 'gameProfile'
     */
    public void setGameProfile(WrappedGameProfile value) {
        this.handle.getGameProfiles().write(0, value);
    }

}
