function create(size, func, separator) {
    var list = [];
    for (var i = 0; i < size; i++) list.push(func(i));
    return list.join(separator)
}
function createToast(index) {
    if (index === 0) return "";
    var lines = [];
    var type = create(index, function(i) { return "T" + i; }, ", ");
    var targs = create(index, function(i) { return "T"+i+" val"+i; }, ", ");
    var atype = create(index, function(i) { return "A" + i; }, ", ");
    var args = create(index, function(i) { return "val" + i; }, ", ");
    lines.push("    public static <"+type+">Toast"+index+"<"+type+"> toast("+targs+"){ return new Toast"+index+"<>("+args+"); }");

    lines.push("    public static class Toast"+index+"<"+type+"> extends IToast {");
    lines.push("        public LockToast"+index+"<"+type+"> lock() { return new LockToast"+index+"<>(this); }");
    lines.push("        @Override public Object[] getValues() { return new Object[] { "+args+" }; }");
    lines.push("        public Toast"+index+"("+targs+") { "+create(index, function(i) { return "this.val"+i+" = val"+i+";"; }, " ")+" }");
    lines.push("        "+create(index, function(i) { return "public T"+i+" val"+i+";"; }, " "));
    lines.push("        @Override public int size() { return "+index+"; }");
    lines.push("        @Override public int hashCode() { return super.hashCode(); }");
    lines.push("        @Override public boolean equals(Object obj) { return super.equals(obj); }");
    lines.push("        @Override public Object get(int index) { switch (index) { "+create(index, function(i) { return "case "+i+": return val"+i+";"; }, " ")+" } return null; }");
    lines.push("        @Override public void set(int index, Object value) { switch (index) { "+create(index, function(i) { return "case "+i+": val"+i+" = (T"+i+")value; break;"; }, " ")+" } }");
    lines.push("        @Override public Object edit(int index, system.Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }");
    lines.push("        public <"+atype+">Toast"+index+"<"+atype+"> map("+create(index, function(i) { return "system.Func1<T"+i+", A"+i+"> map"+i; }, ", ")+") { return system.toast("+create(index, function(i) { return "map"+i+".invoke(val"+i+")"; }, ", ")+"); }");
    lines.push("        public void invoke(system.Action"+index+"<"+type+"> action) { action.invoke("+args+"); }");
    lines.push("        public <T>T invokeGet(system.Func"+index+"<"+type+", T> func) { return func.invoke("+args+"); }");
    lines.push("    }");

    lines.push("    public static class LockToast"+index+"<"+type+"> extends ILockToast<Toast"+index+"<"+type+">> {");
    lines.push("        public LockToast"+index+"(Toast"+index+"<"+type+"> base) { super(base); }");
    lines.push("        "+create(index, function(i) { return "public T"+i+" get"+i+"() { return (T"+i+")get("+i+"); }"; }, " "));
    lines.push("        "+create(index, function(i) { return "public void set"+i+"(T"+i+" value) { set("+i+", value); }"; }, " "));
    lines.push("        "+create(index, function(i) { return "public T"+i+" edit"+i+"(system.Func1<T"+i+", T"+i+"> func) { return (T"+i+")edit("+i+", v -> func.invoke((T"+i+")v)); }"; }, " "));
    lines.push("    }");
    return lines.join("\n");
}
function createObj(index) {
    var lines = [];
    lines.push(createToast(index));
    return lines.join("\n");
}
var res = [];
for (var i = 0; i < 10; i++) res.push(createObj(i));
res.join("\n");