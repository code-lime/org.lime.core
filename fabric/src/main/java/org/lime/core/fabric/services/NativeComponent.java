package org.lime.core.fabric.services;

//#switch PROPERTIES.versionAdventurePlatform
//#caseof 6.3.0;6.6.0
//OF//import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
//#default
import net.kyori.adventure.platform.fabric.FabricAudiences;
//#endswitch

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;

@Singleton
public class NativeComponent {
    //#switch PROPERTIES.versionAdventurePlatform
    //#caseof 6.3.0;6.6.0
    //OF//    @Inject MinecraftAudiences audiences;
    //#default
    @Inject FabricAudiences audiences;
    //#endswitch

    public Component convert(net.minecraft.network.chat.Component component) {
        //#switch PROPERTIES.versionAdventurePlatform
        //#caseof 6.3.0;6.6.0
        //OF//        return audiences.asAdventure(component);
        //#default
        return component.asComponent();
        //#endswitch
    }
    public net.minecraft.network.chat.Component convert(Component component) {
        //#switch PROPERTIES.versionAdventurePlatform
        //#caseof 6.3.0;6.6.0
        //OF//        return audiences.asNative(component.asComponent());
        //#default
        return audiences.toNative(component.asComponent());
        //#endswitch
    }
}
