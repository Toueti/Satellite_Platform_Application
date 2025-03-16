package com.enit.satellite_platform.user_management.model;

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

import com.enit.satellite_platform.project_management.model.Project;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
@Data
public class User implements UserDetails {

    @Id
    private ObjectId id;

    @Field("username")
    private String username;

    @JsonIgnore
    @Field("password")
    private String password;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @DBRef
    @Field("authorities")
    private Set<Authority> authorities = new HashSet<>();

    @DBRef(lazy = true)
    @JsonIgnore
    private Set<Project> sharedProjects = new HashSet<>();

    @DBRef(lazy = true)
    @JsonIgnore
    private Set<Project> projects = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
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
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static User example() {
        User user = new User();
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setAuthority("THEMATICIAN");
        authorities.add(authority);
        user.setUsername("user1");
        user.setPassword("password");
        user.setEmail("user1@example.com");
        user.setAuthorities(authorities);
        return user;
    }
}