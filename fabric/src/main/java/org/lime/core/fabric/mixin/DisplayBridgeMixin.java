package org.lime.core.fabric.mixin;

import net.minecraft.world.entity.Display;
import org.lime.core.fabric.bridge.displays.DisplayEntityBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Display.class)
public abstract class DisplayBridgeMixin
        implements DisplayEntityBridge {
    @Shadow
    protected abstract float getHeight();
    @Shadow
    protected abstract void setHeight(float height);

    @Override
    public float core$height() {
        return getHeight();
    }
    @Override
    public void height(float height) {
        setHeight(height);
    }
}
