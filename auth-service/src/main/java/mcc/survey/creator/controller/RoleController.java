package mcc.survey.creator.controller;

import mcc.survey.creator.dto.AuthorityDto;
import mcc.survey.creator.dto.CreateRoleRequest;
import mcc.survey.creator.dto.RoleDto;
import mcc.survey.creator.dto.UpdateRoleRequest;
import mcc.survey.creator.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('OP_MANAGE_ROLES')") // Class-level authorization
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping("/authorities")
    public ResponseEntity<List<AuthorityDto>> getAllAuthorities() {
        List<AuthorityDto> authorities = roleService.getAllAuthorities();
        return ResponseEntity.ok(authorities);
    }

    @PostMapping
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest createRoleRequest) {
        RoleDto createdRole = roleService.createRole(createRoleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @GetMapping
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        List<RoleDto> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long id) {
        RoleDto role = roleService.getRoleById(id);
        return ResponseEntity.ok(role);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDto> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest updateRoleRequest) {
        RoleDto updatedRole = roleService.updateRole(id, updateRoleRequest);
        return ResponseEntity.ok(updatedRole);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        // Consider adding more checks here, e.g., cannot delete system roles
        // For now, it relies on RoleService's potential checks
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
