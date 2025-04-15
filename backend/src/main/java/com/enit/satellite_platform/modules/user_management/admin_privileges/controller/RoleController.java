package com.enit.satellite_platform.modules.user_management.admin_privileges.controller;

import com.enit.satellite_platform.modules.user_management.admin_privileges.dto.CreateRoleRequest;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.Authority;
import com.enit.satellite_platform.modules.user_management.management_cvore_service.services.RoleService;
import com.enit.satellite_platform.shared.dto.GenericResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')") // Secure endpoints for Admins only
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<GenericResponse<Authority>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        Authority newRole = roleService.createRole(request.getRoleName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericResponse<>("CREATED", "Role created successfully", newRole));
    }

    @GetMapping
    public ResponseEntity<GenericResponse<List<Authority>>> getAllRoles() {
        List<Authority> roles = roleService.getAllRoles();
        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Roles retrieved successfully", roles));
    }

    @GetMapping("/{roleName}")
    public ResponseEntity<GenericResponse<Authority>> getRoleByName(@PathVariable String roleName) {
        Authority role = roleService.findRoleByNameOrThrow(roleName);
        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Role retrieved successfully", role));
    }

    @DeleteMapping("/{roleName}")
    public ResponseEntity<GenericResponse<Void>> deleteRole(@PathVariable String roleName) {
        roleService.deleteRole(roleName);
        return ResponseEntity.ok(new GenericResponse<>("SUCCESS", "Role deleted successfully"));
    }
}
