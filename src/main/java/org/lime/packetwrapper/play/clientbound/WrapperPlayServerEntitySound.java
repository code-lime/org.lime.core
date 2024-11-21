package org.lime.packetwrapper.play.clientbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.EnumWrappers.SoundCategory;
import org.bukkit.Sound;

public class WrapperPlayServerEntitySound extends AbstractPacket {

    /**
     * The packet type that is wrapped by this wrapper.
     */
    public static final PacketType TYPE = PacketType.Play.Server.ENTITY_SOUND;

    /**
     * Constructs a new wrapper and initialize it with a packet handle with default values
     */
    public WrapperPlayServerEntitySound() {
        super(TYPE);
    }

    public WrapperPlayServerEntitySound(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'sound'
     * ProtocolLib currently does not provide a wrapper for this type. Access to this type is only provided by an InternalStructure
     *
     * @return 'sound'
     */
    public Sound getSound() {
        return this.handle.getHolders(MinecraftReflection.getSoundEffectClass(), BukkitConverters.getSoundConverter()).read(0);
    }

    /**
     * Sets the value of field 'sound'
     *
     * @param value New value for field 'sound'
     */
    public void setSound(Sound value) {
        this.handle.getHolders(MinecraftReflection.getSoundEffectClass(), BukkitConverters.getSoundConverter()).write(0, value);
    }

    /**
     * Retrieves the value of field 'source'
     *
     * @return 'source'
     */
    public SoundCategory getSource() {
        return this.handle.getSoundCategories().read(0);
    }

    /**
     * Sets the value of field 'source'
     *
     * @param value New value for field 'source'
     */
    public void setSource(SoundCategory value) {
        this.handle.getSoundCategories().write(0, value);
    }

    /**
     * Retrieves the value of field 'id'
     *
     * @return 'id'
     */
    public int getId() {
        return this.handle.getIntegers().read(0);
    }

    /**
     * Sets the value of field 'id'
     *
     * @param value New value for field 'id'
     */
    public void setId(int value) {
        this.handle.getIntegers().write(0, value);
    }

    /**
     * Retrieves the value of field 'volume'
     *
     * @return 'volume'
     */
    public float getVolume() {
        return this.handle.getFloat().read(0);
    }

    /**
     * Sets the value of field 'volume'
     *
     * @param value New value for field 'volume'
     */
    public void setVolume(float value) {
        this.handle.getFloat().write(0, value);
    }

    /**
     * Retrieves the value of field 'pitch'
     *
     * @return 'pitch'
     */
    public float getPitch() {
        return this.handle.getFloat().read(1);
    }

    /**
     * Sets the value of field 'pitch'
     *
     * @param value New value for field 'pitch'
     */
    public void setPitch(float value) {
        this.handle.getFloat().write(1, value);
    }

    /**
     * Retrieves the value of field 'seed'
     *
     * @return 'seed'
     */
    public long getSeed() {
        return this.handle.getLongs().read(0);
    }

    /**
     * Sets the value of field 'seed'
     *
     * @param value New value for field 'seed'
     */
    public void setSeed(long value) {
        this.handle.getLongs().write(0, value);
    }

}
