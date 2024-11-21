package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class WrapperPlayClientPosition extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.POSITION;

    public WrapperPlayClientPosition() {
        super(TYPE);
    }

    public WrapperPlayClientPosition(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'x'
     *
     * @return 'x'
     */
    public double getX() {
        return this.handle.getDoubles().read(0);
    }

    /**
     * Sets the value of field 'x'
     *
     * @param value New value for field 'x'
     */
    public void setX(double value) {
        this.handle.getDoubles().write(0, value);
    }

    /**
     * Retrieves the value of field 'y'
     *
     * @return 'y'
     */
    public double getY() {
        return this.handle.getDoubles().read(1);
    }

    /**
     * Sets the value of field 'y'
     *
     * @param value New value for field 'y'
     */
    public void setY(double value) {
        this.handle.getDoubles().write(1, value);
    }

    /**
     * Retrieves the value of field 'z'
     *
     * @return 'z'
     */
    public double getZ() {
        return this.handle.getDoubles().read(2);
    }

    /**
     * Sets the value of field 'z'
     *
     * @param value New value for field 'z'
     */
    public void setZ(double value) {
        this.handle.getDoubles().write(2, value);
    }

    /**
     * Retrieves the value of field 'yRot'
     *
     * @return 'yRot'
     */
    public float getYRot() {
        return this.handle.getFloat().read(0);
    }

    /**
     * Sets the value of field 'yRot'
     *
     * @param value New value for field 'yRot'
     */
    public void setYRot(float value) {
        this.handle.getFloat().write(0, value);
    }

    /**
     * Retrieves the value of field 'xRot'
     *
     * @return 'xRot'
     */
    public float getXRot() {
        return this.handle.getFloat().read(1);
    }

    /**
     * Sets the value of field 'xRot'
     *
     * @param value New value for field 'xRot'
     */
    public void setXRot(float value) {
        this.handle.getFloat().write(1, value);
    }

    /**
     * Retrieves the value of field 'hasPos'
     *
     * @return 'hasPos'
     */
    public boolean getHasPos() {
        return this.handle.getBooleans().read(0);
    }

    /**
     * Sets the value of field 'hasPos'
     *
     * @param value New value for field 'hasPos'
     */
    public void setHasPos(boolean value) {
        this.handle.getBooleans().write(0, value);
    }

    /**
     * Retrieves the value of field 'hasRot'
     *
     * @return 'hasRot'
     */
    public boolean getHasRot() {
        return this.handle.getBooleans().read(1);
    }

    /**
     * Sets the value of field 'hasRot'
     *
     * @param value New value for field 'hasRot'
     */
    public void setHasRot(boolean value) {
        this.handle.getBooleans().write(1, value);
    }

}
