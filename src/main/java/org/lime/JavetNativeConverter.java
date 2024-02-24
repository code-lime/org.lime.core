package org.lime;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.converters.JavetBridgeConverter;
import com.caoccao.javet.values.V8Value;

public class JavetNativeConverter extends JavetBridgeConverter {
    private final long threadId = Thread.currentThread().getId();

    @Override protected <T extends V8Value> T toV8Value(V8Runtime v8Runtime, Object object, int depth) throws JavetException {
        V8Value v8Value;

        boolean isAsync = threadId != Thread.currentThread().getId();

        String prefix = isAsync ? "JAVET:ASYNC" : "JAVET:GLOBAL";

        if (isAsync) {
            System.out.println("["+prefix+"] CREATE NATIVE OF " + object + " WITH TYPE " + (object == null ? "NULL" : object.getClass().toString()));
            Thread.dumpStack();
        }

        if (object instanceof Integer value) {
            v8Value = v8Runtime.createV8ValueInteger(value);
        } else if (object instanceof Boolean value) {
            v8Value = v8Runtime.createV8ValueBoolean(value);
        } else if (object instanceof Double value) {
            v8Value = v8Runtime.createV8ValueDouble(value);
        } else if (object instanceof Float value) {
            v8Value = v8Runtime.createV8ValueDouble(value);
        } else if (object instanceof Long value) {
            v8Value = v8Runtime.createV8ValueLong(value);
        } else if (object instanceof Short value) {
            v8Value = v8Runtime.createV8ValueInteger(value);
        } else if (object instanceof Byte value) {
            v8Value = v8Runtime.createV8ValueInteger(value);
        } else if (object instanceof Character value) {
            v8Value = v8Runtime.createV8ValueString(Character.toString(value));
        } else if (object instanceof Number value) {
            v8Value = v8Runtime.createV8ValueDouble(value.doubleValue());
        } else {
            v8Value = super.toV8Value(v8Runtime, object, depth);
        }

        if (isAsync)
            System.out.println("["+prefix+"] Created!");

        return (T)v8Value;
    }
}
