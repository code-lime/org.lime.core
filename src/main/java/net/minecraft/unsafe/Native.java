package net.minecraft.unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

@SuppressWarnings("removal")
public class Native {
    private static final VarHandle MODIFIERS;
    private static final VarHandle SIGNATURE;
    static {
        try {
            MODIFIERS = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup())
                    .findVarHandle(Field.class, "modifiers", int.class);
            SIGNATURE = MethodHandles.privateLookupIn(Method.class, MethodHandles.lookup())
                    .findVarHandle(Method.class, "signature", String.class);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    public static String signature(Method method){
        String sig = (String)SIGNATURE.get(method);
        if(sig != null) return sig;

        StringBuilder sb = new StringBuilder("(");
        for(Class<?> c : method.getParameterTypes())
            sb.append((sig = Array.newInstance(c, 0).toString()), 1, sig.indexOf('@'));
        return sb.append(')')
                .append(method.getReturnType() == void.class
                        ? "V"
                        : (sig = Array.newInstance(method.getReturnType(), 0).toString()).substring(1, sig.indexOf('@'))
                )
                .toString()
                .replace('.', '/');
    }
    public static Field nonFinal(Field field) {
        int mods = field.getModifiers();
        //if (Modifier.isFinal(mods)) {
        try {
            /*
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            */
            Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
            getDeclaredFields0.setAccessible(true);
            Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
            Field modifiers = null;
            for (Field each : fields) {
                if ("modifiers".equals(each.getName())) {
                    modifiers = each;
                    break;
                }
            }
            assert modifiers != null;
            modifiers.setAccessible(true);
            mods &= ~Modifier.FINAL;
            mods |= Modifier.PUBLIC;
            modifiers.setInt(field, mods);
            return field;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        //}
        //if (Modifier.isFinal(mods)) MODIFIERS.set(field, mods & ~Modifier.FINAL);
        //return field;
    }
    public static <T extends AccessibleObject>T access(T val) {
        if (System.getSecurityManager() == null) {
            val.setAccessible(true);
            return val;
        }
        else return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
            val.setAccessible(true);
            return val;
        });
    }
}
