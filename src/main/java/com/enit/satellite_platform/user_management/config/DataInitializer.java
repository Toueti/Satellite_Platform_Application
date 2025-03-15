package com.enit.satellite_platform.user_management.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.enit.satellite_platform.user_management.model.Authority;
import com.enit.satellite_platform.user_management.repository.AuthorityRepository;


@Component
public class DataInitializer implements CommandLineRunner {

    private final AuthorityRepository roleRepository;

    public DataInitializer(AuthorityRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.findByAuthority("ROLE_THEMATICIAN").isEmpty()) {
            Authority userRole = new Authority();
            userRole.setAuthority("ROLE_THEMATICIAN");
            roleRepository.save(userRole);
        }

        if (roleRepository.findByAuthority("ROLE_ADMIN").isEmpty()) {
            Authority adminRole = new Authority();
            adminRole.setAuthority("ROLE_ADMIN");
            roleRepository.save(adminRole);
        }
    }
}