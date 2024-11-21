package org.lime.packetwrapper.login.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.Converters;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class WrapperLoginClientStart extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Login.Client.START;

    public WrapperLoginClientStart() {
        super(TYPE);
    }

    public WrapperLoginClientStart(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'name'
     *
     * @return 'name'
     */
    public String getName() {
        return this.handle.getStrings().read(0);
    }

    /**
     * Sets the value of field 'name'
     *
     * @param value New value for field 'name'
     */
    public void setName(String value) {
        this.handle.getStrings().write(0, value);
    }

    /**
     * Retrieves the value of field 'profileId'
     *
     * @return 'profileId'
     */
    public Optional<UUID> getProfileId() {
        return this.handle.getOptionals(Converters.passthrough(UUID.class)).read(0);
    }

    /**
     * Sets the value of field 'profileId'
     *
     * @param value New value for field 'profileId'
     */
    public void setProfileId(@Nullable UUID value) {
        this.handle.getOptionals(Converters.passthrough(UUID.class)).write(0, Optional.ofNullable(value));
    }

}
