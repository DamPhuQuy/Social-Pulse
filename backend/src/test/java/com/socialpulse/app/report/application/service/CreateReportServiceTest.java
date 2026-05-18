package com.socialpulse.app.report.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.dto.request.CreateReportRequest;
import com.socialpulse.app.report.domain.enums.ReportTargetType;
import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.report.domain.repository.ReportRepository;

@ExtendWith(MockitoExtension.class)
class CreateReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private ReportTargetValidator reportTargetValidator;
    @Mock private ReportMapper reportMapper;

    private CreateReportService service;

    @BeforeEach
    void setUp() {
        service = new CreateReportService(reportRepository, reportTargetValidator, reportMapper);
    }

    @Test
    void createReport_success() {
        CreateReportRequest request = CreateReportRequest.builder()
                .targetType(ReportTargetType.POST).targetId(1L).reason("spam").build();
        Report report = Report.builder().id(1L).reporterId(5L).build();
        when(reportMapper.toReport(request, 5L)).thenReturn(report);
        when(reportRepository.save(any())).thenReturn(report);

        Report result = service.createReport(5L, request);

        assertNotNull(result);
        verify(reportTargetValidator).validate(ReportTargetType.POST, 1L);
        verify(reportRepository).save(report);
    }
}
