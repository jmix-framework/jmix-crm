package com.company.crm.util;

import com.company.crm.model.user.User;
import com.company.crm.security.role.ManagerRole;
import com.company.crm.security.role.SupervisorRole;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@TestComponent
public class TestUsers {

    private final UnconstrainedDataManager dataManager;
    private final PasswordEncoder passwordEncoder;

    public TestUsers(UnconstrainedDataManager dataManager, PasswordEncoder passwordEncoder) {
        this.dataManager = dataManager;
        this.passwordEncoder = passwordEncoder;
    }

    public User ensureUser(String username) {
        return findUser(username).orElseGet(() -> createUser(username));
    }

    public User manager() {
        User user = ensureUser(ManagerRole.CODE);
        assignRole(user.getUsername(), ManagerRole.CODE);
        return user;
    }

    public User supervisor() {
        User user = ensureUser(SupervisorRole.CODE);
        assignRole(user.getUsername(), SupervisorRole.CODE);
        return user;
    }

    public void assignRole(String username, String roleCode) {
        boolean exists = dataManager.load(RoleAssignmentEntity.class)
                .query("e.username = ?1 and e.roleCode = ?2", username, roleCode)
                .optional()
                .isPresent();

        if (exists) {
            return;
        }

        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(username);
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        dataManager.save(assignment);
    }

    private Optional<User> findUser(String username) {
        return dataManager.load(User.class)
                .query("e.username = ?1", username)
                .optional();
    }

    private User createUser(String username) {
        User user = dataManager.create(User.class);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(username));
        user.setActive(true);
        return dataManager.save(user);
    }
}
