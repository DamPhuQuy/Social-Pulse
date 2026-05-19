package com.socialpulse.app.report.application.dto.request;

import com.socialpulse.app.report.domain.enums.ReviewAction;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReportRequest {

    @NotNull(message = "Action is required")
    private ReviewAction action;

    private String note;
}
