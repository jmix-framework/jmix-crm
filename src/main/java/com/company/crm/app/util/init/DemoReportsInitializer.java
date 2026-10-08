package com.company.crm.app.util.init;

import com.company.crm.app.config.SpringProfiles;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.reports.ReportImportExport;
import io.jmix.reports.entity.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

/**
 * Imports the two AI JPQL demo reports from {@code demo/reports/ai-jpql-reports.zip} (a resource root in
 * build.gradle) when the stand database lacks either of them, so a fresh or reset stand needs no manual import.
 */
@Component
public class DemoReportsInitializer {

    private static final Logger log = LoggerFactory.getLogger(DemoReportsInitializer.class);

    static final String ARCHIVE = "ai-jpql-reports.zip";
    // The ids the reports have in the archive; an import keeps them.
    public static final List<UUID> REPORT_IDS = List.of(
            UUID.fromString("5a1e7b2c-0c6f-4b7e-9a51-3f2d1c0a5001"),
            UUID.fromString("5a1e7b2c-0c6f-4b7e-9a51-3f2d1c0a5002"));

    private final SpringProfiles springProfiles;
    private final SystemAuthenticator systemAuthenticator;
    private final UnconstrainedDataManager dataManager;
    private final ReportImportExport reportImportExport;

    public DemoReportsInitializer(SpringProfiles springProfiles,
                                  SystemAuthenticator systemAuthenticator,
                                  UnconstrainedDataManager dataManager,
                                  ReportImportExport reportImportExport) {
        this.springProfiles = springProfiles;
        this.systemAuthenticator = systemAuthenticator;
        this.dataManager = dataManager;
        this.reportImportExport = reportImportExport;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!springProfiles.isLocalProfile()) {
            return;
        }
        try {
            importIfAbsent();
        } catch (RuntimeException e) {
            // The stand stays up without the reports; they can still be imported in Reports.
            log.error("AI JPQL demo reports were not imported from {}", ARCHIVE, e);
        }
    }

    /**
     * Imports the archive when either report is missing; a repeated import restores both to the archive version.
     */
    public void importIfAbsent() {
        systemAuthenticator.runWithSystem(() -> {
            long present = dataManager.loadValue("select count(e) from report_Report e where e.id in :ids", Long.class)
                    .parameter("ids", REPORT_IDS)
                    .one();
            if (present == REPORT_IDS.size()) {
                log.info("AI JPQL demo reports are present, import skipped");
                return;
            }
            List<String> names = reportImportExport.importReports(readArchive()).stream()
                    .map(Report::getName)
                    .toList();
            log.info("AI JPQL demo reports imported from {}: {}", ARCHIVE, names);
        });
    }

    private byte[] readArchive() {
        try {
            return new ClassPathResource(ARCHIVE).getContentAsByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + ARCHIVE + " from the classpath", e);
        }
    }
}
