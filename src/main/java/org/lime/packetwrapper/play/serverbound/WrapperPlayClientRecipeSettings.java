package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;

public class WrapperPlayClientRecipeSettings extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.RECIPE_SETTINGS;
    private static final Class<?> ACTION_TYPE = MinecraftReflection.getNullableNMS("world.inventory.RecipeBookType");
    public WrapperPlayClientRecipeSettings() {
        super(TYPE);
    }

    public WrapperPlayClientRecipeSettings(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'bookType'
     *
     * @return 'bookType'
     */
    public RecipeBookType getBookType() {
        return this.handle.getModifier().withType(ACTION_TYPE, new EnumWrappers.IndexedEnumConverter<>(RecipeBookType.class, ACTION_TYPE)).read(0);
    }

    /**
     * Sets the value of field 'bookType'
     *
     * @param value New value for field 'bookType'
     */
    public void setBookType(RecipeBookType value) {
        this.handle.getModifier().withType(ACTION_TYPE, new EnumWrappers.IndexedEnumConverter<>(RecipeBookType.class, ACTION_TYPE)).write(0, value);
    }

    /**
     * Retrieves the value of field 'isOpen'
     *
     * @return 'isOpen'
     */
    public boolean getIsOpen() {
        return this.handle.getBooleans().read(0);
    }

    /**
     * Sets the value of field 'isOpen'
     *
     * @param value New value for field 'isOpen'
     */
    public void setIsOpen(boolean value) {
        this.handle.getBooleans().write(0, value);
    }

    /**
     * Retrieves the value of field 'isFiltering'
     *
     * @return 'isFiltering'
     */
    public boolean getIsFiltering() {
        return this.handle.getBooleans().read(1);
    }

    /**
     * Sets the value of field 'isFiltering'
     *
     * @param value New value for field 'isFiltering'
     */
    public void setIsFiltering(boolean value) {
        this.handle.getBooleans().write(1, value);
    }

    public enum RecipeBookType {
        CRAFTING,
        FURNACE,
        BLAST_FURNACE,
        SMOKER;
    }

}
