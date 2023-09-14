package org.lime.system;

import org.lime.system.toast.Toast;
import org.lime.system.toast.Toast2;

import java.util.Arrays;
import java.util.List;

public class Roman {
    private static final List<Toast2<Integer, String>> romans = Arrays.asList(
            Toast.of(1000, "M"),
            Toast.of(900, "CM"),
            Toast.of(500, "D"),
            Toast.of(400, "CD"),
            Toast.of(100, "C"),
            Toast.of(90, "XC"),
            Toast.of(50, "L"),
            Toast.of(40, "XL"),
            Toast.of(10, "X"),
            Toast.of(9, "IX"),
            Toast.of(5, "V"),
            Toast.of(4, "IV"),
            Toast.of(1, "I")
    );

    public static String formatRoman(int number) {
        if (number > 3999 || number < 1) return String.valueOf(number);
        for (Toast2<Integer, String> roman : romans) {
            if (number < roman.val0) continue;
            number -= roman.val0;
            return roman.val1 + (number == 0 ? "" : formatRoman(number));
        }
        return String.valueOf(number);
    }
}
