package com.enit.satellite_platform.config.cache_handler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for generating SHA-256 hashes from files and bytes.
 * This is primarily used by cache handlers to generate consistent cache keys
 * for file-based content.
 */
@Component
public class FileHashingUtil {
    private static final Logger log = LoggerFactory.getLogger(FileHashingUtil.class);
    private static final int BUFFER_SIZE = 8192;
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Calculates the SHA-256 hash of a file's content.
     *
     * @param file The file to hash
     * @return The hex string representation of the hash
     * @throws IOException If there's an error reading the file
     * @throws IllegalArgumentException If the file is null or doesn't exist
     */
    public String calculateFileHash(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            return calculateHash(fis);
        } catch (IOException e) {
            log.error("Error calculating hash for file: {}", file.getAbsolutePath(), e);
            throw e;
        }
    }

    /**
     * Calculates the SHA-256 hash of a byte array.
     *
     * @param content The byte array to hash
     * @return The hex string representation of the hash
     * @throws IllegalArgumentException If the content is null
     */
    public String calculateHash(byte[] content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(content);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // This should never happen as SHA-256 is required to be supported by all Java implementations
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Calculates the SHA-256 hash of an input stream's content.
     * Note: This method does not close the input stream.
     *
     * @param inputStream The input stream to hash
     * @return The hex string representation of the hash
     * @throws IOException If there's an error reading from the stream
     * @throws IllegalArgumentException If the input stream is null
     */
    public String calculateHash(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // This should never happen as SHA-256 is required to be supported by all Java implementations
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Calculates the SHA-256 hash of a string's UTF-8 bytes.
     *
     * @param content The string to hash
     * @return The hex string representation of the hash
     * @throws IllegalArgumentException If the content is null
     */
    public String calculateHash(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }

        return calculateHash(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a byte array to its hexadecimal string representation.
     *
     * @param bytes The byte array to convert
     * @return The hexadecimal string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
