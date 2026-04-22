package com.socialpulse.app.report.domain.model;

import java.time.LocalDateTime;

import com.socialpulse.app.report.domain.enums.ReportStatus;
import com.socialpulse.app.report.domain.enums.ReportTargetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {
	private Long id;
	private Long reporterId;
	private ReportTargetType targetType;
	private Long targetId;
	private String reason;
	private ReportStatus status;
	private LocalDateTime createdAt;

	public void markPending() {
		this.status = ReportStatus.PENDING;
	}

	public void markResolved() {
		this.status = ReportStatus.RESOLVED;
	}

    public void markRejected() {
        this.status = ReportStatus.REJECTED;
    }
}
