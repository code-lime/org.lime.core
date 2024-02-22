package org.lime.system.range;

import org.lime.docs.IIndexGroup;
import org.lime.docs.json.IComment;
import org.lime.docs.json.IJElement;
import org.lime.docs.json.JsonEnumInfo;
import org.lime.docs.json.JsonGroup;

import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class IRange {
    public static IRange parse(String text) {
        if (text.contains(","))
            return new ListRange(Arrays.stream(text.split(",")).map(IRange::parse).toList());
        String[] damageList = text.replace("..", ":").split(":");

        if (damageList.length == 1) {
            String prefix = damageList[0];
            int length = prefix.length();
            return prefix.endsWith("%")
                    ? new PercentRange(Double.parseDouble(prefix.substring(0, length - 1)) / 100)
                    : new OnceRange(Double.parseDouble(prefix));
        }

        return new DoubleRange(parse(damageList[0]), parse(damageList[1]));
    }

    public abstract double getMin(double max);
    public abstract double getMax(double max);
    public abstract double getValue(double max);

    public int getIntMin(double max) { return (int) Math.round(getMin(max)); }
    public int getIntMax(double max) { return (int) Math.round(getMax(max)); }
    public int getIntValue(double max) { return (int) Math.round(getValue(max)); }

    public abstract String displayText();
    public abstract IntStream getAllInts(double max);
    public abstract boolean hasInt(double max, int value);
    public abstract boolean inRange(double value, double max);

    public abstract String toFormat();

    public static IIndexGroup docs(String index) {
        return JsonEnumInfo.of(index)
                .add(IJElement.raw(10), IComment.text("Значние"))
                .add(IJElement.raw("10%"), IComment.text("Процентное значение от максимального"))
                .add(IJElement.join(
                        IJElement.linkCurrent(),
                        IJElement.text(","),
                        IJElement.linkCurrent(),
                        IJElement.text(","),
                        IJElement.any(),
                        IJElement.text(","),
                        IJElement.linkCurrent()
                ), IComment.text("Набор значений"))
                .add(IJElement.or(
                        IJElement.join(
                                IJElement.field("FROM"),
                                IJElement.text(".."),
                                IJElement.field("TO")
                        ),
                        IJElement.join(
                                IJElement.field("FROM"),
                                IJElement.text(":"),
                                IJElement.field("TO")
                        )
                ), IComment.join(
                        IComment.text("Выборка значений от "),
                        IComment.field("FROM"),
                        IComment.text(" до "),
                        IComment.field("TO"),
                        IComment.text(" включительно. Пример: "),
                        IComment.raw("3..6"),
                        IComment.text("  - порядковые номера "),
                        IComment.raw(3), IComment.text(", "),
                        IComment.raw(4), IComment.text(", "),
                        IComment.raw(5), IComment.text(" и "),
                        IComment.raw(6)
                ));
    }
}
