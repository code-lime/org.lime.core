package org.lime.docs;

import org.lime.system.execute.Func0;

import java.util.List;
import java.util.stream.Stream;

public interface IDocs {
    Stream<String> lines();

    static IDocs remote(Func0<IDocs> docs) {
        return () -> docs.invoke().lines();
    }
    static IDocs style() {
        List<String> lines = List.of(
                "<style>",
                "name { color: Aqua }",
                "comment { color: #57A64A; font-style: italic }",
                "string { color: #D69D85 }",
                "bool { color: #569CD6 }",
                "warning { color: #FFFF00 }",
                "any { color: Gray }",
                "empty { font-size: 0px }",
                "</style>"
        );
        return lines::stream;
    }

    /*
    static IDocs indexing(Stream<IIndexDocs> docsList) {
        return () -> {
            Tuple1<Integer> number = Tuple.of(0);
            return docsList.map(v -> {
                number.val0++;
                return number.val0 + ". " + v.link();
            });
        };
    }
    */
}















