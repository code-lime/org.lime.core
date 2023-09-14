package org.lime.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public interface IFile extends ILogger {
    default boolean _existFile(String path) {
        File myObj = new File(path);
        return (myObj.isFile() && myObj.exists());
    }
    default String _readAllText(String path) {
        try {
            return Files.readString(Paths.get(path)).replace("\r", "");
            //return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8).replace("\r", "");
        } catch (IOException e) {
            _log("An error occurred.");
            _log(e.getMessage());
            return null;
        }
    }
    default String _readAllText(File file) { return _readAllText(file.getAbsolutePath()); }
    default void _writeAllText(String path, String text) {
        try {
            Files.writeString(Paths.get(path), text);
        } catch (IOException e) {
            _log("An error occurred.");
            _log(e.getMessage());
        } catch (Exception e) {
            _log(e.getMessage());
        }
    }
    default void _deleteText(String path) {
        try {
            File myObj = new File(path);
            myObj.delete();
        } catch (Exception e) {
            _log(e.getMessage());
        }
    }
}
