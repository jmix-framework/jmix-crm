package com.company.crm.test.report;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.user.User;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.SaveContext;
import io.jmix.core.impl.StandardSerialization;
import io.jmix.data.PersistenceHints;
import io.jmix.reports.ReportImportExport;
import io.jmix.reports.ReportsSerialization;
import io.jmix.reports.entity.DataSet;
import io.jmix.reports.entity.DataSetType;
import io.jmix.reports.entity.JmixTableData;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportInputParameter;
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.runner.ReportRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * The archive the AI demo imports in Administration → Reports → Reports: two reports with an
 * "AI-generated JPQL" band. The stored query runs without a model, with the rights of the current user.
 */
class AiJpqlReportsArchiveTest extends AbstractTest {

    private static final Path ARCHIVE = Path.of("demo/reports/ai-jpql-reports.zip");
    private static final String REVENUE_REPORT = "Выручка клиентов (AI JPQL)";
    private static final String LIVE_REPORT = "Выручка клиентов (живая генерация)";

    @Autowired
    private ReportImportExport reportImportExport;
    @Autowired
    private ReportsSerialization reportsSerialization;
    @Autowired
    private ReportRunner reportRunner;
    @Autowired
    private StandardSerialization standardSerialization;
    @Autowired
    private UnconstrainedDataManager unconstrainedDataManager;

    private final List<Report> imported = new ArrayList<>();

    @Test
    void archiveHoldsBothReportsWithAnAiJpqlBand() throws IOException {
        importArchive();

        assertThat(imported).extracting(Report::getName).containsExactlyInAnyOrder(REVENUE_REPORT, LIVE_REPORT);
        Report revenue = structure(REVENUE_REPORT);
        assertThat(clientsDataSet(revenue).getType()).isEqualTo(DataSetType.LLM);
        assertThat(clientsDataSet(revenue).getLlmGeneratedQuery()).contains(":fromDate", ":toDate");
        assertThat(revenue.getInputParameters())
                .extracting(ReportInputParameter::getAlias, ReportInputParameter::getType)
                .containsExactly(tuple("fromDate", ParameterType.DATE),
                        tuple("toDate", ParameterType.DATE));
        assertThat(revenue.getDefaultTemplate().getReportOutputType()).isEqualTo(ReportOutputType.TABLE);

        Report live = structure(LIVE_REPORT);
        assertThat(clientsDataSet(live).getType()).isEqualTo(DataSetType.LLM);
        assertThat(clientsDataSet(live).getLlmGeneratedQuery()).isNull();
    }

    @Test
    void storedQueryRunsWithTheRightsOfTheCurrentUser() throws IOException {
        importArchive();
        User alice = testUsers.ensureUser("alice");
        User admin = testUsers.ensureUser("admin");
        Client aliceClient = client("Alice Client", alice);
        Client adminClient = client("Admin Client", admin);
        entities.order(aliceClient, LocalDate.of(2025, 3, 1), OrderStatus.NEW, new BigDecimal("100"));
        entities.order(adminClient, LocalDate.of(2025, 4, 1), OrderStatus.NEW, new BigDecimal("200"));
        entities.order(aliceClient, LocalDate.of(2023, 1, 1), OrderStatus.NEW, new BigDecimal("5000"));

        assertThat(withUser(admin, this::runRevenueReport)).containsExactly("Admin Client", "Alice Client");
        assertThat(withUser(alice, this::runRevenueReport)).containsExactly("Alice Client");
    }

    @AfterEach
    void removeImportedReports() {
        systemAuthenticator.runWithSystem(() -> imported.forEach(report -> {
            Report loaded = unconstrainedDataManager.load(Report.class).id(report.getId()).one();
            loaded.setDefaultTemplate(null);
            unconstrainedDataManager.save(loaded);
            SaveContext templates = new SaveContext().setHint(PersistenceHints.SOFT_DELETION, false);
            unconstrainedDataManager.load(Report.class).id(report.getId())
                    .fetchPlan(fp -> fp.addFetchPlan("_base").add("templates", "_base"))
                    .one().getTemplates().forEach(templates::removing);
            unconstrainedDataManager.save(templates);
            unconstrainedDataManager.save(new SaveContext().setHint(PersistenceHints.SOFT_DELETION, false)
                    .removing(unconstrainedDataManager.load(Report.class).id(report.getId()).one()));
        }));
        imported.clear();
    }

    private void importArchive() throws IOException {
        byte[] archive = Files.readAllBytes(ARCHIVE);
        imported.addAll(systemAuthenticator.withSystem(() -> reportImportExport.importReports(archive)));
    }

    private Report structure(String name) {
        Report report = imported.stream().filter(r -> name.equals(r.getName())).findFirst().orElseThrow();
        return reportsSerialization.convertToReport(report.getXml());
    }

    private DataSet clientsDataSet(Report report) {
        return report.getBands().stream()
                .filter(band -> "clients".equals(band.getName()))
                .findFirst().orElseThrow()
                .getDataSets().getFirst();
    }

    private Client client(String name, User accountManager) {
        Client client = entities.client(name);
        client.setAccountManager(accountManager);
        return saveWithoutReload(client);
    }

    private List<String> runRevenueReport() {
        byte[] content = reportRunner.byReportCode("customer-revenue-ai-jpql")
                .withParams(Map.of("fromDate", LocalDate.of(2024, 1, 1), "toDate", LocalDate.of(2026, 12, 31)))
                .run()
                .getContent();
        JmixTableData table = (JmixTableData) standardSerialization.deserialize(content);
        Collection<KeyValueEntity> rows = table.getData().get("clients");
        return rows.stream().map(row -> (String) row.getValue("clientName")).toList();
    }
}
