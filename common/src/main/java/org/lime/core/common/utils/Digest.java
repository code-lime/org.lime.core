package org.lime.core.common.utils;

import org.lime.core.common.utils.execute.Execute;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.stream.Stream;

public record Digest(
        String algorithm) {
    public static final Digest MD2 = new Digest("MD2");
    public static final Digest MD5 = new Digest("MD5");
    public static final Digest SHA1 = new Digest("SHA-1");
    public static final Digest SHA224 = new Digest("SHA-224");
    public static final Digest SHA256 = new Digest("SHA-256");
    public static final Digest SHA384 = new Digest("SHA-384");
    public static final Digest SHA512 = new Digest("SHA-512");

    public String miniHashStream(InputStream... parts) {
        return toMini(hashStream(parts));
    }
    public String miniHashStream(Stream<InputStream> parts) {
        return toMini(hashStream(parts));
    }
    public String miniHash(byte[]... parts) {
        return toMini(hash(parts));
    }
    public String miniHash(Stream<byte[]> parts) {
        return toMini(hash(parts));
    }

    public String hashStream(InputStream... parts) {
        return hashStream(Stream.of(parts));
    }
    public String hashStream(Stream<InputStream> parts) {
        return hash(parts.map(Execute.funcEx(InputStream::readAllBytes).throwable()));
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
    
    private static String toMini(String hash) {
        return hash.substring(5, 15);
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
