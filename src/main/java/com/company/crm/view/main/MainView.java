package com.company.crm.view.main;

import com.company.crm.app.ui.component.CrmLoader;
import com.company.crm.app.util.demo.DemoUtils;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.user.User;
import com.company.crm.view.client.ClientListView;
import com.company.crm.view.home.HomeView;
import com.google.common.base.Strings;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.app.main.StandardMainView;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.component.SupportsTypedValue.TypedValueChangeEvent;
import io.jmix.flowui.component.main.JmixListMenu;
import io.jmix.flowui.component.main.JmixListMenu.ViewMenuItem;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.virtuallist.JmixVirtualList;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.main.ListMenu.MenuBarItem;
import io.jmix.flowui.kit.component.main.ListMenu.MenuItem;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.company.crm.app.util.demo.DemoUtils.defaultSleepForSearchClient;

@Route("")
@ViewController(id = "MainView")
@ViewDescriptor(path = "main-view.xml")
public class MainView extends StandardMainView {

    @Autowired
    private Messages messages;
    @Autowired
    private Metadata metadata;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private UiAsyncTasks uiAsyncTasks;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private CurrentUserSubstitution currentUserSubstitution;

    @ViewComponent
    private JmixListMenu menu;
    @ViewComponent
    private TypedTextField<String> searchField;
    @ViewComponent
    private JmixButton notificationsButton;

    final Popover[] searchPopover = {null};
    final Popover[] notificationsPopover = {null};

    @Subscribe
    private void onReady(final ReadyEvent event) {
        selectSuitableMenuItem();
    }

    @Subscribe("userMenu.profileItem.profileAction")
    private void onUserMenuProfileItemProfileAction(final ActionPerformedEvent event) {
        dialogWindows.detail(this, User.class)
                .editEntity(((User) currentAuthentication.getUser()))
                .open();
    }

    @Install(to = "userMenu", subject = "buttonRenderer")
    private Component userMenuButtonRenderer(final UserDetails userDetails) {
        if (!(userDetails instanceof User user)) {
            return null;
        }

        String userName = generateUserName(user);

        Div content = uiComponents.create(Div.class);
        content.setClassName("user-menu-button-content");

        Avatar avatar = createAvatar(userName);

        Span name = uiComponents.create(Span.class);
        name.setText(userName);
        name.setClassName("user-menu-text");

        content.add(avatar, name);

        if (isSubstituted(user)) {
            Span subtext = uiComponents.create(Span.class);
            subtext.setText(messages.getMessage("userMenu.substituted"));
            subtext.setClassName("user-menu-subtext");

            content.add(subtext);
        }

        return content;
    }

    @Install(to = "userMenu", subject = "headerRenderer")
    private Component userMenuHeaderRenderer(final UserDetails userDetails) {
        User user = (User) userDetails;

        if (user == null) {
            return null;
        }

        Div content = uiComponents.create(Div.class);
        content.setClassName("user-menu-header-content");

        String name = generateUserName(user);

        Avatar avatar = createAvatar(name);
        avatar.addThemeVariants(AvatarVariant.LUMO_LARGE);

        Span text = uiComponents.create(Span.class);
        text.setText(name);
        text.setClassName("user-menu-text");

        content.add(avatar, text);

        if (name.equals(user.getUsername())) {
            text.addClassNames("user-menu-text-subtext");
        } else {
            Span subtext = uiComponents.create(Span.class);
            subtext.setText(user.getUsername());
            subtext.setClassName("user-menu-subtext");

            content.add(subtext);
        }

        return content;
    }

    @Subscribe("searchField")
    private void onSearchFieldTypedValueChange(final TypedValueChangeEvent<TypedTextField<String>, String> event) {
        onSearchFieldValueChange(event);
    }

    @Subscribe(id = "notificationsButton", subject = "doubleClickListener")
    public void onNotificationsButtonDoubleClick(final ClickEvent<JmixButton> event) {
        onNotificationButtonClick();
    }

    @Subscribe(id = "notificationsButton", subject = "clickListener")
    private void onNotificationsButtonSingleClick(final ClickEvent<JmixButton> event) {
        onNotificationButtonClick();
    }

    @Subscribe(id = "applicationTitle", subject = "clickListener")
    private void onApplicationTitleClick(final ClickEvent<H2> event) {
        UI currentUI = UI.getCurrent();
        if (currentUI == null) {
            return;
        }

        Component currentView = currentUI.getCurrentView();
        if (currentView == null) {
            return;
        }

        if (currentView instanceof HomeView homeView) {
            homeView.getUI().ifPresent(ui -> ui.getPage().reload());
        } else {
            viewNavigators.view(this, HomeView.class).navigate();
        }
    }

    private Avatar createAvatar(String fullName) {
        Avatar avatar = uiComponents.create(Avatar.class);
        avatar.setName(fullName);
        avatar.getElement().setAttribute("tabindex", "-1");
        avatar.setClassName("user-menu-avatar");

        return avatar;
    }

    private String generateUserName(User user) {
        String userName = String.format("%s %s",
                        Strings.nullToEmpty(user.getFirstName()),
                        Strings.nullToEmpty(user.getLastName()))
                .trim();

        return userName.isEmpty() ? user.getUsername() : userName;
    }

    private boolean isSubstituted(User user) {
        UserDetails authenticatedUser = currentUserSubstitution.getAuthenticatedUser();
        return user != null && !authenticatedUser.getUsername().equals(user.getUsername());
    }

