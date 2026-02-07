package com.company.crm.app.util.role;

import com.company.crm.model.user.User;
import com.company.crm.security.role.AdministratorRole;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.security.role.assignment.RoleAssignment;
import io.jmix.security.role.assignment.RoleAssignmentRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class RoleUtils {

    private final CurrentAuthentication currentAuthentication;
    private final RoleAssignmentRepository roleAssignmentRepository;

    public static final String[] ADMIN_ROLE_CODES = new String[]{AdministratorRole.CODE};

    public RoleUtils(CurrentAuthentication currentAuthentication, RoleAssignmentRepository roleAssignmentRepository) {
        this.currentAuthentication = currentAuthentication;
        this.roleAssignmentRepository = roleAssignmentRepository;
    }

    public boolean isUserHasRole(final User user, final String... roleCodes) {
        return isUserHasRole(user.getUsername(), roleCodes);
    }

    public boolean isUserHasRole(final String username, final String... roleCodes) {
        Collection<RoleAssignment> userRoleAssignments = roleAssignmentRepository.getAssignmentsByUsername(username);
        for (String roleCode : roleCodes) {
            boolean userHasRole = userRoleAssignments.stream().anyMatch(role -> roleCode.equals(role.getRoleCode()));
            if (userHasRole) {
                return true;
            }
        }

        return false;
    }

    public boolean isAdmin(final String username) {
        return isUserHasRole(username, ADMIN_ROLE_CODES);
    }

    public boolean isAdmin(UserDetails user) {
        return isAdmin(user.getUsername());
    }

    public boolean isCurrentUserAdmin() {
        return isAdmin(currentAuthentication.getUser());
    }
}
