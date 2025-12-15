package com.company.crm.app.ui.action;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import io.jmix.core.CoreProperties;
import io.jmix.core.MessageTools;
import io.jmix.core.Messages;
import io.jmix.core.security.ClientDetails;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.security.SecurityContextHelper;
import io.jmix.core.security.SystemAuthenticationToken;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.action.ActionType;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.action.usermenu.UserMenuAction;
import io.jmix.flowui.component.usermenu.UserMenu;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.kit.component.usermenu.TextUserMenuItem;
import io.jmix.flowui.kit.component.usermenu.UserMenuItem;
import io.jmix.flowui.kit.component.usermenu.UserMenuItem.HasSubMenu;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static com.company.crm.app.util.ui.CrmUiUtils.reloadCurrentPage;

@ActionType(UserMenuLanguageSwitchAction.ID)
public class UserMenuLanguageSwitchAction extends UserMenuAction<UserMenuLanguageSwitchAction, UserMenu> {

    public static final String ID = "userMenu_languageSwitch";

    protected Dialogs dialogs;
    protected Messages messages;
    protected MessageTools messageTools;
    protected CoreProperties coreProperties;
    protected CurrentAuthentication currentAuthentication;
    protected AuthenticationManager authenticationManager;

    protected final Map<String, UserMenuItem> menuItems = new HashMap<>(3);
    protected HasSubMenu.SubMenu subMenu;

    public UserMenuLanguageSwitchAction() {
        this(ID);
    }

    public UserMenuLanguageSwitchAction(String id) {
        super(id);
    }

    @Override
    protected void initAction() {
        super.initAction();
        icon = VaadinIcon.GLOBE.create();
    }

    @Override
    protected void setMenuItemInternal(@Nullable UserMenuItem menuItem) {
        super.setMenuItemInternal(menuItem);

        if (subMenu != null) {
            subMenu.removeAll();
            subMenu = null;
        }

        if (menuItem == null) {
            return;
        }

        if (!(menuItem instanceof HasSubMenu hasSubMenu)) {
            throw new IllegalStateException("%s does not implement %s"
                    .formatted(menuItem, HasSubMenu.class.getSimpleName()));
        }

        subMenu = hasSubMenu.getSubMenu();
        initItems(subMenu);
    }

    protected void initItems(HasSubMenu.SubMenu subMenu) {
        coreProperties.getAvailableLocales().forEach(locale ->
                menuItems.put(locale.getLanguage(), createItem(subMenu, locale,
                        e -> changeLanguage(locale))));
    }

    protected UserMenuItem createItem(HasSubMenu.SubMenu subMenu, Locale locale,
                                      Consumer<UserMenuItem.HasClickListener.ClickEvent<TextUserMenuItem>> listener) {
        String itemId = "%s_%sUserMenuItem".formatted(ID, locale);
        String message = messages.getMessage(locale.getLanguage().toLowerCase());
        UserMenuItem menuItem = subMenu.addTextItem(itemId, message, createIcon(locale), listener);
        menuItem.setCheckable(true);
        menuItem.setChecked(locale.equals(getLocaleFromCurrentAuthentication()));
        return menuItem;
    }

    protected Avatar createIcon(Locale locale) {
        String languageCode = locale.getLanguage().toUpperCase();
        Avatar avatar = new Avatar(languageCode);
        avatar.setAbbreviation(languageCode);
        return avatar;
    }

    @Autowired
    public void setDialogs(Dialogs dialogs) {
        this.dialogs = dialogs;
    }

    @Autowired
    public void setMessages(Messages messages) {
        this.messages = messages;
        this.text = messages.getMessage("language");
    }

    @Autowired
    public void setCoreProperties(CoreProperties coreProperties) {
        this.coreProperties = coreProperties;
    }

    @Autowired
    public void setMessageTools(MessageTools messageTools) {
        this.messageTools = messageTools;
    }

    @Autowired
    public void setCurrentAuthentication(CurrentAuthentication currentAuthentication) {
        this.currentAuthentication = currentAuthentication;
    }

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void execute() {
        // do nothing
    }

    private Locale getLocaleFromCurrentAuthentication() {
        try {
            return currentAuthentication.getLocale();
        } catch (Exception e) {
            return getDefaultLocale();
        }
    }

    private Locale getDefaultLocale() {
        return messageTools.getDefaultLocale();
    }

    private void changeLanguage(Locale locale) {
        showYesNoDialog(
                messages.getMessage("changeLanguageNotificationTitle"),
                messages.getMessage("changeLanguageNotificationMessage"),
                answer -> {
                    if (answer) doChangeLanguage(locale);
                }
        );
    }

    private void doChangeLanguage(Locale locale) {
        Authentication currAuth = currentAuthentication.getAuthentication();
        // If user has logged in through LoginView
        if (currAuth.getDetails() instanceof ClientDetails currDetails) {
            // Set new locale
            VaadinSession.getCurrent().setLocale(locale);
            // Copy client details from current authentication and change locale
            ClientDetails newDetails = ClientDetails.builder().of(currDetails)
                    .locale(VaadinSession.getCurrent().getLocale())
                    .build();

            // Use system token since we cannot get raw user's password.
            // And set current authorities
            SystemAuthenticationToken authentication = new SystemAuthenticationToken(
                    currentAuthentication.getUser(),
                    currentAuthentication.getUser().getAuthorities());
            authentication.setDetails(newDetails);

            Authentication newAuth;
            try {
                newAuth = authenticationManager.authenticate(authentication);
            } catch (AuthenticationException e) {
                Notification.show("Error on changing locale");
                return;
            }

            SecurityContextHelper.setAuthentication(newAuth);
            reloadCurrentPage();
        }
    }

    private void showYesNoDialog(String header, String message,
                                 Consumer<Boolean> resultHandler) {
        doShowYesNoDialog(header, message, null, null, resultHandler);
    }

    private void doShowYesNoDialog(String header, String message,
                                   @Nullable String yesText,
                                   @Nullable String noText,
                                   Consumer<Boolean> resultHandler) {
        DialogAction yesAction = createYesAction(resultHandler, yesText);
        DialogAction noAction = createNoAction(resultHandler, noText);
        createOptionDialog(header, message, yesAction, noAction);
    }

    private void createOptionDialog(String header, String message, DialogAction... actions) {
        dialogs.createOptionDialog()
                .withHeader(header)
                .withText(message)
                .withActions(actions)
                .open();
    }

    private DialogAction createYesAction(Consumer<Boolean> resultHandler, @Nullable String text) {
        DialogAction action = new DialogAction(DialogAction.Type.YES)
                .withVariant(ActionVariant.PRIMARY)
                .withHandler(e -> resultHandler.accept(true));
        if (StringUtils.isNotBlank(text)) {
            action.withText(text);
        }
        return action;
    }

    private DialogAction createNoAction(Consumer<Boolean> resultHandler, @Nullable String text) {
        DialogAction action = new DialogAction(DialogAction.Type.NO)
                .withHandler(e -> resultHandler.accept(false));
        if (StringUtils.isNotBlank(text)) {
            action.withText(text);
        }
        return action;
    }
}
