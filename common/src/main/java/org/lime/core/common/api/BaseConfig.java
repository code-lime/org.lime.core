package org.lime.core.common.api;

import java.io.File;

public interface BaseConfig extends BaseFile {
    String configFile();

    default File $configFile(String file) {
        return new File(configFile() + file);
    }

    default boolean $existConfig(String config) {
        return $existConfig(config, ".json");
    }
    default String $readAllConfig(String config) {
        return $readAllConfig(config, ".json");
    }
    default void $writeAllConfig(String config, String text) {
        $writeAllConfig(config, ".json", text);
    }
    default void $deleteConfig(String config) {
        $deleteConfig(config, ".json");
    }

    default boolean $existConfig(String config, String ext) {
        return $existFile(configFile() + config + ext);
    }
    default String $readAllConfig(String config, String ext) {
        return $readAllText(configFile() + config + ext);
    }
    default void $writeAllConfig(String config, String ext, String text) {
        $writeAllText(configFile() + config + ext, text);
    }
    default void $deleteConfig(String config, String ext) {
        $deleteText(configFile() + config + ext);
    }
}
