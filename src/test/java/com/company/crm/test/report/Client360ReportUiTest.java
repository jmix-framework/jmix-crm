package com.company.crm.test.report;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.client.Client;
import com.company.crm.report.Client360Report;
import com.company.crm.view.client.ClientDetailView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.flowui.OpenedDialogWindows;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.download.DownloadFormat;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.flowui.view.ViewValidation;
import io.jmix.reports.entity.Report;
import io.jmix.reportsflowui.download.ReportDownloader;
import io.jmix.reportsflowui.view.run.InputParametersDialog;
import io.jmix.reportsflowui.view.run.InputParametersFragment;
import io.jmix.reportsflowui.view.run.ReportRunView;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-key",
        "jmix.reports.client.use-background-report-processing=false"
})
@Import(Client360ReportUiTest.DownloadConfiguration.class)
class Client360ReportUiTest extends AbstractUiTest {

    @Autowired
    private ViewValidation viewValidation;
    @Autowired
    private DatatypeFormatter datatypeFormatter;
    @Autowired
    private ReportDownloader reportDownloader;

    @Override
    protected void beforeEach() {
        reset(reportDownloader);
    }

    @Test
    void runningFromReportsPromptsForRequiredParametersAndGeneratesHtml() {
        Client client = entities.client("Manual report client");
        ReportRunView view = viewTestSupport.navigateTo(ReportRunView.class);
        CollectionContainer<Report> reports = ViewControllerUtils.getViewData(view).getContainer("reportsDc");
        Report report = reports.getItems().stream()
                .filter(item -> Client360Report.CODE.equals(item.getCode()))
                .findFirst().orElseThrow();
        DataGrid<Report> grid = viewTestSupport.getComponent(view, "reportDataGrid");
        grid.select(report);

        Objects.requireNonNull(grid.getAction("runReport")).actionPerform(grid);

        InputParametersDialog dialog = (InputParametersDialog) openedDialogWindows().getCurrentDialog().orElse(null);
        assertThat(dialog).isNotNull();
        Component layout = viewTestSupport.getComponent(dialog, "inputParametersLayout");
        InputParametersFragment fragment = layout.getChildren()
                .filter(InputParametersFragment.class::isInstance)
                .map(InputParametersFragment.class::cast)
                .findFirst().orElseThrow();
        for (String alias : List.of("client", "fromDate", "toDate")) {
            assertThat(parameterField(fragment, alias).isVisible()).as(alias).isTrue();
            assertThat(fragment.getReport().getInputParameters().stream()
                    .filter(parameter -> alias.equals(parameter.getAlias())).findFirst().orElseThrow().getRequired())
                    .as(alias + " required").isTrue();
        }
        assertThat(parameterField(fragment, "audience").isVisible()).isFalse();
        assertThat(viewValidation.validateUiComponents(fragment.getContent()).getAll())
                .extracting(ValidationErrors.Item::getComponent)
                .containsExactlyInAnyOrder(parameterField(fragment, "client"),
                        parameterField(fragment, "fromDate"), parameterField(fragment, "toDate"));

        viewTestSupport.click(dialog, "printReportButton");

        verifyNoInteractions(reportDownloader);
        assertThat(openedDialogWindows().getDialogs()).contains(dialog);

        setClient(fragment, client);
        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        LocalDate toDate = LocalDate.of(2025, 2, 1);
        setDate(fragment, "fromDate", fromDate);
        setDate(fragment, "toDate", toDate);
        assertThat(viewValidation.validateUiComponents(fragment.getContent()).getAll()).isEmpty();

        viewTestSupport.click(dialog, "printReportButton");

        assertThat(downloadedHtml()).contains("Client 360° Report", client.getName(),
                datatypeFormatter.formatLocalDate(fromDate) + " - " + datatypeFormatter.formatLocalDate(toDate));
    }

    @Test
    void downloadingClientProfileUsesPresetParametersWithoutPrompting() {
        Client client = entities.client("Profile download client");
        ClientDetailView view = viewTestSupport.navigateToDetailView(client, ClientDetailView.class);
        LocalDate today = LocalDate.now();

        viewTestSupport.click(view, "downloadProfileButton");

        assertThat(openedDialogWindows().getDialogs()).noneMatch(InputParametersDialog.class::isInstance);
        assertThat(downloadedHtml()).contains("Client 360° Report", client.getName(),
                datatypeFormatter.formatLocalDate(today.minusYears(1)) + " - "
                        + datatypeFormatter.formatLocalDate(today));
    }

    private OpenedDialogWindows openedDialogWindows() {
        return applicationContext.getBean(OpenedDialogWindows.class);
    }

    private Component parameterField(InputParametersFragment fragment, String alias) {
        return UiComponentUtils.findComponent(fragment.getContent(), "param_" + alias).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private void setClient(InputParametersFragment fragment, Client client) {
        ((HasValue<?, Client>) parameterField(fragment, "client")).setValue(client);
    }

    private void setDate(InputParametersFragment fragment, String alias, LocalDate date) {
        TypedDatePicker<?> field = (TypedDatePicker<?>) parameterField(fragment, alias);
        field.setValue(date);
    }

    private String downloadedHtml() {
        ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
        verify(reportDownloader).download(content.capture(), eq("client-360-report.html"), eq(DownloadFormat.HTML));
        return new String(content.getValue(), StandardCharsets.UTF_8);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DownloadConfiguration {

        @Bean
        @Primary
        ReportDownloader client360ReportTestDownloader() {
            return mock(ReportDownloader.class);
        }
    }
}
