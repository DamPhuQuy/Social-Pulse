import { apiClient } from "@/lib/axiosClient";
import type { PageResponse } from "@/services/user/userService";

export type MetricsPeriod = "LAST_7_DAYS" | "LAST_30_DAYS" | "LAST_90_DAYS" | "ALL_TIME";

export interface SystemMetricsResponse {
  generatedAt: string;
  period: MetricsPeriod;
  totalUsers: number;
  newUsers: number;
  usersByStatus: Record<string, number>;
  totalPosts: number;
  newPosts: number;
  toxicPosts: number;
  deletedPosts: number;
}

export interface AdminUserResponse {
  id: number;
  username: string;
  email: string;
  displayName: string | null;
  avatarUrl: string | null;
  status: string;
  verification: string;
  locked: boolean;
  failedLoginAttempts: number;
  roles: string[];
  lastLoginAt: string | null;
  createdAt: string;
}

export interface RbacRoleResponse {
  name: string;
  description: string | null;
  permissions: string[];
}

export interface AiStatusResponse {
  enabled: boolean;
  baseUrl: string;
  featureSchemaVersion: string;
  healthReachable: boolean;
  trainingControlsAvailable: boolean;
}

export async function getAdminMetrics(period: MetricsPeriod): Promise<{ ok: boolean; data?: SystemMetricsResponse; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: SystemMetricsResponse }>(`/admin/metrics?period=${period}`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch admin metrics." };
  }
}

export async function getAdminUsers(query = "", page = 0, size = 20): Promise<{ ok: boolean; data?: PageResponse<AdminUserResponse>; message?: string }> {
  try {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (query.trim()) params.set("query", query.trim());
    const res = await apiClient.get<{ code: number; message: string; data: PageResponse<AdminUserResponse> }>(`/admin/users?${params.toString()}`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch admin users." };
  }
}

export async function changeAdminUserRoles(userId: number, roles: string[]): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.patch(`/admin/users/${userId}/role`, { roles });
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to update user roles." };
  }
}

export async function banAdminUser(userId: number, ban: boolean): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.patch(`/admin/users/${userId}/ban`, { ban });
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to update user lock state." };
  }
}

export async function getRbacRoles(): Promise<{ ok: boolean; data?: RbacRoleResponse[]; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: RbacRoleResponse[] }>("/admin/rbac/roles");
    return { ok: true, data: res.data.data ?? [] };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch RBAC roles." };
  }
}

export async function getAiStatus(): Promise<{ ok: boolean; data?: AiStatusResponse; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: AiStatusResponse }>("/admin/ai/status");
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Failed to fetch AI status." };
  }
}
