var logs = [];
function getAllClasses(className, types = ['method'], regex = null) {
    try {
        const regexp = new RegExp(regex === null ? '.*' : regex);
        const mappingTree = PROPERTIES.mappingTree;

        className = className.replace(/\./g, '/');

        let namespaceId = -1;
        let checkClass = mappingTree.getClass(className);

        for (const namespaceName of mappingTree.getDstNamespaces()) {
            const id = mappingTree.getNamespaceId(namespaceName);
            const dat = mappingTree.getClass(className, id);

            if (dat !== null) {
                namespaceId = id;
                checkClass = dat;
            }
        }

        const lines = [];

        if (checkClass !== null) {
            const name = checkClass.getName(namespaceId);
            if (types.includes('method')) {
                for (const method of checkClass.getMethods()) {
                    const methodName = method.getName(namespaceId);
                    if (methodName === '<clinit>' || !regexp.test(methodName)) continue;
                    const methodDesc = method.getDesc(namespaceId);
                    lines.push(`accessible method ${name} ${methodName} ${methodDesc}`);
                }
            }
            if (types.includes('field')) {
                for (const field of checkClass.getFields()) {
                    const fieldName = field.getName(namespaceId);
                    if (!regexp.test(fieldName)) continue;
                    const fieldDesc = field.getDesc(namespaceId);
                    lines.push(`accessible field ${name} ${fieldName} ${fieldDesc}`);
                }
            }
        }
        return lines.sort().join('\n')
    } catch (e) {
        return 'ERROR ' + e.name + ": " + e.message + "\nLogs:\n"+logs.join('\n');
    }
}
