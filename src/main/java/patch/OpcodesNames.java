package patch;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class OpcodesNames {
    private static final HashBiMap<Integer, String> names = HashBiMap.create();
    private static final BiMap<String, Integer> opcodes = names.inverse();

    public static String getName(int opcode) {
        return names.get(opcode);
    }
    public static int getOpcode(String name) {
        return opcodes.get(name);
    }

    static {
        Field[] fields = Opcodes.class.getFields();
        int state = 0;
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                var fClass = field.getType();
                if (fClass != int.class && fClass != Integer.class)
                    continue;
                if (state == 0) {
                    if (fClass == Integer.class)
                        state = 1;
                } else if (state == 1) {
                    if (fClass == int.class)
                        state = 2;
                }
                if (state != 2)
                    continue;
                String name = field.getName();
                try {
                    int value = field.getInt(null);
                    String oldName = names.put(value, name);
                    if (oldName != null) {
                        throw new RuntimeException("Value "+value+" is dup: " + String.join(", ", name, oldName));
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    }
}
