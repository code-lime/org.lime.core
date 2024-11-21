package org.lime.packetwrapper.play.serverbound;

import org.lime.packetwrapper.AbstractPacket;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;

public class WrapperPlayClientSetCommandBlock extends AbstractPacket {

    public static final PacketType TYPE = PacketType.Play.Client.SET_COMMAND_BLOCK;
    private static final Class<?> COMMAND_BLOCK_MODE_CLASS = MinecraftReflection.getNullableNMS("world.level.block.entity.CommandBlockEntity$Mode", "world.level.block.entity.TileEntityCommand$Type");
    private static final EquivalentConverter<CommandBlockMode> COMMAND_BLOCK_MODE_CONVERTER = new EnumWrappers.IndexedEnumConverter<>(CommandBlockMode.class, COMMAND_BLOCK_MODE_CLASS);

    public WrapperPlayClientSetCommandBlock() {
        super(TYPE);
    }

    public WrapperPlayClientSetCommandBlock(PacketContainer packet) {
        super(packet, TYPE);
    }

    /**
     * Retrieves the value of field 'pos'
     *
     * @return 'pos'
     */
    public BlockPosition getPos() {
        return this.handle.getBlockPositionModifier().read(0);
    }

    /**
     * Sets the value of field 'pos'
     *
     * @param value New value for field 'pos'
     */
    public void setPos(BlockPosition value) {
        this.handle.getBlockPositionModifier().write(0, value);
    }

    /**
     * Retrieves the value of field 'command'
     *
     * @return 'command'
     */
    public String getCommand() {
        return this.handle.getStrings().read(0);
    }

    /**
     * Sets the value of field 'command'
     *
     * @param value New value for field 'command'
     */
    public void setCommand(String value) {
        this.handle.getStrings().write(0, value);
    }

    /**
     * Retrieves the value of field 'trackOutput'
     *
     * @return 'trackOutput'
     */
    public boolean getTrackOutput() {
        return this.handle.getBooleans().read(0);
    }

    /**
     * Sets the value of field 'trackOutput'
     *
     * @param value New value for field 'trackOutput'
     */
    public void setTrackOutput(boolean value) {
        this.handle.getBooleans().write(0, value);
    }

    /**
     * Retrieves the value of field 'conditional'
     *
     * @return 'conditional'
     */
    public boolean getConditional() {
        return this.handle.getBooleans().read(1);
    }

    /**
     * Sets the value of field 'conditional'
     *
     * @param value New value for field 'conditional'
     */
    public void setConditional(boolean value) {
        this.handle.getBooleans().write(1, value);
    }

    /**
     * Retrieves the value of field 'automatic'
     *
     * @return 'automatic'
     */
    public boolean getAutomatic() {
        return this.handle.getBooleans().read(2);
    }

    /**
     * Sets the value of field 'automatic'
     *
     * @param value New value for field 'automatic'
     */
    public void setAutomatic(boolean value) {
        this.handle.getBooleans().write(2, value);
    }

    /**
     * Retrieves the value of field 'mode'
     *
     * @return 'mode'
     */
    public CommandBlockMode getMode() {
        return this.handle.getModifier().withType(COMMAND_BLOCK_MODE_CLASS, COMMAND_BLOCK_MODE_CONVERTER).read(0);
    }

    /**
     * Sets the value of field 'mode'
     *
     * @param value New value for field 'mode'
     */
    public void setMode(CommandBlockMode value) {
        this.handle.getModifier().withType(COMMAND_BLOCK_MODE_CLASS, COMMAND_BLOCK_MODE_CONVERTER).write(0, value);
    }

    public enum CommandBlockMode {
        SEQUENCE,
        AUTO,
        REDSTONE
    }
}
