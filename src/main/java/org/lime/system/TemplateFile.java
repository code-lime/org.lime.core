package org.lime.system;

import java.io.File;
import java.io.IOException;

public class TemplateFile {
    public record TempFile(File file) implements AutoCloseable {
        @Override public void close() {
            file.delete();
        }
    }
    public static TempFile temp() throws IOException {
        return new TempFile(File.createTempFile("lime-", ".tmp"));
    }
}
