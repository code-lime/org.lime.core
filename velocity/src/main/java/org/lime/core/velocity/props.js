function registerProperty(name) {
    return () => JSON.stringify(`${PROPERTIES[name]}`);
}

const id = registerProperty('pluginId');
const name = registerProperty('pluginName');
const version = registerProperty('version');
