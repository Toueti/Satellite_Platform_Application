package com.enit.satellite_platform.config.cache_handler;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import com.enit.satellite_platform.config.cache_handler.general_cache_handler.ICacheKeyGenerator;

@Component
public class CacheKeyGenerator implements ICacheKeyGenerator {

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

