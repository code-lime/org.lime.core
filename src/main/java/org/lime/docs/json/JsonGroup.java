package org.lime.docs.json;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.lime.docs.BaseGroup;
import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast1;

import java.util.stream.Stream;

public final class JsonGroup extends BaseGroup<IJElement> {
    public JsonGroup(String title, String index, IJElement element, ImmutableList<IComment> comments) {
        super(title, index, element, comments);
    }

    public static JsonGroup of(String title, String index, IJElement element) { return new JsonGroup(title, index, element, ImmutableList.of()); }
    public static JsonGroup of(String title, String index, IJElement element, IComment... comments) { return new JsonGroup(title, index, element, ImmutableList.copyOf(comments)); }

    public static JsonGroup of(String title, IJElement element) { return of(title, title.toLowerCase(), element); }
    public static JsonGroup of(String title, IJElement element, IComment... comments) { return of(title, title.toLowerCase(), element, comments); }

    @Override public Stream<String> elementContext() {
        Toast1<Boolean> isFirst = Toast.of(true);
        return Streams.concat(
                element().lines(this)
                        .map(v -> {
                            if (isFirst.val0) {
                                isFirst.val0 = false;
                                return "<pre><code>" + v;
                            }
                            return v;
                        }),
                Stream.of("</code></pre>")
                        .map(v -> (isFirst.val0 ? "<pre><code>" : "")+v)
        );
    }
}