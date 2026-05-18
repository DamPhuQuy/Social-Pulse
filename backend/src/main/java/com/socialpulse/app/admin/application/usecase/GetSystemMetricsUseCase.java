package com.socialpulse.app.admin.application.usecase;

import com.socialpulse.app.admin.application.dto.SystemMetricsResponse;

public interface GetSystemMetricsUseCase {
    SystemMetricsResponse getMetrics(String period);
    byte[] exportCsv(String period);
}
