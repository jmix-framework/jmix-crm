package com.company.crm.ai.report.run;

import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.FluentLoader;
import io.jmix.core.Messages;
import io.jmix.reports.ReportRepository;
import io.jmix.reports.entity.Report;
import io.jmix.reports.runner.ReportRunner;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
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
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiReportExecutionServicePersistenceUnitTest {

    @SuppressWarnings("unchecked")
    @Test
    void testPersistReportResult_cleansFileWhenAttachmentSaveFails() throws Exception {
        // given
        ReportRepository reportRepository = mock(ReportRepository.class);
        ReportRunner reportRunner = mock(ReportRunner.class);
        AiReportParameterConverter parameterConverter = mock(AiReportParameterConverter.class);
        ReportContentConverter contentConverter = mock(ReportContentConverter.class);
        FileStorage fileStorage = mock(FileStorage.class);
        DataManager dataManager = mock(DataManager.class);
        Messages messages = mock(Messages.class);

        AiReportExecutionService service = new AiReportExecutionService(
                reportRepository,
                reportRunner,
                parameterConverter,
                contentConverter,
                fileStorage,
                dataManager,
                messages
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

        ReportExecutionResult originalResult = ReportExecutionResult.success(
                "client-360-report",
                null,
                "HTML",
                "report body"
        );

        // when
        ReportExecutionResult returnedResult = invokePersistReportResult(
                service,
                originalResult,
                conversationId,
                "Client 360 Report"
        );

        // then
        assertThat(returnedResult).isEqualTo(originalResult);
        verify(fileStorage).removeFile(fileRef);
        verify(dataManager).save(any(AiConversationAttachment.class));
        verify(dataManager, never()).save(any(ChatMessage.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPersistReportResult_returnsOriginalWhenFileStorageFails() throws Exception {
        // given
        ReportRepository reportRepository = mock(ReportRepository.class);
        ReportRunner reportRunner = mock(ReportRunner.class);
        AiReportParameterConverter parameterConverter = mock(AiReportParameterConverter.class);
        ReportContentConverter contentConverter = mock(ReportContentConverter.class);
        FileStorage fileStorage = mock(FileStorage.class);
        DataManager dataManager = mock(DataManager.class);
        Messages messages = mock(Messages.class);

        AiReportExecutionService service = new AiReportExecutionService(
                reportRepository,
                reportRunner,
                parameterConverter,
                contentConverter,
                fileStorage,
                dataManager,
                messages
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

        ReportExecutionResult originalResult = ReportExecutionResult.success(
                "client-360-report",
                null,
                "HTML",
                "report body"
        );

        // when
        ReportExecutionResult returnedResult = invokePersistReportResult(
                service,
                originalResult,
                conversationId,
                "Client 360 Report"
        );

        // then
        assertThat(returnedResult).isEqualTo(originalResult);
        verify(dataManager, never()).save(any(AiConversationAttachment.class));
        verify(fileStorage, never()).removeFile(any(FileRef.class));
    }

    @Test
    void testExecuteReport_whenRunnerThrows_mapsToExecutionError() {
        // given
        ReportRepository reportRepository = mock(ReportRepository.class);
        ReportRunner reportRunner = mock(ReportRunner.class);
        AiReportParameterConverter parameterConverter = mock(AiReportParameterConverter.class);
        ReportContentConverter contentConverter = mock(ReportContentConverter.class);
        FileStorage fileStorage = mock(FileStorage.class);
        DataManager dataManager = mock(DataManager.class);
        Messages messages = mock(Messages.class);

        AiReportExecutionService service = new AiReportExecutionService(
                reportRepository,
                reportRunner,
                parameterConverter,
                contentConverter,
                fileStorage,
                dataManager,
                messages
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

        // when
        ReportExecutionResult result = service.executeReport(
                "client-360-report",
                Map.of(),
                null,
                null,
                List.of("client-360-report")
        );

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.EXECUTION_ERROR);
        assertThat(result.errorMessage()).contains("runner exploded");
    }

    @Test
    void testSaveAttachmentEventMessage_usesAssistantNameInContent() throws Exception {
        // given
        ReportRepository reportRepository = mock(ReportRepository.class);
        ReportRunner reportRunner = mock(ReportRunner.class);
        AiReportParameterConverter parameterConverter = mock(AiReportParameterConverter.class);
        ReportContentConverter contentConverter = mock(ReportContentConverter.class);
        FileStorage fileStorage = mock(FileStorage.class);
        DataManager dataManager = mock(DataManager.class);
        Messages messages = mock(Messages.class);

        AiReportExecutionService service = new AiReportExecutionService(
                reportRepository,
                reportRunner,
                parameterConverter,
                contentConverter,
                fileStorage,
                dataManager,
                messages
        );

        when(messages.getMessage("com.company.crm.ai.view.aiconversation/assistantName"))
                .thenReturn("CRM AI");

        AiConversation conversation = new AiConversation();
        conversation.setId(UUID.randomUUID());
        ChatMessage chatMessage = new ChatMessage();
        when(dataManager.create(ChatMessage.class)).thenReturn(chatMessage);

        Method saveAttachmentEventMessage = AiReportExecutionService.class
                .getDeclaredMethod("saveAttachmentEventMessage", AiConversation.class, String.class);
        saveAttachmentEventMessage.setAccessible(true);

        // when
        saveAttachmentEventMessage.invoke(service, conversation, "Client 360 Report");

        // then
        assertThat(chatMessage.getType()).isEqualTo(com.company.crm.ai.model.ChatMessageType.ATTACHMENT);
        assertThat(chatMessage.getContent()).isEqualTo("CRM AI added attachment \"Client 360 Report\"");
        verify(dataManager).save(eq(chatMessage));
    }

    private ReportExecutionResult invokePersistReportResult(AiReportExecutionService service,
                                                            ReportExecutionResult originalResult,
                                                            UUID conversationId,
                                                            String reportTitle) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method persistReportResult = AiReportExecutionService.class
                .getDeclaredMethod("persistReportResult", ReportExecutionResult.class, UUID.class, String.class);
        persistReportResult.setAccessible(true);
        return (ReportExecutionResult) persistReportResult.invoke(
                service,
                originalResult,
                conversationId,
                reportTitle
        );
    }
}
