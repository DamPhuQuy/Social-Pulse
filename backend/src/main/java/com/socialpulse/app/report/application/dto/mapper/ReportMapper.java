package com.socialpulse.app.report.application.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.report.application.dto.request.CreateReportRequest;
import com.socialpulse.app.report.application.dto.response.ReportResponse;
import com.socialpulse.app.report.domain.model.Report;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    @Mapping(target = "reporterId", source = "reporterId")
    @Mapping(target = "targetType", source = "request.targetType")
    @Mapping(target = "targetId", source = "request.targetId")
    @Mapping(target = "reason", source = "request.reason")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Report toReport(CreateReportRequest request, Long reporterId);

    ReportResponse toResponse(Report report);
}
