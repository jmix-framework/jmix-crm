package com.company.crm.test.invoice;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderDiscountType;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.util.UniqueValues;
import com.company.crm.view.invoice.InvoiceDetailView;
import com.company.crm.view.order.OrderListView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.testassist.UiTestUtils;
import io.jmix.flowui.view.ViewControllerUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceTotalsViewTest extends AbstractUiTest {

    @Test
    void subtotalAndVatClientEditsRecalculateReadOnlyTotalAndPersist() {
        InvoiceDetailView view = newInvoice(entities.client());

        setClientAmount(view, "subtotalField", "100");
        setClientAmount(view, "vatField", "20");
        assertAmounts(view, "100", "20", "120");

        setClientAmount(view, "subtotalField", "200");
        assertAmounts(view, "200", "20", "220");
        setClientAmount(view, "vatField", "5");
        assertAmounts(view, "200", "5", "205");
        TypedTextField<BigDecimal> total = viewTestSupport.getComponent(view, "totalField");
        assertThat(total.isReadOnly()).isTrue();

        assertAmounts(saveAndReload(view), "200", "5", "205");
    }

    @Test
    void frameworkPriceRoundingKeepsLiveAndStoredTotalsConsistent() {
        InvoiceDetailView view = newInvoice(entities.client());

        setClientAmount(view, "subtotalField", "100.005");
        setClientAmount(view, "vatField", "20.005");

        // Price fields format client input with HALF_EVEN before updating the entity.
        assertAmounts(view.getEditedEntity(), "100.00", "20.00", "120.00");
        assertAmounts(view, "100.00", "20.00", "120.00");
        assertAmounts(saveAndReload(view), "100.00", "20.00", "120.00");
    }

    @Test
    void openingAndSavingAnExistingInvoicePreservesItsAmounts() {
        Order order = order("120");
        line(order, "100", "20");
        Invoice invoice = invoice(order, "100", "20");

        InvoiceDetailView view = viewTestSupport.navigateToDetailView(invoice, InvoiceDetailView.class);

        assertAmounts(view, "100", "20", "120");
        assertAmounts(saveAndReload(view), "100", "20", "120");
    }

    @Test
    void creatingAnInvoiceFromTheOrderListUsesTheOrderAmounts() {
        Order order = order("120");
        line(order, "100", "20");
        OrderListView view = viewTestSupport.navigateTo(OrderListView.class);
        CollectionContainer<Order> orders = ViewControllerUtils.getViewData(view).getContainer("ordersDc");
        DataGrid<Order> grid = viewTestSupport.getComponent(view, "ordersDataGrid");
        grid.select(findOrder(orders, order));

        Objects.requireNonNull(grid.getAction("addInvoice")).actionPerform(grid);

        InvoiceDetailView invoiceView = UiTestUtils.getLastOpenedViewDialog();
        assertThat(invoiceView).isNotNull();
        assertAmounts(invoiceView, "100", "20", "120");
        assertThat(invoiceView.getEditedEntity().getClient()).isEqualTo(order.getClient());
    }

    @Test
    void choosingAnOrderPreservesMixedVatAndTheOrderDiscount() {
        Order order = order("157.50");
        order.setDiscountType(OrderDiscountType.PERCENT);
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountValue(new BigDecimal("17.50"));
        saveWithoutReload(order);
        line(order, "100", "20");
        line(order, "50", "5");
        InvoiceDetailView view = newInvoice(order.getClient());

        chooseOrder(view, order);

        assertAmounts(view, "135", "22.50", "157.50");
        assertAmounts(saveAndReload(view), "135", "22.50", "157.50");
    }

    @Test
    void choosingAnOrderWithoutVatKeepsTheInvoiceVatZero() {
        Order order = order("100");
        line(order, "100", "0");
        InvoiceDetailView view = newInvoice(order.getClient());

        chooseOrder(view, order);

        assertAmounts(view, "100", "0", "100");
    }

    @Test
    void aNewInvoiceUsesTheNetAndVatAmountsRemainingAfterOtherInvoices() {
        Order order = order("175");
        line(order, "100", "20");
        line(order, "50", "5");
        invoice(order, "60", "10");
        InvoiceDetailView view = newInvoice(order.getClient());

        chooseOrder(view, order);

        assertAmounts(view, "90", "15", "105");
        assertAmounts(saveAndReload(view), "90", "15", "105");
    }

    @Test
    void clearingTheOrderOnANewInvoiceClearsItsAutofilledAmounts() {
        Order order = order("120");
        line(order, "100", "20");
        InvoiceDetailView view = newInvoice(order.getClient());
        chooseOrder(view, order);

        chooseOrder(view, null);

        assertThat(view.getEditedEntity().getOrder()).isNull();
        assertAmounts(view, "0", "0", "0");
    }

    private InvoiceDetailView newInvoice(Client client) {
        InvoiceDetailView view = viewTestSupport.navigateToNewEntityDetail(Invoice.class, InvoiceDetailView.class);
        EntityComboBox<Client> clientField = viewTestSupport.getComponent(view, "clientsComboBox");
        clientField.setValue(client);
        return view;
    }

    private Order order(String total) {
        return entities.order(entities.client(), LocalDate.now(), OrderStatus.NEW, new BigDecimal(total));
    }

    private void line(Order order, String net, String vat) {
        CategoryItem product = entities.categoryItem("Invoice product", UniqueValues.string(), null,
                new BigDecimal(net), UomType.PIECES);
        entities.createAndSaveEntity(OrderItem.class, item -> {
            item.setOrder(order);
            item.setCategoryItem(product);
            item.setQuantity(BigDecimal.ONE);
            item.setNetPrice(new BigDecimal(net));
            item.setVat(new BigDecimal(vat));
            item.setGrossPrice(new BigDecimal(net).add(new BigDecimal(vat)));
        });
    }

    private Invoice invoice(Order order, String subtotal, String vat) {
        return entities.createAndSaveEntity(Invoice.class, invoice -> {
            invoice.setClient(order.getClient());
            invoice.setOrder(order);
            invoice.setDate(LocalDate.now());
            invoice.setStatus(InvoiceStatus.NEW);
            invoice.setSubtotal(new BigDecimal(subtotal));
            invoice.setVat(new BigDecimal(vat));
            invoice.setTotal(new BigDecimal(subtotal).add(new BigDecimal(vat)));
        });
    }

    private Order findOrder(CollectionContainer<Order> orders, Order expected) {
        return orders.getItems().stream().filter(order -> order.getId().equals(expected.getId()))
                .findFirst().orElseThrow();
    }

    private void chooseOrder(InvoiceDetailView view, Order order) {
        EntityComboBox<Order> field = viewTestSupport.getComponent(view, "ordersComboBox");
        Order oldValue = field.getValue();
        CollectionContainer<Order> orders = ViewControllerUtils.getViewData(view).getContainer("ordersDc");
        field.setValue(order == null ? null : findOrder(orders, order));
        ComponentUtil.fireEvent(field, new ComponentValueChangeEvent<>(field, field, oldValue, true));
    }

    private void setClientAmount(InvoiceDetailView view, String id, String value) {
        TypedTextField<BigDecimal> field = viewTestSupport.getComponent(view, id);
        field.getElement().getNode().getFeature(ElementPropertyMap.class).updateFromClient("value", value);
    }

    private Invoice saveAndReload(InvoiceDetailView view) {
        assertThat(UiTestUtils.validateView(view).getAll()).isEmpty();
        Invoice invoice = view.getEditedEntity();
        viewTestSupport.click(view, "saveAndCloseButton");
        return dataManager.load(Invoice.class).id(invoice.getId()).one();
    }

    private void assertAmounts(InvoiceDetailView view, String subtotal, String vat, String total) {
        assertField(view, "subtotalField", subtotal);
        assertField(view, "vatField", vat);
        assertField(view, "totalField", total);
    }

    private void assertAmounts(Invoice invoice, String subtotal, String vat, String total) {
        assertThat(invoice.getSubtotal()).isEqualByComparingTo(subtotal);
        assertThat(invoice.getVat()).isEqualByComparingTo(vat);
        assertThat(invoice.getTotal()).isEqualByComparingTo(total);
    }

    private void assertField(InvoiceDetailView view, String id, String expected) {
        TypedTextField<BigDecimal> field = viewTestSupport.getComponent(view, id);
        assertThat(field.getTypedValue()).as(id).isEqualByComparingTo(expected);
    }
}
