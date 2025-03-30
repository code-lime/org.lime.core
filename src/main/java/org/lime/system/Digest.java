package org.lime.system;

import java.security.MessageDigest;
import java.util.Formatter;
import java.util.stream.Stream;

public class Digest {
    public static final Digest MD2 = new Digest("MD2");
    public static final Digest MD5 = new Digest("MD5");
    public static final Digest SHA1 = new Digest("SHA-1");
    public static final Digest SHA224 = new Digest("SHA-224");
    public static final Digest SHA256 = new Digest("SHA-256");
    public static final Digest SHA384 = new Digest("SHA-384");
    public static final Digest SHA512 = new Digest("SHA-512");

    private final String algorithm;
    public Digest(String algorithm) {
        this.algorithm = algorithm;
    }

    public String miniHash(byte[]... parts) {
        return miniHash(Stream.of(parts));
    }
    public String miniHash(Stream<byte[]> parts) {
        return hash(parts).substring(5, 15);
    }

    public String hash(byte[]... parts) {
        return hash(Stream.of(parts));
    }
    public String hash(Stream<byte[]> parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            parts.forEach(digest::update);
            return hex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String hex(byte... bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes)
            formatter.format("%02x", b);
        return formatter.toString();
    }
    public static String hex(Stream<Byte> bytes) {
        Formatter formatter = new Formatter();
        bytes.forEach(b -> formatter.format("%02x", b));
        return formatter.toString();
    }
    public static String hex(Iterable<Byte> bytes) {
        Formatter formatter = new Formatter();
        bytes.forEach(b -> formatter.format("%02x", b));
        return formatter.toString();
    }
}
