package com.socialpulse.app.report.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.report.adapter.persistence.ReportRepositoryAdapter;
import com.socialpulse.app.report.application.dto.mapper.ReportMapper;
import com.socialpulse.app.report.application.service.CreateReportService;
import com.socialpulse.app.report.application.service.GetReportsService;
import com.socialpulse.app.report.application.service.ReportTargetValidator;
import com.socialpulse.app.report.application.service.UpdateReportStatusService;
import com.socialpulse.app.report.application.usecase.CreateReportUseCase;
import com.socialpulse.app.report.application.usecase.GetReportsUseCase;
import com.socialpulse.app.report.application.usecase.UpdateReportStatusUseCase;
import com.socialpulse.app.report.domain.repository.ReportRepository;
import com.socialpulse.app.report.infrastructure.persistence.mapper.ReportPersistenceMapper;
import com.socialpulse.app.report.infrastructure.persistence.repository.JpaReportRepository;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Configuration
public class ReportConfig {

	@Bean
	public ReportRepository reportRepositoryPort(JpaReportRepository jpaReportRepository,
									 ReportPersistenceMapper reportPersistenceMapper) {
		return new ReportRepositoryAdapter(jpaReportRepository, reportPersistenceMapper);
	}

	@Bean
	public CreateReportUseCase createReportUseCase(ReportRepository reportRepositoryPort,
											   ReportTargetValidator reportTargetValidator,
											   ReportMapper reportMapper) {
		return new CreateReportService(reportRepositoryPort, reportTargetValidator, reportMapper);
	}

	@Bean
	public ReportTargetValidator reportTargetValidator(PostRepository postRepository,
													   CommentRepository commentRepository,
													   UserRepository userRepository) {
		return new ReportTargetValidator(postRepository, commentRepository, userRepository);
	}

	@Bean
	public GetReportsUseCase getReportsUseCase(ReportRepository reportRepositoryPort,
											   ReportMapper reportMapper) {
		return new GetReportsService(reportRepositoryPort, reportMapper);
	}

	@Bean
	public UpdateReportStatusUseCase updateReportStatusUseCase(ReportRepository reportRepositoryPort) {
		return new UpdateReportStatusService(reportRepositoryPort);
	}

}

