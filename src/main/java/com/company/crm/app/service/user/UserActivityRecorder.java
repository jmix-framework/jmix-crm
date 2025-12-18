package com.company.crm.app.service.user;

import com.company.crm.model.base.CreateAuditEntity;
import com.company.crm.model.base.CreateUpdateAuditEntity;
import com.company.crm.model.base.FullAuditEntity;
import com.company.crm.model.base.UuidEntity;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.Order;
import com.company.crm.model.user.User;
import com.company.crm.model.user.activity.client.ClientUserActivity;
import com.company.crm.model.user.activity.userprofile.UserProfileUserActivity;
import io.jmix.core.DataManager;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.security.Authenticated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.stream;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

@Service
public class UserActivityRecorder {

    private final UnconstrainedDataManager dataManager;

    public UserActivityRecorder(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Authenticated
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEntityChanged(final EntityChangedEvent<? extends UuidEntity> event) {
        Optional<? extends UuidEntity> entityOpt = getEntityFromEvent(event);

        if (entityOpt.isEmpty()) {
            return;
        }

        switch (entityOpt.get()) {
            case User user -> processUserChanges(event, user);
            case Order order -> processOrderChanges(event, order);
            case Client client -> processClientChanges(event, client);
            default -> {}
        }
    }

    private void processUserChanges(EntityChangedEvent<?> event, User user) {
        if (isEntityUpdatedEvent(event)) {
            User updatedBy = getUpdatedBy(user).orElse(null);
            if (Objects.equals(user, updatedBy)) {
                UserProfileUserActivity userActivity = dataManager.create(UserProfileUserActivity.class);
                userActivity.setUser(user);
                userActivity.setActionDescription("Update profile info");
                dataManager.save(userActivity);
            }
        }
    }

    private void processClientChanges(EntityChangedEvent<?> event, Client client) {
        ClientUserActivity userActivity = dataManager.create(ClientUserActivity.class);
        userActivity.setClient(client);

        if (isEntityCreatedEvent(event)) {
            getCreatedBy(client).ifPresent(createdBy -> {
                userActivity.setUser(createdBy);
                userActivity.setActionDescription("Add new client to system");
            });
        } else if (isEntityUpdatedEvent(event)) {
            getUpdatedBy(client).ifPresent(updatedBy -> {
                userActivity.setUser(updatedBy);
                userActivity.setActionDescription("Update client profile");
            });
        } else if (isEntityDeletedEvent(event)) {
            getDeletedBy(client).ifPresent(deletedBy -> {
                userActivity.setUser(deletedBy);
                userActivity.setActionDescription("Delete client from system");
            });
        }

        if (userActivity.getUser() != null) {
            dataManager.save(userActivity);
        }
    }

    private void processOrderChanges(EntityChangedEvent<?> event, Order order) {
        ClientUserActivity userActivity = dataManager.create(ClientUserActivity.class);
        Client client = order.getClient();
        userActivity.setClient(client);

        String orderNumber = order.getNumber();

        if (isEntityCreatedEvent(event)) {
            getCreatedBy(order).ifPresent(createdBy -> {
                userActivity.setUser(createdBy);
                userActivity.setActionDescription("Create order %s".formatted(orderNumber));
            });
        } else if (isEntityUpdatedEvent(event)) {
            getUpdatedBy(order).ifPresent(updatedBy -> {
                userActivity.setUser(updatedBy);
                userActivity.setActionDescription("Update order %s".formatted(orderNumber));
            });
        } else if (isEntityDeletedEvent(event)) {
            getDeletedBy(order).ifPresent(deletedBy -> {
                userActivity.setUser(deletedBy);
                userActivity.setActionDescription("Delete order %s".formatted(orderNumber));
            });
        }

        if (userActivity.getUser() != null) {
            dataManager.save(userActivity);
        }
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

        return username.filter(u -> !List.of("admin", "system").contains(u))
                .flatMap(this::findUser);
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
