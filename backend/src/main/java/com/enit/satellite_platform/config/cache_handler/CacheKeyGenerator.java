package com.enit.satellite_platform.config.cache_handler;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.ICacheKeyGenerator;

/**
 * A concrete implementation of {@link ICacheKeyGenerator} that generates cache keys
 * based on the input object's string representation, followed by SHA-256 hashing.
 * It provides specific handling for {@link Map} objects to ensure consistent key generation
 * regardless of map entry order by sorting the map before generating the string.
 * It adds prefixes ("MAP:", "LIST:", "OBJ:") to the hash for basic type identification in the key.
 */
@Component
public class CacheKeyGenerator implements ICacheKeyGenerator {

    /**
     * Generates a cache key by:
     * 1. Handling null input.
     * 2. Checking if the object is a {@link Map}. If so, creates a {@link TreeMap} to sort keys,
     *    converts it to a string, hashes it using SHA-256, and prefixes with "MAP:".
     * 3. Checking if the object is an {@link Iterable}. If so, converts it to a string (using default toString),
     *    hashes it using SHA-256, and prefixes with "LIST:".
     * 4. For any other object type, converts it to a string (using default toString),
     *    hashes it using SHA-256, and prefixes with "OBJ:".
     *
     * @param obj The object to generate a key for.
     * @return A string cache key prefixed and hashed based on the object type and content. Returns "null" for null input.
     */
    @Override
    public String generateKey(Object obj) {
        if (obj == null) return "null";

        if (obj instanceof Map) {
            Map<?, ?> sortedMap = new TreeMap<>((Map<?, ?>) obj);
            return "MAP:" + doHash(sortedMap.toString());
        } else if (obj instanceof Iterable) {
            return "LIST:" + doHash(obj.toString());
        } else {
            return "OBJ:" + doHash(obj.toString());
        }
    }

    /**
     * Computes the SHA-256 hash of the input string and returns it as a hexadecimal string.
     *
     * @param input The string to hash.
     * @return The SHA-256 hash of the input string, represented as a lowercase hexadecimal string.
     * @throws RuntimeException if the SHA-256 algorithm is not available in the environment.
     */
    private static String doHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
