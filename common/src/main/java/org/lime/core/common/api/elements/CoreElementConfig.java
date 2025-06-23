package org.lime.core.common.api.elements;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.lime.core.common.system.execute.Action0;
import org.lime.core.common.system.execute.Func1;

public interface CoreElementConfig<Self extends CoreElementConfig<Self>> {
    Self addFile(String file, String name, CoreResource<?> data);

    default Self addText(String file, Func1<CoreResource<String>, CoreResource<String>> builder) {
        return addFile(file, file, builder.invoke(CoreResource.text()));
    }
    default <J extends JsonElement> Self addConfig(String config, Func1<CoreResource<J>, CoreResource<J>> builder) {
        return addFile(config + ".json", config, builder.invoke(CoreResource.json()));
    }
    default <J>Self addConfig(String config, Class<J> rawClass, Func1<CoreResource<J>, CoreResource<J>> builder) {
        return addConfig(config, rawClass, v -> v, builder);
    }
    default <J>Self addConfig(String config, Class<J> rawClass, Func1<GsonBuilder, GsonBuilder> configure, Func1<CoreResource<J>, CoreResource<J>> builder) {
        return addFile(config + ".json", config, builder.invoke(CoreResource.gson(rawClass, configure)));
    }

    default Self addEmpty(String key, Action0 callback) {
        return addFile("", key, CoreResource.none().withInvoke(v0 -> callback.invoke()));
    }
    default Self addEmptyInit(String key, Action0 callback) {
        return addFile("", key, CoreResource.init().withInvoke(v0 -> callback.invoke()));
    }
}
