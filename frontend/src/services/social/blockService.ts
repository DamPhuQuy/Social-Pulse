import { apiClient } from "@/lib/axiosClient";

const BLOCKS_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1").trim();

export async function blockUser(userId: number): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.post(`${BLOCKS_BASE_URL}/blocks/${userId}`);
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Không thể chặn người dùng này." };
  }
}

export async function unblockUser(userId: number): Promise<{ ok: boolean; message?: string }> {
  try {
    await apiClient.delete(`${BLOCKS_BASE_URL}/blocks/${userId}`);
    return { ok: true };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Không thể hủy chặn người dùng này." };
  }
}

export async function getBlockedUserIds(): Promise<{ ok: boolean; data?: number[]; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: number[] }>(`${BLOCKS_BASE_URL}/blocks/ids`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Không thể tải danh sách chặn." };
  }
}

export async function checkIsBlocked(userId: number): Promise<{ ok: boolean; data?: boolean; message?: string }> {
  try {
    const res = await apiClient.get<{ code: number; message: string; data: boolean }>(`${BLOCKS_BASE_URL}/blocks/${userId}/is-blocked`);
    return { ok: true, data: res.data.data };
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return { ok: false, message: axiosErr?.response?.data?.message ?? "Không thể kiểm tra trạng thái chặn." };
  }
}
