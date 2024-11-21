package org.lime.system;

import org.lime.system.tuple.Tuple;
import org.lime.system.tuple.Tuple2;

import java.util.Arrays;
import java.util.List;

public class Roman {
    private static final List<Tuple2<Integer, String>> romans = Arrays.asList(
            Tuple.of(1000, "M"),
            Tuple.of(900, "CM"),
            Tuple.of(500, "D"),
            Tuple.of(400, "CD"),
            Tuple.of(100, "C"),
            Tuple.of(90, "XC"),
            Tuple.of(50, "L"),
            Tuple.of(40, "XL"),
            Tuple.of(10, "X"),
            Tuple.of(9, "IX"),
            Tuple.of(5, "V"),
            Tuple.of(4, "IV"),
            Tuple.of(1, "I")
    );

    public static String formatRoman(int number) {
        if (number > 3999 || number < 1) return String.valueOf(number);
        for (Tuple2<Integer, String> roman : romans) {
            if (number < roman.val0) continue;
            number -= roman.val0;
            return roman.val1 + (number == 0 ? "" : formatRoman(number));
        }
        return String.valueOf(number);
    }
}
