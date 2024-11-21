package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.Converters;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class WrapperPlayClientBEdit extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.B_EDIT;

    public WrapperPlayClientBEdit() {
        super(TYPE);
    }

    public WrapperPlayClientBEdit(PacketContainer packet) {
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

    /**
     * Retrieves the value of field 'pages'
     *
     * @return 'pages'
     */
    public List<String> getPages() {
        return this.handle.getLists(Converters.passthrough(String.class)).read(0);
    }

    /**
     * Sets the value of field 'pages'
     *
     * @param value New value for field 'pages'
     */
    public void setPages(List<String> value) {
        this.handle.getLists(Converters.passthrough(String.class)).write(0, value);
    }

    /**
     * Retrieves the value of field 'title'
     *
     * @return 'title'
     */
    public Optional<String> getTitle() {
        return this.handle.getOptionals(Converters.passthrough(String.class)).read(0);
    }

    /**
     * Sets the value of field 'title'
     *
     * @param value New value for field 'title'
     */
    public void setTitle(@Nullable String value) {
        this.handle.getOptionals(Converters.passthrough(String.class)).write(0, Optional.ofNullable(value));
    }

}
