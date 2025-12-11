package com.company.crm.view.user;

import com.company.crm.app.util.role.RoleUtils;
import com.company.crm.model.user.User;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import io.jmix.securityflowui.view.changepassword.ChangePasswordView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

@Route(value = "users/:id", layout = MainView.class)
@ViewController(id = "User.detail")
@ViewDescriptor(path = "user-detail-view.xml")
@EditedEntityContainer("userDc")
public class UserDetailView extends StandardDetailView<User> {

    @Autowired
    private RoleUtils roleUtils;
    @Autowired
    private EntityStates entityStates;
    @Autowired
    private Notifications notifications;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @ViewComponent
    private TypedTextField<String> usernameField;
    @ViewComponent
    private PasswordField passwordField;
    @ViewComponent
    private PasswordField confirmPasswordField;
    @ViewComponent
    private ComboBox<String> timeZoneField;
    @ViewComponent
    private MessageBundle messageBundle;
    @ViewComponent
    private JmixButton changePasswordButton;

    private boolean newEntity;
    @Autowired
    private DialogWindows dialogWindows;

    @Subscribe
    private void onInit(final InitEvent event) {
        timeZoneField.setItems(List.of(TimeZone.getAvailableIDs()));
    }

    @Subscribe
    private void onInitEntity(final InitEntityEvent<User> event) {
        usernameField.setReadOnly(false);
    }

    @Subscribe
    private void onReady(final ReadyEvent event) {
        User editedEntity = getEditedEntity();
        if (entityStates.isNew(editedEntity)) {
            showPasswordFields();
            usernameField.focus();
        } else {
            showChangePasswordButton(editedEntity);
        }
    }

    private void showChangePasswordButton(User editedEntity) {
        UserDetails currentUser = currentAuthentication.getUser();
        boolean canChangePassword = editedEntity.equals(currentUser) || roleUtils.isAdmin(currentUser);
        changePasswordButton.setVisible(canChangePassword);
        if (canChangePassword) {
            changePasswordButton.addClickListener(e -> {
                showChangePasswordDialog(editedEntity);
            });
        }
    }

    private void showPasswordFields() {
        passwordField.setVisible(true);
        confirmPasswordField.setVisible(true);
    }

    @Subscribe
    private void onValidation(final ValidationEvent event) {
        if (entityStates.isNew(getEditedEntity())
                && !Objects.equals(passwordField.getValue(), confirmPasswordField.getValue())) {
            event.getErrors().add(messageBundle.getMessage("passwordsDoNotMatch"));
        }
    }

    @Subscribe
    private void onBeforeSave(final BeforeSaveEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            getEditedEntity().setPassword(passwordEncoder.encode(passwordField.getValue()));
            newEntity = true;
        }
    }

    @Subscribe
    private void onAfterSave(final AfterSaveEvent event) {
        if (newEntity) {
            notifications.create(messageBundle.getMessage("noAssignedRolesNotification"))
                    .withThemeVariant(NotificationVariant.LUMO_WARNING)
                    .withPosition(Notification.Position.TOP_END)
                    .show();

            newEntity = false;
        }
    }

    private void showChangePasswordDialog(User editedEntity) {
        DialogWindow<ChangePasswordView> dialog = dialogWindows
                .view(this, ChangePasswordView.class)
                .build();
        ChangePasswordView view = dialog.getView();
        view.setUsername(editedEntity.getUsername());
        view.setCurrentPasswordRequired(true);
        dialog.open();
    }
}