package com.enit.satellite_platform.shared.converters;

import com.enit.satellite_platform.modules.user_management.normal_user_service.repositories.UserRepository;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component // Make it a Spring component to allow dependency injection
public class UserKeyDeserializer extends KeyDeserializer {

    // Inject the UserRepository to fetch User objects
    private final UserRepository userRepository;

    public UserKeyDeserializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        if (key == null || key.isEmpty()) {
            return null;
        }
        try {
            ObjectId userId = new ObjectId(key);
            // Find the user by ID. Throw exception if not found to signal deserialization failure.
            return userRepository.findById(userId)
                    .orElseThrow(() -> new IOException("Cannot find User with ID: " + key));
        } catch (IllegalArgumentException e) {
            // Handle cases where the key is not a valid ObjectId string
            throw new IOException("Invalid ObjectId format for User key: " + key, e);
        }
    }
}
