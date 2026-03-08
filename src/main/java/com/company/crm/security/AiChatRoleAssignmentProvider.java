package com.company.crm.security;

import com.company.crm.security.role.AiChatUserRole;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.security.role.assignment.RoleAssignment;
import io.jmix.security.role.assignment.RoleAssignmentProvider;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component("crm_AiChatRoleAssignmentProvider")
public class AiChatRoleAssignmentProvider implements RoleAssignmentProvider {

    private final UnconstrainedDataManager dataManager;

    public AiChatRoleAssignmentProvider(UnconstrainedDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public Collection<RoleAssignment> getAllAssignments() {
        Set<String> usersWithResourceRoles = new HashSet<>(dataManager.loadValue(
                        "select distinct e.username from sec_RoleAssignmentEntity e where e.roleType = :resourceRoleType",
                        String.class)
                .parameter("resourceRoleType", RoleAssignmentRoleType.RESOURCE)
                .list());

        Set<String> usersWithExplicitChatRowLevelRole = new HashSet<>(dataManager.loadValue(
                        "select distinct e.username from sec_RoleAssignmentEntity e " +
                                "where e.roleCode = :roleCode and e.roleType = :rowLevelRoleType",
                        String.class)
                .parameter("roleCode", AiChatUserRole.CODE)
                .parameter("rowLevelRoleType", RoleAssignmentRoleType.ROW_LEVEL)
                .list());

        usersWithResourceRoles.removeAll(usersWithExplicitChatRowLevelRole);

        return usersWithResourceRoles.stream()
                .map(this::createChatRowLevelAssignment)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<RoleAssignment> getAssignmentsByUsername(String username) {
        if (!hasResourceRole(username) || hasExplicitChatRowLevelRole(username)) {
            return List.of();
        }

        return List.of(createChatRowLevelAssignment(username));
    }

    private boolean hasResourceRole(String username) {
        Long count = dataManager.loadValue(
                        "select count(e) from sec_RoleAssignmentEntity e " +
                                "where e.username = :username and e.roleType = :resourceRoleType",
                        Long.class)
                .parameter("username", username)
                .parameter("resourceRoleType", RoleAssignmentRoleType.RESOURCE)
                .one();

        return count != null && count > 0;
    }

    private boolean hasExplicitChatRowLevelRole(String username) {
        Long count = dataManager.loadValue(
                        "select count(e) from sec_RoleAssignmentEntity e " +
                                "where e.username = :username and e.roleCode = :roleCode and e.roleType = :rowLevelRoleType",
                        Long.class)
                .parameter("username", username)
                .parameter("roleCode", AiChatUserRole.CODE)
                .parameter("rowLevelRoleType", RoleAssignmentRoleType.ROW_LEVEL)
                .one();

        return count != null && count > 0;
    }

    private RoleAssignment createChatRowLevelAssignment(String username) {
        return new RoleAssignment(username, AiChatUserRole.CODE, RoleAssignmentRoleType.ROW_LEVEL);
    }
}
