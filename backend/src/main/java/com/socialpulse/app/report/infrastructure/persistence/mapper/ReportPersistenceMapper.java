package com.socialpulse.app.report.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;

import com.socialpulse.app.report.domain.model.Report;
import com.socialpulse.app.report.infrastructure.persistence.entity.ReportEntity;

@Mapper(componentModel = "spring")
public interface ReportPersistenceMapper {
    Report toDomain(ReportEntity entity);

    ReportEntity toEntity(Report domain);
}