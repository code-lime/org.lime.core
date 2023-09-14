package org.lime.docs;

import org.lime.system.toast.*;

import java.util.List;
import java.util.stream.Stream;

public interface IDocs {
    Stream<String> lines();

    static IDocs style() {
        List<String> lines = List.of(
                "<style>",
                "name { color: Aqua }",
                "comment { color: #57A64A; font-style: italic }",
                "string { color: #D69D85 }",
                "bool { color: #569CD6 }",
                "warning { color: #FFFF00 }",
                "any { color: Gray }",
                "</style>"
        );
        return lines::stream;
    }
    static IDocs indexing(Stream<IIndexDocs> docsList) {
        return () -> {
            Toast1<Integer> number = Toast.of(0);
            return docsList.map(v -> {
                number.val0++;
                return number.val0 + ". " + v.link();
            });
        };
    }
}















