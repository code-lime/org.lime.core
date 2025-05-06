var logs = [];
function create(size, func, separator) {
    var list = [];
    for (var i = 0; i < size; i++) list.push(func(i));
    return list.join(separator)
}
function createCase(index) {
    var args = create(index, function(i) { return "args[" + i + "]"; }, ", ");

    var lines = [
        "            case "+index+" -> handle.invoke("+args+");"
    ];

    return lines;
}

function createLines(count) {
    var out = [];
    for (var i = 0; i < count; i++)
        out = out.concat(createCase(i));
    return out;
}
function getAllCases(count) {
    try {
        return createLines(count).join("\n");
    } catch (e) {
        return 'ERROR ' + e.name + ": " + e.message + "\nLogs:\n"+logs.join('\n');
    }
}