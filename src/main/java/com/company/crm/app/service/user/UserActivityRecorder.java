package com.company.crm.app.service.user;

import com.company.crm.app.annotation.TrackedByUserActivityRecorder;
import com.company.crm.model.base.CreateAuditEntity;
import com.company.crm.model.base.CreateUpdateAuditEntity;
import com.company.crm.model.base.FullAuditEntity;
import com.company.crm.model.base.UuidEntity;
import com.company.crm.model.user.User;
import com.company.crm.model.user.UserActivity;
import io.jmix.core.DataManager;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.security.Authenticated;
import io.jmix.core.security.SystemAuthenticator;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.union;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

@Service
public class UserActivityRecorder {

    private static final Logger log = LoggerFactory.getLogger(UserActivityRecorder.class);

    private static final List<String> defaultIgnoredUsernames = List.of("admin", "system");

    private final UnconstrainedDataManager dataManager;
    private final SystemAuthenticator systemAuthenticator;


    public UserActivityRecorder(DataManager dataManager, SystemAuthenticator systemAuthenticator) {
        this.dataManager = dataManager;
        this.systemAuthenticator = systemAuthenticator;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEntityChanged(final EntityChangedEvent<? extends UuidEntity> event) {
        systemAuthenticator.runWithSystem(() -> recordUserActivitiesIfNeeded(event));
    }

    private void recordUserActivitiesIfNeeded(EntityChangedEvent<? extends UuidEntity> event) {
        List<UserActivity> userActivities = new ArrayList<>();
        userActivities.addAll(checkTrackedEntity(event));
        userActivities.addAll(checkUserProfileUpdate(event));
        userActivities.forEach(userActivity -> {
            String logMsg = "%s '%s' has been created for %s".formatted(
                    UserActivity.class.getSimpleName(),
                    userActivity.getActionDescription(),
                    userActivity.getUser());
            log.info(logMsg);
        });
    }

    private List<UserActivity> checkTrackedEntity(EntityChangedEvent<? extends UuidEntity> event) {
        Optional<? extends UuidEntity> entityOpt = getEntityFromEvent(event);
        if (entityOpt.isEmpty()) {
            return emptyList();
        }

        UuidEntity entity = entityOpt.get();

        if (findAnnotation(entity.getClass(), TrackedByUserActivityRecorder.class) == null) {
            return emptyList();
        }

        String entityClassName = entity.getClass().getSimpleName();

        List<UserActivity> userActivities = new ArrayList<>();
        if (isEntityCreatedEvent(event)) {
            getCreatedBy(entity).ifPresent(createdBy ->
                    userActivities.add(createUserActivity(createdBy, "Create the %s".formatted(entityClassName))));
        } else if (isEntityUpdatedEvent(event)) {
            getUpdatedBy(entity).ifPresent(updatedBy ->
                    userActivities.add(createUserActivity(updatedBy, "Update the %s".formatted(entityClassName))));
        } else if (isEntityDeletedEvent(event)) {
            getDeletedBy(entity).ifPresent(deletedBy ->
                    userActivities.add(createUserActivity(deletedBy, "Delete the %s".formatted(entityClassName))));
        }

        return userActivities;
    }

    private List<UserActivity> checkUserProfileUpdate(EntityChangedEvent<? extends UuidEntity> event) {
        if (!isEntityDeletedEvent(event)) {
            Optional<? extends UuidEntity> entityOpt = getEntityFromEvent(event);
            if (entityOpt.isEmpty()) {
                return emptyList();
            }

            UuidEntity entity = entityOpt.get();

            if (entity instanceof User updatedUser) {
                User updatedBy = getUpdatedBy(entity).orElse(null);
                if (isEntityUpdatedEvent(event) && Objects.equals(updatedUser, updatedBy)) {
                    return List.of(createUserActivity(updatedUser, "Update profile"));
                }
            }
        }

        return emptyList();
    }

    private Optional<User> getCreatedBy(UuidEntity entity) {
        return getChangesAuthorByTrait(entity, Trait.CREATED_BY);
    }

    private Optional<User> getUpdatedBy(UuidEntity entity) {
        return getChangesAuthorByTrait(entity, Trait.UPDATED_BY);
    }

    private Optional<User> getDeletedBy(UuidEntity entity) {
        return getChangesAuthorByTrait(entity, Trait.DELETED_BY);
    }

    private Optional<User> getChangesAuthorByTrait(UuidEntity entity, Trait trait) {
        Optional<String> username = switch (trait) {
            case CREATED_BY -> entity instanceof CreateAuditEntity createUpdateAudit
                    ? Optional.ofNullable(createUpdateAudit.getCreatedBy())
                    : Optional.empty();

            case UPDATED_BY -> entity instanceof CreateUpdateAuditEntity createUpdateAudit
                    ? Optional.ofNullable(createUpdateAudit.getUpdatedBy())
                    : Optional.empty();

            case DELETED_BY -> entity instanceof FullAuditEntity fullAuditEntity
                    ? Optional.ofNullable(fullAuditEntity.getDeletedBy())
                    : Optional.empty();
        };

        return username.filter(u -> !defaultIgnoredUsernames.contains(u)).flatMap(this::findUser);
    }

    private Optional<User> findUser(String username) {
        return dataManager.load(User.class)
                .query("e.username = ?1", username)
                .maxResults(1)
                .optional();
    }

    private enum Trait {
        CREATED_BY, UPDATED_BY, DELETED_BY
    }

    private UserActivity createUserActivity(User user, String activity) {
        UserActivity userActivity = dataManager.create(UserActivity.class);
        userActivity.setUser(user);
        userActivity.setActionDescription(activity);
        return dataManager.save(userActivity);
    }

    private boolean isEntityDeletedEvent(EntityChangedEvent<?> event) {
        return hasChangeOfType(event, EntityChangedEvent.Type.DELETED);
    }

    private boolean isEntityCreatedEvent(EntityChangedEvent<?> event) {
        return hasChangeOfType(event, EntityChangedEvent.Type.CREATED);
    }

    private boolean isEntityUpdatedEvent(EntityChangedEvent<?> event) {
        return hasChangeOfType(event, EntityChangedEvent.Type.UPDATED);
    }

    private boolean hasChangeOfType(EntityChangedEvent<?> event, EntityChangedEvent.Type type) {
        return event.getType().equals(type);
    }

    private boolean isEntityAttributeHasChanged(EntityChangedEvent<?> event, String attributeName) {
        return event.getChanges().isChanged(attributeName);
    }

    private <E extends UuidEntity> Optional<E> getEntityFromEvent(EntityChangedEvent<E> event) {
        if (isEntityDeletedEvent(event)) {
            return Optional.empty();
        } else {
            return dataManager.load(event.getEntityId()).optional();
        }
    }
}
