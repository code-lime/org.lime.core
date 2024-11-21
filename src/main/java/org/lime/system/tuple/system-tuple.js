var logs = [];
function create(size, func, separator) {
    var list = [];
    for (var i = 0; i < size; i++) list.push(func(i));
    return list.join(separator)
}
function createTuple(index) {
    if (index === 0) return {
        'files': {},
        'functions': []
    };
    var files = {};
    var type = create(index, function(i) { return "T" + i; }, ", ");
    var targs = create(index, function(i) { return "T"+i+" val"+i; }, ", ");
    var atype = create(index, function(i) { return "A" + i; }, ", ");
    var args = create(index, function(i) { return "val" + i; }, ", ");

    var functions = [
        "    public static <"+type+">Tuple"+index+"<"+type+"> of("+targs+"){ return new Tuple"+index+"<>("+args+"); }",
        "    public static <"+type+">LockTuple"+index+"<"+type+"> lock("+targs+"){ return of("+args+").lock(); }"
    ];

    files["Tuple"+index+".java"] = [
        "package org.lime.system.tuple;",
        "",
        "import org.lime.system.execute.*;",
        "",
        "// Generated by JavaScript (c) Lime",
        "public class Tuple"+index+"<"+type+"> extends ITuple {",
        "    public LockTuple"+index+"<"+type+"> lock() { return new LockTuple"+index+"<>(this); }",
        "    @Override public Object[] getValues() { return new Object[] { "+args+" }; }",
        "    public Tuple"+index+"("+targs+") { "+create(index, function(i) { return "this.val"+i+" = val"+i+";"; }, " ")+" }",
        "    "+create(index, function(i) { return "public T"+i+" val"+i+";"; }, " "),
        "    "+create(index, function(i) { return "public T"+i+" get"+i+"() { return val"+i+"; }"; }, " "),
        "    @Override public int size() { return "+index+"; }",
        "    @Override public int hashCode() { return super.hashCode(); }",
        "    @Override public boolean equals(Object obj) { return super.equals(obj); }",
        "    @Override public Object get(int index) { switch (index) { "+create(index, function(i) { return "case "+i+": return val"+i+";"; }, " ")+" } return null; }",
        "    @Override public void set(int index, Object value) { switch (index) { "+create(index, function(i) { return "case "+i+": val"+i+" = (T"+i+")value; break;"; }, " ")+" } }",
        "    @Override public Object edit(int index, Func1<Object, Object> func) { Object ret; set(index, ret = func.invoke(get(index))); return ret; }",
        "    public void set(Tuple"+index+"<"+type+"> other) { "+create(index, function(i) { return "this.val"+i+" = other.val"+i+";"; }, " ")+" }",
        "    public <"+atype+">Tuple"+index+"<"+atype+"> map("+create(index, function(i) { return "Func1<T"+i+", A"+i+"> map"+i; }, ", ")+") { return Tuple.of("+create(index, function(i) { return "map"+i+".invoke(val"+i+")"; }, ", ")+"); }",
        "    public void invoke(Action"+index+"<"+type+"> action) { action.invoke("+args+"); }",
        "    public <T>T invokeGet(Func"+index+"<"+type+", T> func) { return func.invoke("+args+"); }",
        "}",
    ].join('\n');
    files["LockTuple"+index+".java"] = [
        "package org.lime.system.tuple;",
        "",
        "import org.lime.system.execute.*;",
        "",
        "// Generated by JavaScript (c) Lime",
        "public class LockTuple"+index+"<"+type+"> extends ILockTuple<Tuple"+index+"<"+type+">> {",
        "    public LockTuple"+index+"(Tuple"+index+"<"+type+"> base) { super(base); }",
        "    "+create(index, function(i) { return "public T"+i+" get"+i+"() { return (T"+i+")get("+i+"); }"; }, " "),
        "    "+create(index, function(i) { return "public void set"+i+"(T"+i+" value) { set("+i+", value); }"; }, " "),
        "    "+create(index, function(i) { return "public T"+i+" edit"+i+"(Func1<T"+i+", T"+i+"> func) { return (T"+i+")edit("+i+", v -> func.invoke((T"+i+")v)); }"; }, " "),
        "}"
    ].join('\n');

    return {
        'files': files,
        'functions': functions
    }
}
function combineItem(a, b) {
    var out = {
        'files': {},
        'functions': []
    };
    var ab = [a,b];
    for (var i in ab) {
        var item = ab[i];
        for (var key in item.files) out.files[key] = item.files[key];
        out.functions = out.functions.concat(item.functions);
    }
    return out;
}

function createObj() {
    var out = {
        'files': {},
        'functions': []
    };
    for (var i = 1; i < 10; i++)
        out = combineItem(out, createTuple(i));
    return out;
}
function getAllFunctions() {
    try {
        return createObj().functions.join("\n");
    } catch (e) {
        return 'ERROR ' + e.name + ": " + e.message + "\nLogs:\n"+logs.join('\n');
    }
}
function getAllFiles() {
    try {
        var files = createObj().files;
        var out = [];
        for (var fileName in files) {
            var fileText = files[fileName];
            out.push(fileName);
            out.push(fileText);
        }
        return out.join('\r');
    } catch (e) {
        return 'ERROR ' + e.name + ": " + e.message + "\nLogs:\n"+logs.join('\n');
    }
}