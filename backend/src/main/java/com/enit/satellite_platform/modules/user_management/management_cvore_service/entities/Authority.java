package com.enit.satellite_platform.modules.user_management.management_cvore_service.entities;

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

    // Removed static constants ROLE_THEMATICIAN and ROLE_ADMIN
    // Removed static valueOf method

    @Override
    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        // Consider adding validation or formatting here if needed,
        // although RoleService currently handles formatting.
        this.authority = authority;
    }

    // No need for valueOf method anymore, roles are looked up via RoleService
}
