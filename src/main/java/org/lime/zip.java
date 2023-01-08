package org.lime;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class zip {
    public static HashMap<String, byte[]> unzip(byte[] input) {
        File file = new File(UUID.randomUUID() + ".zip");
        try {
            HashMap<String, byte[]> files = new HashMap<>();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(input);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }

            try (ZipFile zip = new ZipFile(file)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;
                    InputStream stream = zip.getInputStream(entry);
                    files.put(entry.getName(), stream.readAllBytes());
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            return files;
        } finally {
            file.delete();
        }
    }
    @SuppressWarnings("all")
    public static byte[] zip(HashMap<String, byte[]> entries) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            try (ZipOutputStream zip = new ZipOutputStream(stream)) {
                for (Map.Entry<String, byte[]> kv : entries.entrySet()) {
                    ZipEntry entry = new ZipEntry(kv.getKey());
                    zip.putNextEntry(entry);
                    zip.write(kv.getValue());
                    zip.closeEntry();
                }
            }
            return stream.toByteArray();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
