package com.company.crm.test.user;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.user.User;
import com.company.crm.util.UniqueValues;
import com.company.crm.view.user.UserDetailView;
import com.company.crm.view.user.UserListView;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.JmixPasswordField;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.testassist.UiTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Sample UI integration test for the User entity.
 */
public class UserUiTest extends AbstractUiTest {

    @Test
    void test_createUser() {
        // Navigate to user list view
        UserListView userListView = viewTestSupport.navigateTo(UserListView.class);

        // Click the "Create" button
        JmixButton createBtn = UiTestUtils.getComponent(userListView, "createButton");
        createBtn.click();

        // Get user detail view
        UserDetailView userDetailView = UiTestUtils.getCurrentView();

        // Set username and password in the fields
        TypedTextField<String> usernameField = UiTestUtils.getComponent(userDetailView, "usernameField");
        String username = UniqueValues.string();
        usernameField.setValue(username);

        String password = UniqueValues.string();
        JmixPasswordField passwordField = UiTestUtils.getComponent(userDetailView, "passwordField");
        passwordField.setValue(password);

        JmixPasswordField confirmPasswordField = UiTestUtils.getComponent(userDetailView, "confirmPasswordField");
        confirmPasswordField.setValue(password);

        // Click "OK"
        JmixButton commitAndCloseBtn = UiTestUtils.getComponent(userDetailView, "saveAndCloseButton");
        commitAndCloseBtn.click();

        // Get navigated user list view
        userListView = UiTestUtils.getCurrentView();

        // Check the created user is shown in the table
        DataGrid<User> usersDataGrid = UiTestUtils.getComponent(userListView, "usersDataGrid");

        DataGridItems<User> usersDataGridItems = usersDataGrid.getItems();
        Assertions.assertNotNull(usersDataGridItems);

        usersDataGridItems.getItems().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow();
    }
}
