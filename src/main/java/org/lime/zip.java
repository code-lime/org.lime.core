package org.lime;

import com.google.common.collect.Streams;
import org.lime.system.execute.Action2;
import org.lime.system.execute.ActionEx2;
import org.lime.system.tuple.Tuple;
import org.lime.system.tuple.Tuple1;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
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

    public static void unzip(InputStream input, Action2<String, InputStream> entryCallback) {
        try (ZipInputStream stream = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                entryCallback.invoke(entry.getName(), stream);
                stream.closeEntry();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    public static void unzipEx(InputStream input, ActionEx2<String, InputStream> entryCallback) throws Throwable {
        try (ZipInputStream stream = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                entryCallback.invoke(entry.getName(), stream);
                stream.closeEntry();
            }
        }
    }

    public record ZipStreamEntry(String name, InputStream stream) { }
    public static Stream<ZipStreamEntry> unzipStream(InputStream input) {
        ZipInputStream stream = new ZipInputStream(input);
        return Streams.stream(new Iterator<>() {
            private Tuple1<ZipEntry> entry;

            private void updateNext() {
                try {
                    if (entry != null && entry.val0 != null)
                        stream.closeEntry();
                    do {
                        entry = Tuple.of(stream.getNextEntry());
                    } while (entry.val0 != null && entry.val0.isDirectory());
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
            @Override public boolean hasNext() {
                if (entry == null) updateNext();
                return entry.val0 != null;
            }
            @Override public ZipStreamEntry next() {
                ZipEntry memory = entry.val0;
                updateNext();
                return new ZipStreamEntry(memory.getName(), stream);
            }
        });
    }
}
