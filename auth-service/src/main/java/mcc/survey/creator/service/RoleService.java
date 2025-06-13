package mcc.survey.creator.service;

import mcc.survey.creator.dto.AuthorityDto;
import mcc.survey.creator.dto.CreateRoleRequest;
import mcc.survey.creator.dto.RoleDto;
import mcc.survey.creator.dto.UpdateRoleRequest;
import mcc.survey.creator.model.Authority;
import mcc.survey.creator.model.Role;
import mcc.survey.creator.repository.AuthorityRepository;
import mcc.survey.creator.repository.RoleRepository;
import mcc.survey.creator.exception.ResourceNotFoundException;
import mcc.survey.creator.exception.DuplicateResourceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    // --- Authority DTO Mapping ---
    private AuthorityDto toAuthorityDto(Authority authority) {
        return new AuthorityDto(authority.getId(), authority.getName());
    }

    // --- Role DTO Mapping ---
    private RoleDto toRoleDto(Role role) {
        Set<AuthorityDto> authorityDtos = role.getAuthorities().stream()
                .map(this::toAuthorityDto)
                .collect(Collectors.toSet());
        return new RoleDto(role.getId(), role.getName(), authorityDtos);
    }

    @Transactional(readOnly = true)
    public List<AuthorityDto> getAllAuthorities() {
        return authorityRepository.findAll().stream()
                .map(this::toAuthorityDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toRoleDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleDto getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return toRoleDto(role);
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest createRoleRequest) {
        if (roleRepository.existsByName(createRoleRequest.getName())) {
            throw new DuplicateResourceException("Role with name '" + createRoleRequest.getName() + "' already exists.");
        }

        Role role = new Role();
        role.setName(createRoleRequest.getName());

        Set<Authority> authorities = new HashSet<>();
        if (createRoleRequest.getAuthorityIds() != null) {
            for (Long authorityId : createRoleRequest.getAuthorityIds()) {
                Authority authority = authorityRepository.findById(authorityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Authority not found with id: " + authorityId));
                authorities.add(authority);
            }
        }
        role.setAuthorities(authorities);
        Role savedRole = roleRepository.save(role);
        return toRoleDto(savedRole);
    }

    @Transactional
    public RoleDto updateRole(Long id, UpdateRoleRequest updateRoleRequest) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        // Check if name is being changed and if the new name already exists
        if (!role.getName().equals(updateRoleRequest.getName()) && roleRepository.existsByName(updateRoleRequest.getName())) {
            throw new DuplicateResourceException("Role with name '" + updateRoleRequest.getName() + "' already exists.");
        }
        role.setName(updateRoleRequest.getName());

        Set<Authority> authorities = new HashSet<>();
        if (updateRoleRequest.getAuthorityIds() != null) {
            for (Long authorityId : updateRoleRequest.getAuthorityIds()) {
                Authority authority = authorityRepository.findById(authorityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Authority not found with id: " + authorityId));
                authorities.add(authority);
            }
        }
        role.setAuthorities(authorities); // Replace existing authorities

        Role updatedRole = roleRepository.save(role);
        return toRoleDto(updatedRole);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        // Add checks here to prevent deletion of system roles, e.g.
        // if (role.getName().equals("ROLE_SYSTEM_ADMIN") || role.getName().equals("ROLE_USER")) {
        //    throw new IllegalArgumentException("Cannot delete system-defined role: " + role.getName());
        // }
        // Also, consider implications if users are currently assigned this role.
        // For simplicity, this example directly deletes.
        roleRepository.delete(role);
    }
}
