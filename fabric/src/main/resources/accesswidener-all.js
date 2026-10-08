function getAllClasses(className, types = ['method'], regex = null) {
    try {
        const regexp = new RegExp(regex === null ? '.*' : regex);
        const ZipFile = Java.type('java.util.zip.ZipFile');
        const loom = PROJECT.getExtensions().getByName('loom');
        const ClassNode = Java.type('org.objectweb.asm.tree.ClassNode');
        const ClassReader = Java.type('org.objectweb.asm.ClassReader');
        const name = className.replace(/\./g, '/');
        for (const jar of loom.getNamedMinecraftProvider().getMinecraftJarPaths()) {
            const zip = new ZipFile(jar.toFile());
            try {
                const entry = zip.getEntry(name + '.class');
                if (entry === null) continue;
                const input = zip.getInputStream(entry);
                let bytes;
                try { bytes = input.readAllBytes(); } finally { input.close(); }
                const node = new ClassNode();
                const reader = new ClassReader(bytes);
                reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                const lines = [];
                if (types.includes('method'))
                    for (const method of node.methods)
                        if (method.name !== '<clinit>' && regexp.test(method.name))
                            lines.push(`accessible method ${name} ${method.name} ${method.desc}`);
                if (types.includes('field'))
                    for (const field of node.fields)
                        if (regexp.test(field.name))
                            lines.push(`accessible field ${name} ${field.name} ${field.desc}`);
                return lines.sort().join('\n');
            } finally { zip.close(); }
        }
        return '';
    } catch (e) {
        return '#ERROR ' + e.name + ": " + e.message;
    }
}
