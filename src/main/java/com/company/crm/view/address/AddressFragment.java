package com.company.crm.view.address;

import com.company.crm.model.address.Address;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewValidation;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

@FragmentDescriptor("address-fragment.xml")
public class AddressFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private InstanceContainer<Address> addressDc;
    @Autowired
    private ViewValidation viewValidation;

    public ValidationErrors validate() {
        ValidationErrors validationErrors = viewValidation.validateUiComponents(getContent());
        viewValidation.showValidationErrors(validationErrors);
        return validationErrors;
    }

    public void setAddress(Address address) {
        addressDc.setItem(address);
    }

    @Nullable
    public Address getAddress() {
        return addressDc.getItemOrNull();
    }
}