package net.minecraft.paper.java;

import joptsimple.OptionSet;

import java.io.File;

public class OptionSetUtils {
    private static OptionSet options;
    public static void setOptions(OptionSet options) {
        System.out.println("SETUP OPTIONS: " + options);
        OptionSetUtils.options = options;
    }

    public static File getUniverseFile(String fileName) {
        System.out.println("GET UNIVERSE FILE OF NAME: " + fileName);
        if (options.has("universe")) {
            File folder = (File)options.valueOf("universe");
            return new File(folder, fileName);
        }
        return new File(fileName);
    }
    public static File getUniverseFile(File file) {
        System.out.println("GET UNIVERSE FILE OF FILE: " + file.getAbsolutePath());
        if (options.has("universe")) {
            File folder = (File)options.valueOf("universe");
            return new File(folder, file.getName());
        }
        return file;
    }
}
