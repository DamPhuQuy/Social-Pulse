import { apiClient } from "@/lib/axiosClient";
import type { PageResponse } from "@/services/user/userService";

const REPORT_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1").trim().replace(/\/api\/v1\/?$/, "/api");

export type ReportTargetType = "POST" | "COMMENT" | "USER";
export type ReportStatus = "PENDING" | "RESOLVED" | "REJECTED";
export type ReviewAction = "REJECT" | "DELETE_CONTENT" | "BAN_USER" | "DELETE_CONTENT_AND_BAN_USER";

export interface CreateReportRequest {
  targetType: ReportTargetType;
  targetId: number;
  reason: string;
}

export interface ReportResponse {
  id: number;
  reporterId: number;
  targetType: ReportTargetType;
  targetId: number;
  reason: string;
  status: ReportStatus;
  createdAt: string;
  targetContent?: string | null;
  targetOwnerId?: number | null;
  targetOwnerUsername?: string | null;
}

export async function createReport(payload: CreateReportRequest): Promise<{ ok: boolean; data?: ReportResponse; message?: string }> {
  try {
    const res = await apiClient.request<{ code: number; message: string; data: ReportResponse }>({
      method: "post",
      url: `${REPORT_BASE_URL}/reports`,
      data: payload,
    });
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to submit report." };
  }
}

export async function getReports(status?: ReportStatus, page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<ReportResponse>; message?: string }> {
  try {
    const query = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) query.set("status", status);
    const res = await apiClient.request<{ code: number; message: string; data: PageResponse<ReportResponse> }>({
      method: "get",
      url: `${REPORT_BASE_URL}/reports?${query.toString()}`,
    });
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch reports." };
  }
}

export async function getReportDetail(reportId: number): Promise<{ ok: boolean; data?: ReportResponse; message?: string }> {
  try {
    const res = await apiClient.request<{ code: number; message: string; data: ReportResponse }>({
      method: "get",
      url: `${REPORT_BASE_URL}/reports/${reportId}`,
    });
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch report detail." };
  }
}

export async function updateReportStatus(reportId: number, status: ReportStatus): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.request<{ code: number; message: string }>({
      method: "patch",
      url: `${REPORT_BASE_URL}/reports/${reportId}/status`,
      data: { status }
    });
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to update report status." };
  }
}

export async function reviewReport(
  reportId: number,
  action: ReviewAction,
  note?: string,
): Promise<{ ok: boolean; data?: ReportResponse; message?: string }> {
  try {
    const res = await apiClient.request<{ code: number; message: string; data: ReportResponse }>({
      method: "post",
      url: `${REPORT_BASE_URL}/reports/${reportId}/review`,
      data: { action, note },
    });
    return { ok: true, data: res.data.data, message: res.data.message };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to review report." };
  }
}
