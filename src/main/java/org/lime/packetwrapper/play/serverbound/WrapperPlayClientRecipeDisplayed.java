package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.MinecraftKey;

public class WrapperPlayClientRecipeDisplayed extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.RECIPE_DISPLAYED;

    public WrapperPlayClientRecipeDisplayed() {
        super(TYPE);
    }

    public WrapperPlayClientRecipeDisplayed(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'recipe'
     *
     * @return 'recipe'
     */
    public MinecraftKey getRecipe() {
        return this.handle.getMinecraftKeys().read(0);
    }

    /**
     * Sets the value of field 'recipe'
     *
     * @param value New value for field 'recipe'
     */
    public void setRecipe(MinecraftKey value) {
        this.handle.getMinecraftKeys().write(0, value);
    }

}
