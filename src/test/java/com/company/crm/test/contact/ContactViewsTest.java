package com.company.crm.test.contact;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.contact.Contact;
import com.company.crm.view.contact.ContactDetailView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactViewsTest extends AbstractUiTest {

    @Test
    void opensContactDetailView() {
        var view = viewTestSupport.navigateToNewEntityDetail(Contact.class, ContactDetailView.class);
        assertThat(view).isInstanceOf(ContactDetailView.class);
    }
}
