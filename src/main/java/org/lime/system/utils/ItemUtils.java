package org.lime.system.utils;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public class ItemUtils {
    public static String saveItem(ItemStack item) {
        return item == null ? null : new String(Base64.getEncoder().encode(item.serializeAsBytes()));
    }
    public static ItemStack loadItem(String str) {
        return str == null ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(str.getBytes()));
    }
}
