package com.enit.satellite_platform.modules.user_management.management_cvore_service.services;

import com.enit.satellite_platform.exceptions.DuplicationException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.Authority;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.exceptions.RoleNotFoundException;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.repositories.AuthorityRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
    private final AuthorityRepository authorityRepository;

    @Transactional
    public Authority createRole(String authorityName) {
        Assert.hasText(authorityName, "Authority name cannot be empty");
        String formattedAuthorityName = formatAuthorityName(authorityName);

        if (authorityRepository.existsByAuthority(formattedAuthorityName)) {
            logger.warn("Attempted to create duplicate role: {}", formattedAuthorityName);
            throw new DuplicationException("Role '" + formattedAuthorityName + "' already exists.");
        }

        Authority newAuthority = new Authority();
        newAuthority.setAuthority(formattedAuthorityName);
        Authority savedAuthority = authorityRepository.save(newAuthority);
        logger.info("Created new role: {}", savedAuthority.getAuthority());
        return savedAuthority;
    }

    public Optional<Authority> findRoleByName(String authorityName) {
        Assert.hasText(authorityName, "Authority name cannot be empty");
        return authorityRepository.findByAuthority(formatAuthorityName(authorityName));
    }

    public Authority findRoleByNameOrThrow(String authorityName) {
        return findRoleByName(authorityName)
                .orElseThrow(() -> new RoleNotFoundException("Role '" + authorityName + "' not found."));
    }

    public List<Authority> getAllRoles() {
        return authorityRepository.findAll();
    }

    @Transactional
    public void deleteRole(String authorityName) {
        Assert.hasText(authorityName, "Authority name cannot be empty");
        String formattedAuthorityName = formatAuthorityName(authorityName);

        Authority authorityToDelete = authorityRepository.findByAuthority(formattedAuthorityName)
                .orElseThrow(() -> new RoleNotFoundException("Role '" + formattedAuthorityName + "' not found. Cannot delete."));

        // TODO: Add check here to prevent deletion if the role is assigned to any users.
        // This would require injecting UserRepository and querying users by role.

        authorityRepository.delete(authorityToDelete);
        logger.info("Deleted role: {}", formattedAuthorityName);
    }

    /**
     * Ensures the authority name starts with "ROLE_".
     * Converts the rest of the name to uppercase.
     * Example: "admin" becomes "ROLE_ADMIN", "Role_Editor" becomes "ROLE_EDITOR"
     */
    private String formatAuthorityName(String authorityName) {
        String upperCaseName = authorityName.trim().toUpperCase();
        if (upperCaseName.startsWith("ROLE_")) {
            return upperCaseName;
        }
        return "ROLE_" + upperCaseName;
    }
}
