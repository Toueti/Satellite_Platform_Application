package com.enit.satellite_platform.user_management.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;

@Document(collection = "authorities")
@Data
public class Authority implements GrantedAuthority {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @Field("authority")
    private String authority;

    public static final String ROLE_THEMATICIAN = "ROLE_THEMATICIAN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    @Override
    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public static String valueOf(String role) {
        switch (role.toUpperCase()) {
            case "ROLE_THEMATICIAN":
            case "THEMATICIAN":
                return ROLE_THEMATICIAN;
            case "ROLE_ADMIN":
            case "ADMIN":
                return ROLE_ADMIN;
            default:
                throw new IllegalArgumentException("Invalid role: " + role);
        }
    }
}
