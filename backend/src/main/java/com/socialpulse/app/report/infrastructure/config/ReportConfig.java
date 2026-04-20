package com.socialpulse.app.report.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.report.adapter.out.ReportRepositoryAdapter;
import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.port.in.CreateReportUseCase;
import com.socialpulse.app.report.application.port.out.ReportRepositoryPort;
import com.socialpulse.app.report.application.service.CreateReportService;
import com.socialpulse.app.report.infrastructure.persistence.mapper.ReportDomainToEntity;
import com.socialpulse.app.report.infrastructure.persistence.mapper.ReportEntityToDomain;
import com.socialpulse.app.report.infrastructure.persistence.repository.JpaReportRepository;

@Configuration
public class ReportConfig {

	@Bean
	public ReportRepositoryPort reportRepositoryPort(JpaReportRepository jpaReportRepository,
													 ReportEntityToDomain reportEntityToDomain,
													 ReportDomainToEntity reportDomainToEntity) {
		return new ReportRepositoryAdapter(jpaReportRepository, reportEntityToDomain, reportDomainToEntity);
	}

	@Bean
	public CreateReportUseCase createReportUseCase(ReportRepositoryPort reportRepositoryPort,
											   ReportMapper reportMapper) {
		return new CreateReportService(reportRepositoryPort, reportMapper);
	}

}
