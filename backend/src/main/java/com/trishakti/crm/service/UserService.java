package com.trishakti.crm.service;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.Role;
import com.trishakti.crm.domain.Team;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.RoleName;
import com.trishakti.crm.dto.UserDtos.CreateUserRequest;
import com.trishakti.crm.dto.UserDtos.UpdateUserRequest;
import com.trishakti.crm.dto.UserDtos.UserResponse;
import com.trishakti.crm.exception.DomainExceptions.BusinessRuleException;
import com.trishakti.crm.exception.DomainExceptions.DuplicateResourceException;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.RoleRepository;
import com.trishakti.crm.repository.TeamRepository;
import com.trishakti.crm.repository.UserRepository;
import com.trishakti.crm.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       TeamRepository teamRepository, PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, RoleName role, Boolean active, Pageable pageable) {
        return PageResponse.of(userRepository.search(q, role, active, pageable), CrmMappers::user);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return CrmMappers.user(find(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> byRole(RoleName role) {
        return userRepository.findActiveByRole(role).stream().map(CrmMappers::user).toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        if (userRepository.existsByUsernameIgnoreCase(req.username())) {
            throw new DuplicateResourceException("Username already in use: " + req.username());
        }
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new DuplicateResourceException("Email already in use: " + req.email());
        }
        User user = new User();
        user.setFullName(req.fullName());
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRoles(resolveRoles(req.roles()));
        user.setActive(req.active() == null || req.active());
        user.setTeam(resolveTeam(req.teamId()));
        user.setManager(req.managerId() != null ? find(req.managerId()) : null);
        userRepository.save(user);
        auditService.record("USER_CREATED", "User", user.getId(), null, CrmMappers.user(user));
        return CrmMappers.user(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req) {
        User user = find(id);
        UserResponse before = CrmMappers.user(user);
        user.setFullName(req.fullName());
        user.setEmail(req.email());
        user.setPhone(req.phone());
        if (req.teamId() != null) user.setTeam(resolveTeam(req.teamId()));
        if (req.managerId() != null) user.setManager(find(req.managerId()));
        if (req.roles() != null && !req.roles().isEmpty()) user.setRoles(resolveRoles(req.roles()));
        if (req.active() != null) user.setActive(req.active());
        auditService.record("USER_UPDATED", "User", id, before, CrmMappers.user(user));
        return CrmMappers.user(user);
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        User user = find(id);
        if (!active && userRepository.findActiveByRole(RoleName.ADMIN).size() <= 1
                && user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN)) {
            throw new BusinessRuleException("Cannot deactivate the last administrator");
        }
        user.setActive(active);
        auditService.record(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "User", id, null, null);
    }

    /**
     * Work that would be orphaned by removing a user. A user is referenced by eleven tables, so a
     * blind delete just fails on a foreign key; this reports what is in the way instead, and the
     * caller is told to deactivate rather than delete.
     */
    private static final List<String[]> USER_REFERENCES = List.of(
            new String[]{"Lead e where e.assignedUser.id = :id", "assigned lead"},
            new String[]{"Lead e where e.createdByUser.id = :id", "created lead"},
            new String[]{"LeadActivity e where e.actor.id = :id", "timeline entry"},
            new String[]{"LeadAssignment e where e.toUser.id = :id or e.fromUser.id = :id or e.assignedBy.id = :id",
                    "assignment record"},
            new String[]{"CallLog e where e.caller.id = :id", "call log"},
            new String[]{"FollowUp e where e.owner.id = :id", "follow-up"},
            new String[]{"SiteVisit e where e.salesExecutive.id = :id", "site visit"},
            new String[]{"Booking e where e.salesExecutive.id = :id", "booking"},
            new String[]{"Task e where e.assignee.id = :id", "task"},
            new String[]{"Team e where e.manager.id = :id", "team managed"},
            new String[]{"User e where e.manager.id = :id", "direct report"});

    @Transactional
    public void delete(Long id) {
        User user = find(id);

        if (id.equals(SecurityUtils.currentUserId())) {
            throw new BusinessRuleException("You cannot delete your own account");
        }
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ADMIN);
        if (isAdmin && userRepository.findActiveByRole(RoleName.ADMIN).size() <= 1) {
            throw new BusinessRuleException("Cannot delete the last administrator");
        }

        List<String> blocking = new ArrayList<>();
        for (String[] ref : USER_REFERENCES) {
            long n = entityManager
                    .createQuery("select count(e) from " + ref[0], Long.class)
                    .setParameter("id", id)
                    .getSingleResult();
            if (n > 0) blocking.add(n + " " + ref[1] + (n == 1 ? "" : "s"));
        }
        if (!blocking.isEmpty()) {
            throw new BusinessRuleException(
                    user.getFullName() + " still has " + String.join(", ", blocking)
                            + ". Deactivate the account instead so this history is preserved.");
        }

        // Rows owned outright by the user and meaningless without them.
        entityManager.createQuery("delete from Notification n where n.recipient.id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("delete from RefreshToken t where t.user.id = :id")
                .setParameter("id", id).executeUpdate();

        UserResponse before = CrmMappers.user(user);
        user.getRoles().clear();          // clears the user_role join rows
        userRepository.delete(user);
        auditService.record("USER_DELETED", "User", id, before, null);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = find(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        auditService.record("USER_PASSWORD_RESET", "User", id, null, null);
    }

    public User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        Set<Role> roles = new HashSet<>();
        for (RoleName name : names) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Role " + name + " not found")));
        }
        return roles;
    }

    private Team resolveTeam(Long teamId) {
        if (teamId == null) return null;
        return teamRepository.findById(teamId).orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
    }
}
