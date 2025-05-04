package com.enit.satellite_platform.modules.user_management.management_cvore_service.entities;

import com.enit.satellite_platform.modules.project_management.entities.Project;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.*;
import java.time.LocalDateTime;

@Document(collection = "users")
@Data
public class User implements UserDetails {

    @Id
    private ObjectId id;

    @Field("username")
    @NotNull(message = "Username cannot be null")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String name;

    @JsonIgnore
    @Field("password")
    @NotNull(message = "Password cannot be null")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @Indexed(unique = true)
    @Field("email")
    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    private String email;

    @Field("locked")
    private boolean locked = false;

    @Field("enabled")
    private boolean enabled = true;

    @DBRef
    @Field("authorities")
    private Set<Authority> authorities = new HashSet<>();

    @DBRef(lazy = true)
    @JsonIgnore
    private Set<Project> sharedProjects = new HashSet<>();

    @DBRef(lazy = true)
    @JsonIgnore
    private Set<Project> projects = new HashSet<>();

    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.unmodifiableSet(authorities);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Adds an authority to the user.
     *
     * @param authority the authority to add.
     */
    public void addAuthority(Authority authority) {
        this.authorities.add(authority);
    }

    /**
     * Removes an authority from the user.
     *
     * @param authority the authority to remove.
     */
    public void removeAuthority(Authority authority) {
        this.authorities.remove(authority);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    public String getId() {
        return id != null ? id.toString() : null;
    }

}
