package org.lime.plugin;

import java.io.File;

public interface IConfig extends IFile {
    String getConfigFile();

    default File _getConfigFile(String file) { return new File(getConfigFile() + file); }

    default boolean _existConfig(String config) { return _existConfig(config, ".json"); }
    default String _readAllConfig(String config) { return _readAllConfig(config, ".json"); }
    default void _writeAllConfig(String config, String text) { _writeAllConfig(config, ".json", text); }
    default void _deleteConfig(String config) { _deleteConfig(config, ".json"); }

    default boolean _existConfig(String config, String ext) { return _existFile(getConfigFile() + config + ext); }
    default String _readAllConfig(String config, String ext) { return _readAllText(getConfigFile() + config + ext); }
    default void _writeAllConfig(String config, String ext, String text) { _writeAllText(getConfigFile() + config + ext, text); }
    default void _deleteConfig(String config, String ext) { _deleteText(getConfigFile() + config + ext); }

    static File getLibraryFile(String file) { return new File("plugins/libs/" + file); }
    static File[] getLibraryFiles(String... files) {
        int length = files.length;
        File[] _files = new File[length];
        for (int i = 0; i < length; i++)
            _files[i] = getLibraryFile(files[i]);
        return _files;
    }
}
