package org.lime.system;

import com.caoccao.javet.values.reference.IV8ValueObject;

public class rawJson {
    public static boolean check(Object value) {
        return value instanceof IV8ValueObject;
    }

    public static json.builder<?> get(Object value) {
        return json.builder.byObject(json.parse(((IV8ValueObject)value).toJsonString()));
    }
}
