package com.company.crm.ai.jmix.report.run;

import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.AiConversationAttachment;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FluentLoader;
import io.jmix.reports.ReportRepository;
import io.jmix.reports.entity.Report;
import io.jmix.reports.runner.ReportRunner;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiReportExecutionServicePersistenceUnitTest {

    @SuppressWarnings("unchecked")
    @Test
    void testPersistReportResult_cleansFileWhenAttachmentSaveFails() throws Exception {
        ReportRepository reportRepository = mock(ReportRepository.class);
        ReportRunner reportRunner = mock(ReportRunner.class);
        AiReportParameterConverter parameterConverter = mock(AiReportParameterConverter.class);
        ReportContentConverter contentConverter = mock(ReportContentConverter.class);
        FileStorage fileStorage = mock(FileStorage.class);
        DataManager dataManager = mock(DataManager.class);

        AiReportExecutionService service = new AiReportExecutionService(
                reportRepository,
                reportRunner,
                parameterConverter,
                contentConverter,
                fileStorage,
                dataManager
        );

        UUID conversationId = UUID.randomUUID();
        AiConversation conversation = new AiConversation();
        conversation.setId(conversationId);

        FluentLoader<AiConversation> loader = mock(FluentLoader.class);
        FluentLoader.ById<AiConversation> byId = mock(FluentLoader.ById.class);
        when(dataManager.load(AiConversation.class)).thenReturn(loader);
        when(loader.id(conversationId)).thenReturn(byId);
        when(byId.optional()).thenReturn(Optional.of(conversation));

        FileRef fileRef = new FileRef("storage", "2026/02/22/report.html", "report.html");
        when(fileStorage.saveStream(anyString(), any(InputStream.class))).thenReturn(fileRef);

        when(dataManager.create(AiConversationAttachment.class)).thenReturn(new AiConversationAttachment());
        doThrow(new RuntimeException("DB save failed"))
                .when(dataManager)
                .save(any(AiConversationAttachment.class));

        Method persistReportResult = AiReportExecutionService.class
                .getDeclaredMethod("persistReportResult", ReportExecutionResult.class, UUID.class, String.class);
        persistReportResult.setAccessible(true);

        ReportExecutionResult originalResult = ReportExecutionResult.success(
                "client-360-report",
                null,
                "HTML",
                "report body"
        );

        ReportExecutionResult returnedResult = (ReportExecutionResult) persistReportResult.invoke(
                service,
                originalResult,
                conversationId,
                "Client 360 Report"
        );

        assertThat(returnedResult).isEqualTo(originalResult);
        verify(fileStorage).removeFile(fileRef);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPersistReportResult_returnsOriginalWhenFileStorageFails() throws Exception {
        ReportRepository reportRepository = mock(ReportRepository.class);
        ReportRunner reportRunner = mock(ReportRunner.class);
        AiReportParameterConverter parameterConverter = mock(AiReportParameterConverter.class);
        ReportContentConverter contentConverter = mock(ReportContentConverter.class);
        FileStorage fileStorage = mock(FileStorage.class);
        DataManager dataManager = mock(DataManager.class);

        AiReportExecutionService service = new AiReportExecutionService(
                reportRepository,
                reportRunner,
                parameterConverter,
                contentConverter,
                fileStorage,
                dataManager
        );

        UUID conversationId = UUID.randomUUID();
        AiConversation conversation = new AiConversation();
        conversation.setId(conversationId);

        FluentLoader<AiConversation> loader = mock(FluentLoader.class);
        FluentLoader.ById<AiConversation> byId = mock(FluentLoader.ById.class);
        when(dataManager.load(AiConversation.class)).thenReturn(loader);
        when(loader.id(conversationId)).thenReturn(byId);
        when(byId.optional()).thenReturn(Optional.of(conversation));

        doThrow(new RuntimeException("File write failed"))
                .when(fileStorage)
                .saveStream(anyString(), any(InputStream.class));

        Method persistReportResult = AiReportExecutionService.class
                .getDeclaredMethod("persistReportResult", ReportExecutionResult.class, UUID.class, String.class);
        persistReportResult.setAccessible(true);

        ReportExecutionResult originalResult = ReportExecutionResult.success(
                "client-360-report",
                null,
                "HTML",
                "report body"
        );

        ReportExecutionResult returnedResult = (ReportExecutionResult) persistReportResult.invoke(
                service,
                originalResult,
                conversationId,
                "Client 360 Report"
        );

        assertThat(returnedResult).isEqualTo(originalResult);
        verify(dataManager, never()).save(any(AiConversationAttachment.class));
        verify(fileStorage, never()).removeFile(any(FileRef.class));
    }

    @Test
    void testExecuteReport_whenRunnerThrows_mapsToExecutionError() {
        ReportRepository reportRepository = mock(ReportRepository.class);
        ReportRunner reportRunner = mock(ReportRunner.class);
        AiReportParameterConverter parameterConverter = mock(AiReportParameterConverter.class);
        ReportContentConverter contentConverter = mock(ReportContentConverter.class);
        FileStorage fileStorage = mock(FileStorage.class);
        DataManager dataManager = mock(DataManager.class);

        AiReportExecutionService service = new AiReportExecutionService(
                reportRepository,
                reportRunner,
                parameterConverter,
                contentConverter,
                fileStorage,
                dataManager
        );

        Report report = mock(Report.class);
        when(report.getCode()).thenReturn("client-360-report");
        when(report.getInputParameters()).thenReturn(List.of());
        when(report.getDefaultTemplate()).thenReturn(null);
        when(report.getTemplates()).thenReturn(List.of());

        when(reportRepository.getAllReports()).thenReturn(List.of(report));
        when(reportRepository.reloadForRunning(report)).thenReturn(report);
        when(parameterConverter.convertParameters(anyList(), anyMap()))
                .thenReturn(ReportParameterConversionResult.success(Map.of()));
        when(reportRunner.byReportEntity(report))
                .thenThrow(new RuntimeException("runner exploded"));

        ReportExecutionResult result = service.executeReport(
                "client-360-report",
                Map.of(),
                null,
                null,
                List.of("client-360-report")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.EXECUTION_ERROR);
        assertThat(result.errorMessage()).contains("runner exploded");
    }
}
