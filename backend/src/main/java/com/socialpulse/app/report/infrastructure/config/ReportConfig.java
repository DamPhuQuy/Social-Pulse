package com.socialpulse.app.report.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.report.adapter.persistence.ReportRepositoryAdapter;
import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.service.CreateReportService;
import com.socialpulse.app.report.application.usecase.CreateReportUseCase;
import com.socialpulse.app.report.domain.repository.ReportRepository;
import com.socialpulse.app.report.infrastructure.persistence.mapper.ReportPersistenceMapper;
import com.socialpulse.app.report.infrastructure.persistence.repository.JpaReportRepository;

@Configuration
public class ReportConfig {

	@Bean
	public ReportRepository reportRepositoryPort(JpaReportRepository jpaReportRepository,
									 ReportPersistenceMapper reportPersistenceMapper) {
		return new ReportRepositoryAdapter(jpaReportRepository, reportPersistenceMapper);
	}

	@Bean
	public CreateReportUseCase createReportUseCase(ReportRepository reportRepositoryPort,
											   ReportMapper reportMapper) {
		return new CreateReportService(reportRepositoryPort, reportMapper);
	}

}