    private void onNotificationButtonClick() {
        Optional.ofNullable(notificationsPopover[0]).ifPresent(Popover::removeFromParent);

        HorizontalLayout layout = new HorizontalLayout(VaadinIcon.CHECK.create(), new Span("Notifications not found"));
        layout.setSizeFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Popover popover = new Popover(layout);
        popover.setTarget(notificationsButton);
        popover.setWidth("16em");
        popover.setHeight("4em");
        popover.setCloseOnEsc(true);
        popover.setCloseOnOutsideClick(true);
        popover.open();

        notificationsPopover[0] = popover;
    }

    private void onSearchFieldValueChange(TypedValueChangeEvent<TypedTextField<String>, String> event) {
        Optional.ofNullable(searchPopover[0]).ifPresent(Popover::removeFromParent);

        String searchText = event.getValue();
        if (StringUtils.isBlank(searchText) || !event.isFromClient()) {
            return;
        }

        searchPopover[0] = showSearchFieldPopover(searchText);
    }

    private Popover showSearchFieldPopover(String searchText) {
        CrmLoader loader = new CrmLoader();
        loader.setSizeFull();
        Popover popover = new Popover(loader);
        popover.setTarget(searchField);
        popover.setWidth("25em");
        popover.setHeight("10em");
        popover.setCloseOnEsc(true);
        popover.setCloseOnOutsideClick(true);
        popover.open();
        loader.startLoading();

        var clientsSize = 10;
        uiAsyncTasks.supplierConfigurer(() -> searchClientsByName(searchText, clientsSize))
                .withResultHandler(clients -> updateSearchPopover(clients, clientsSize, popover))
                .withExceptionHandler(e -> {
                    popover.removeAll();
                    popover.add(new Span("Something went wrong"));
                })
                .supplyAsync();

        return popover;
    }

    private void updateSearchPopover(List<Client> clients, int clientsSize, Popover popover) {
        Client showAll = metadata.create(Client.class);
        showAll.setName("Show all...");
        if (clients.size() <= clientsSize) {
            clients.add(showAll);
        }

        JmixVirtualList<Client> virtualList = uiComponents.create(JmixVirtualList.class);
        virtualList.setItems(clients);

        popover.removeAll();
        popover.add(virtualList);
        popover.setWidth("25em");
        popover.setHeight(Math.min(clients.size() * 1.8, 25) + "em");

        virtualList.setRenderer(createClientsListRenderer(showAll, popover));
    }

    private ComponentRenderer<Component, Client> createClientsListRenderer(Client showAll, Popover popover) {
        return new ComponentRenderer<>(client -> {
            boolean isShowAll = client.equals(showAll);

            JmixButton button = uiComponents.create(JmixButton.class);
            button.setText(client.getName());
            button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_CONTRAST);
            button.setIcon(isShowAll ? VaadinIcon.EXTERNAL_LINK.create() : VaadinIcon.USER.create());

            button.addClickListener(click -> {
                popover.close();
                searchField.clear();
                if (isShowAll) {
                    viewNavigators.view(this, ClientListView.class)
                            .navigate();
                } else {
                    viewNavigators.detailView(this, Client.class)
                            .editEntity(client)
                            .navigate();
                }
            });
            return button;
        });
    }

    private List<Client> searchClientsByName(String name, int size) {
        defaultSleepForSearchClient();
        return clientRepository.findAllByNameContains(name, Pageable.ofSize(size));
    }

    private void selectSuitableMenuItem() {
        getUI().map(UI::getCurrentView)
                .filter(View.class::isInstance)
                .map(View.class::cast)
                .map(v -> v.getClass())
                .ifPresent(this::selectRelatedMenuItem);
    }

    private void selectRelatedMenuItem(Class<? extends View> viewClass) {
        MenuItemStructure menuStructure = buildMenuStructure();
        for (MenuItemInfo itemInfo : menuStructure.itemsInfo()) {
            if (itemInfo.menuItem() instanceof ViewMenuItem viewMenuItem) {
                if (viewClass.equals(viewMenuItem.getControllerClass())) {
                    Optional.ofNullable(itemInfo.parentMenuItem()).ifPresent(parent -> {
                        if (parent instanceof MenuBarItem menuBarItem) {
                            menuBarItem.setOpened(true);
                        }
                    });
                    break;
                }
            }
        }
    }

    private MenuItemStructure buildMenuStructure() {
        MenuItemStructure menuStructure = new MenuItemStructure();
        for (MenuItem menuItem : menu.getMenuItems()) {
            buildMenuStructureRecursively(menuItem, null, menuStructure);
        }
        return menuStructure;
    }

    private void buildMenuStructureRecursively(MenuItem menuItem, MenuItem parentMenuItem, MenuItemStructure menuStructure) {
        menuStructure.addInfo(new MenuItemInfo(menuItem, parentMenuItem));
        if (menuItem instanceof MenuBarItem menuBarItem) {
            for (MenuItem childItem : menuBarItem.getChildItems()) {
                buildMenuStructureRecursively(childItem, menuItem, menuStructure);
            }
        }
    }

    private record MenuItemStructure(Collection<MenuItemInfo> itemsInfo) {

        public MenuItemStructure() {
            this(new ArrayList<>());
        }

        @Override
        public Collection<MenuItemInfo> itemsInfo() {
            return List.of(itemsInfo.toArray(new MenuItemInfo[0]));
        }

        public void addInfo(MenuItemInfo menuItemInfo) {
            itemsInfo.add(menuItemInfo);
        }
    }

    private record MenuItemInfo(MenuItem menuItem,
                                @Nullable MenuItem parentMenuItem) {
    }
}
