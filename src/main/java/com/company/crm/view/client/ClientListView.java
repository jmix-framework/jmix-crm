package com.company.crm.view.client;

import com.company.crm.app.util.ui.component.model.card.CardModel;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.view.home.OrderStatusAmountItem;
import com.company.crm.view.home.OrderStatusAmountValueDescription;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.Grid;
import io.jmix.chartsflowui.kit.component.model.Title;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.PieSeries;
import io.jmix.chartsflowui.kit.component.model.shared.FontStyle;
import io.jmix.chartsflowui.kit.component.model.shared.Orientation;
import io.jmix.chartsflowui.kit.component.model.toolbox.SaveAsImageFeature;
import io.jmix.chartsflowui.kit.component.model.toolbox.Toolbox;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.Messages;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "clients", layout = MainView.class)
@ViewController(id = "Client.list")
@ViewDescriptor(path = "client-list-view.xml")
@LookupComponent("clientsDataGrid")
@DialogMode(width = "64em")
public class ClientListView extends StandardListView<Client> {

    @Autowired
    private ClientRepository repository;

    @ViewComponent
    private HorizontalLayout chartsBlock;
    @Autowired
    private Messages messages;
    @Autowired
    private UiComponents uiComponents;

    @Subscribe
    public void onInit(final InitEvent event) {
        initCharts();
    }

    @Install(to = "clientsDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Client> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return repository.findAll(pageable, context).getContent();
    }

    @Install(to = "clientsDataGrid.removeAction", subject = "delegate")
    private void clientsDataGridRemoveDelegate(final Collection<Client> collection) {
        repository.deleteAll(collection);
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return repository.count(context);
    }

    private void initCharts() {
        chartsBlock.removeAll();
        // createCharts().forEach(chartsBlock::add);
    }

    private List<Chart> createCharts() {
        return List.of(
                createBestBuyersChart(),
                createClientCategoriesChart(),
                createClientsByTypeChart()
        );
    }

    private Chart createBestBuyersChart() {
        return null;
    }

    private Chart createClientCategoriesChart() {
        return null;
    }

    private Chart createClientsByTypeChart() {
        Chart chart = uiComponents.create(Chart.class)
                // .withDataSet(createClientsByTypeChartDataSet(clientsByType))
                .withSeries(new PieSeries()
                        .withAnimation(true))
                .withToolbox(new Toolbox()
                        .withShow(true)
                        .withFeatures(new SaveAsImageFeature().withType(SaveAsImageFeature.SaveType.PNG)))
                .withTitle(new Title()
                        .withText("TYPE")
                        .withTextStyle(new Title.TextStyle()
                                .withFontSize(12)
                                .withFontStyle(FontStyle.NORMAL)))
                .withLegend(new Legend()
                        .withTop("20")
                        .withLeft("0")
                        .withOrientation(Orientation.VERTICAL))
                .withGrid(new Grid()
                        .withShow(false)
                        .withBottom("0")
                        .withRight("0"));

        chart.setHeight(30, Unit.EM);

        return chart;
    }

    private DataSet createClientsByTypeChartDataSet(Map<ClientType, List<Client>> clientsByType) {
        return new DataSet();
        /*var dataItems = new ArrayList<ClientTypeAmountItem>();
        for (Map.Entry<ClientType, List<Client>> entry : clientsByType.entrySet()) {
            ClientTypeAmountValueDescription valueDescription = new ClientTypeAmountValueDescription(
                    messages.getMessage(entry.getKey()), entry.getValue());
            dataItems.add(new ClientTypeAmountItem(valueDescription));
        }

        return new DataSet().withSource(
                new DataSet.Source<OrderStatusAmountItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("type")
                        .withValueField("amount")
        );*/
    }
}