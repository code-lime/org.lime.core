package org.lime.core.fabric.services;

//#switch PROPERTIES.versionAdventurePlatform
//#caseofregex ^5\.[0-9]+\.[0-9]+$
import net.kyori.adventure.platform.fabric.FabricAudiences;
//#default
//OF//import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
//#endswitch

//#switch PROPERTIES.versionMinecraft
//#caseofregex ^1\.(20\.1|21\.([0-9]|10))$
import net.minecraft.resources.ResourceLocation;
//#default
//OF//import net.minecraft.resources.Identifier;
//#endswitch

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

@Singleton
public class NativeComponent {
    //#switch PROPERTIES.versionAdventurePlatform
    //#caseofregex ^5\.[0-9]+\.[0-9]+$
    @Inject FabricAudiences audiences;
    //#default
    //OF//    @Inject MinecraftAudiences audiences;
    //#endswitch

    public Component convert(net.minecraft.network.chat.Component component) {
        //#switch PROPERTIES.versionAdventurePlatform
        //#caseofregex ^5\.[0-9]+\.[0-9]+$
        return component.asComponent();
        //#default
        //OF//        return audiences.asAdventure(component);
        //#endswitch
    }
    public net.minecraft.network.chat.Component convert(Component component) {
        //#switch PROPERTIES.versionAdventurePlatform
        //#caseofregex ^5\.[0-9]+\.[0-9]+$
        return audiences.toNative(component.asComponent());
        //#default
        //OF//        return audiences.asNative(component.asComponent());
        //#endswitch
    }

    //#switch PROPERTIES.versionMinecraft
    //#caseofregex ^1\.(20\.1|21\.([0-9]|10))$
    public ResourceLocation convert(Key key) {
    //#default
    //OF//    public Identifier convert(Key key) {
    //#endswitch
        //#switch PROPERTIES.versionAdventurePlatform
        //#caseofregex ^5\.[0-9]+\.[0-9]+$
        return FabricAudiences.toNative(key);
        //#default
        //OF//        return MinecraftAudiences.asNative(key);
        //#endswitch
    }
    //#switch PROPERTIES.versionMinecraft
    //#caseofregex ^1\.(20\.1|21\.([0-9]|10))$
    public Key convert(ResourceLocation key) {
    //#default
    //OF//    public Key convert(Identifier key) {
    //#endswitch
        return key;
    }
}
