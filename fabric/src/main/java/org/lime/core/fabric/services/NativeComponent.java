package org.lime.core.fabric.services;

//#switch PROPERTIES.versionAdventurePlatform
//#caseofregex 6\.\d\.\d
//OF//import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
//#default
import net.kyori.adventure.platform.fabric.FabricAudiences;
//#endswitch

//#switch PROPERTIES.versionMinecraft
//#caseofregex 1\.21\.11
//OF//import net.minecraft.resources.Identifier;
//#default
import net.minecraft.resources.ResourceLocation;
//#endswitch

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

@Singleton
public class NativeComponent {
    //#switch PROPERTIES.versionAdventurePlatform
    //#caseofregex 6\.\d\.\d
    //OF//    @Inject MinecraftAudiences audiences;
    //#default
    @Inject FabricAudiences audiences;
    //#endswitch

    public Component convert(net.minecraft.network.chat.Component component) {
        //#switch PROPERTIES.versionAdventurePlatform
        //#caseofregex 6\.\d\.\d
        //OF//        return audiences.asAdventure(component);
        //#default
        return component.asComponent();
        //#endswitch
    }
    public net.minecraft.network.chat.Component convert(Component component) {
        //#switch PROPERTIES.versionAdventurePlatform
        //#caseofregex 6\.\d\.\d
        //OF//        return audiences.asNative(component.asComponent());
        //#default
        return audiences.toNative(component.asComponent());
        //#endswitch
    }

    //#if PROPERTIES.versionMinecraft == '1.21.11'
    //IF//    public Identifier convert(Key key) {
    //#else
    public ResourceLocation convert(Key key) {
    //#endif
        //#switch PROPERTIES.versionAdventurePlatform
        //#caseofregex 6\.\d\.\d
        //OF//        return MinecraftAudiences.asNative(key);
        //#default
        return FabricAudiences.toNative(key);
        //#endswitch
    }
    //#if PROPERTIES.versionMinecraft == '1.21.11'
    //IF//    public Key convert(Identifier key) {
    //#else
    public Key convert(ResourceLocation key) {
    //#endif
        return key;
    }
}
