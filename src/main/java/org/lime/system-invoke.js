function createOffset(offset, size, func, separator) {
    var list = [];
    for (var i = offset; i < size; i++) list.push(func(i));
    return list.join(separator)
}
function create(size, func, separator) {
    var list = [];
    for (var i = 0; i < size; i++) list.push(func(i));
    return list.join(separator)
}
function of(size) {
    var list = [];
    for (var i = 0; i < size + 1; i++) list.push(create(size - i, function (v) { return "val0" }, "."));
    for (var i = 1; i < size + 1; i++) list[i] = list[i] + ".val1";
    for (var i = 0; i < size + 1; i++) list[i] = "arg0" + (i === size ? "" : ".") + list[i];
    return list.join(", ")
}
function createAF(index) {
    if (index === 0) return "";
    var lines = [];
    var type = create(index, function(i) { return "T" + i; }, ", ");
    var targs = create(index, function(i) { return "T"+i+" arg"+i; }, ", ");
    var args = create(index, function(i) { return "val" + i; }, ", ");
    var _args = createOffset(1, index, function(i) { return "val" + i; }, ", ");
    var invoke = create(index, function(i) { return "(T"+i+")args["+i+"]"; }, ", ");
    var tt = "T0";
    for (var i = 1; i < index; i++) tt = "system.Toast2<"+tt+","+"T"+i+">";

    lines.push("    public interface Action"+index+"<"+type+"> extends Action"+"1<"+tt+"> {");
    lines.push("        void invoke("+targs+");");
    lines.push("        @Override default void invoke("+tt+" arg0) { invoke("+of(index-1)+"); }");
    lines.push("        @Override default Object call(Object[] args) { invoke("+invoke+"); return null; }");
    lines.push("        static <"+type+">Action"+index+"<"+type+"> of(Method method) {");
    lines.push("            return Modifier.isStatic(method.getModifiers()) ?");
    lines.push("                    (" + args + ") -> system.invoke(method, null, new Object[] { "+args+" }) :");
    lines.push("                    (" + args + ") -> system.invoke(method, val0, new Object[] { "+_args+" });");
    lines.push("        }");
    lines.push("    }");
    lines.push("    public static <"+type+">Action"+index+"<"+type+"> action(Action"+index+"<"+type+"> action) { return action; }");

    lines.push("    public interface Func"+index+"<"+type+", TResult> extends ICallable {");
    lines.push("        TResult invoke("+targs+");");
    lines.push("        @Override default Object call(Object[] args) { return invoke("+invoke+"); }");
    lines.push("        static <"+type+", TResult>Func"+index+"<"+type+", TResult> of(Method method) {");
    lines.push("            return Modifier.isStatic(method.getModifiers()) ?");
    lines.push("                    (" + args + ") -> system.invoke(method, null, new Object[] { "+args+" }) :");
    lines.push("                    (" + args + ") -> system.invoke(method, val0, new Object[] { "+_args+" });");
    lines.push("        }");
    lines.push("    }");
    lines.push("    public static <"+type+", TResult>Func"+index+"<"+type+", TResult> func(Func"+index+"<"+type+", TResult> func) { return func; }");

    lines.push("    public interface ActionEx"+index+"<"+type+"> {");
    lines.push("        void invoke("+targs+") throws Throwable;");
    lines.push("        default Action"+index+"<"+type+"> throwable() {");
    lines.push("            return (" + args + ") -> { try { this.invoke(" + args + "); } catch (Throwable e) { throw new IllegalArgumentException(e); } };");
    lines.push("        }");
    lines.push("        default Func"+index+"<"+type+", Boolean> optional() {");
    lines.push("            return (" + args + ") -> { try { this.invoke(" + args + "); return true; } catch (Throwable e) { return false; } };");
    lines.push("        }");
    lines.push("    }");
    lines.push("    public static <"+type+">ActionEx"+index+"<"+type+"> actionEx(ActionEx"+index+"<"+type+"> action) { return action; }");

    lines.push("    public interface FuncEx"+index+"<"+type+", TResult> {");
    lines.push("        TResult invoke("+targs+") throws Throwable;");
    lines.push("        default Func"+index+"<"+type+", TResult> throwable() {");
    lines.push("            return (" + args + ") -> { try { return this.invoke(" + args + "); } catch (Throwable e) { throw new IllegalArgumentException(e); } };");
    lines.push("        }");
    lines.push("        default Func"+index+"<"+type+", Optional<TResult>> optional() {");
    lines.push("            return (" + args + ") -> { try { return Optional.ofNullable(this.invoke(" + args + ")); } catch (Throwable e) { return Optional.empty(); } };");
    lines.push("        }");
    lines.push("    }");
    lines.push("    public static <"+type+", TResult>FuncEx"+index+"<"+type+", TResult> funcEx(FuncEx"+index+"<"+type+", TResult> func) { return func; }");

    lines.push("    ")

    return lines.join("\n");
}
function createObj(index) {
    var lines = [];
    lines.push(createAF(index));
    return lines.join("\n");
}
var res = [];
for (var i = 2; i < 10; i++) res.push(createObj(i));
res.join("\n");