import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import AppHeader from "@/components/social/AppHeader";
import AppSidebar from "@/components/social/AppSidebar";
import {
  ShieldAlert,
  Users,
  Search,
  UserCheck,
  Shield,
  Award,
  HelpCircle,
  Loader2,
} from "lucide-react";
import {
  changeAdminUserRoles,
  banAdminUser,
  getAdminUsers,
  getRbacRoles,
  type AdminUserResponse,
  type RbacRoleResponse,
} from "@/services/admin/adminService";

const ROLE_ORDER = ["ADMIN", "USER", "GUEST"] as const;

export default function RbacManagement() {
  const [searchQuery, setSearchQuery] = useState("");
  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [roles, setRoles] = useState<RbacRoleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [savingUserId, setSavingUserId] = useState<number | null>(null);
  const [togglingBanUserId, setTogglingBanUserId] = useState<number | null>(null);

  const roleOptions = useMemo(() => {
    const known = new Map(roles.map((role) => [role.name, role]));
    const sortedKnown = [...roles].sort((a, b) => {
      const aIdx = ROLE_ORDER.indexOf(a.name as (typeof ROLE_ORDER)[number]);
      const bIdx = ROLE_ORDER.indexOf(b.name as (typeof ROLE_ORDER)[number]);
      const normA = aIdx === -1 ? Number.MAX_SAFE_INTEGER : aIdx;
      const normB = bIdx === -1 ? Number.MAX_SAFE_INTEGER : bIdx;
      return normA - normB || a.name.localeCompare(b.name);
    });
    return sortedKnown.length > 0 ? sortedKnown : [...ROLE_ORDER].map((name) => ({
      name,
      description: null,
      permissions: [],
    })).filter((role) => !known.has(role.name));
  }, [roles]);

  useEffect(() => {
    void loadData(searchQuery);
  }, []);

  const loadData = async (query: string) => {
    setLoading(true);
    const [usersRes, rolesRes] = await Promise.all([
      getAdminUsers(query, 0, 100),
      getRbacRoles(),
    ]);
    setLoading(false);

    if (usersRes.ok && usersRes.data) {
      setUsers(usersRes.data.items ?? []);
    } else {
      toast.error(usersRes.message ?? "Không thể tải danh sách người dùng.");
    }

    if (rolesRes.ok && rolesRes.data) {
      setRoles(rolesRes.data);
    } else {
      toast.error(rolesRes.message ?? "Không thể tải ma trận quyền.");
    }
  };

  const handleSearch = async (value: string) => {
    setSearchQuery(value);
    await loadData(value);
  };

  const handleRoleChange = async (userId: number, newRole: string) => {
    setSavingUserId(userId);
    const res = await changeAdminUserRoles(userId, [newRole]);
    setSavingUserId(null);
    if (!res.ok) {
      toast.error(res.message ?? "Không thể cập nhật vai trò.");
      return;
    }

    setUsers((prev) =>
      prev.map((user) =>
        user.id === userId
          ? { ...user, roles: [newRole] }
          : user
      ),
    );
    toast.success(`Đã cập nhật vai trò thành ${newRole}.`);
  };

  const handleToggleBan = async (user: AdminUserResponse) => {
    setTogglingBanUserId(user.id);
    const nextBanState = !user.locked;
    const res = await banAdminUser(user.id, nextBanState);
    setTogglingBanUserId(null);
    if (!res.ok) {
      toast.error(res.message ?? "Không thể cập nhật trạng thái khóa tài khoản.");
      return;
    }

    setUsers((prev) =>
      prev.map((item) =>
        item.id === user.id
          ? { ...item, locked: nextBanState }
          : item,
      ),
    );
    toast.success(nextBanState ? "Đã khóa tài khoản." : "Đã mở khóa tài khoản.");
  };

  const permissionsByRole = new Map(roles.map((role) => [role.name, role.permissions]));

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />

      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-8 pt-24 px-6 lg:px-10">
        <AppSidebar active="admin-rbac" />

        <div className="min-w-0 space-y-6 pb-10">
          <div className="flex items-center space-x-3 bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm">
            <div className="p-3 bg-indigo-500/10 text-indigo-500 rounded-xl">
              <ShieldAlert className="w-8 h-8" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight">Quản lý Quyền và Vai trò</h1>
              <p className="text-slate-500 dark:text-slate-400 text-sm mt-0.5">
                Dữ liệu đang lấy trực tiếp từ backend: danh sách tài khoản quản trị và ma trận quyền theo role.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-[1fr_300px] gap-4">
            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 dark:text-neutral-500" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => void handleSearch(e.target.value)}
                placeholder="Tìm kiếm người dùng theo tên, username hoặc email..."
                className="w-full bg-white dark:bg-[#1e1e1e] border border-slate-200/80 dark:border-[#2a2a2a] focus:border-indigo-500 rounded-2xl py-3 pl-12 pr-4 text-sm outline-none transition-all shadow-sm"
              />
            </div>

            <div className="bg-white dark:bg-[#1e1e1e] px-5 py-3 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Users className="w-5 h-5 text-indigo-500" />
                <span className="text-sm font-semibold">Tổng số tài khoản hiển thị</span>
              </div>
              <span className="text-xl font-bold text-indigo-500">{users.length}</span>
            </div>
          </div>

          <div className="bg-white dark:bg-[#1e1e1e] rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm overflow-hidden">
            {loading ? (
              <div className="p-10 flex items-center justify-center text-slate-500 dark:text-neutral-400">
                <Loader2 className="w-6 h-6 animate-spin mr-2" />
                Đang tải dữ liệu RBAC...
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-slate-200/80 dark:border-[#2a2a2a] text-slate-500 dark:text-neutral-400 text-xs font-semibold uppercase bg-slate-50/50 dark:bg-neutral-800/20">
                      <th className="px-6 py-4">Thành viên</th>
                      <th className="px-6 py-4">Vai trò hiện tại</th>
                      <th className="px-6 py-4">Trạng thái</th>
                      <th className="px-6 py-4">Quyền hiệu lực</th>
                      <th className="px-6 py-4 text-right">Điều hành</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200/80 dark:divide-[#2a2a2a] text-sm">
                    {users.map((user) => {
                      const primaryRole = user.roles[0] ?? "USER";
                      const permissions = permissionsByRole.get(primaryRole) ?? [];
                      return (
                        <tr key={user.id} className="hover:bg-slate-50/50 dark:hover:bg-neutral-800/30 transition-colors">
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-3">
                              <div className="w-10 h-10 rounded-full bg-indigo-500/10 text-indigo-500 flex items-center justify-center font-bold text-sm">
                                {(user.displayName || user.username).charAt(0).toUpperCase()}
                              </div>
                              <div>
                                <h4 className="font-bold text-slate-800 dark:text-slate-200">{user.displayName || user.username}</h4>
                                <p className="text-xs text-slate-500 dark:text-slate-400">@{user.username} · {user.email}</p>
                              </div>
                            </div>
                          </td>

                          <td className="px-6 py-4">
                            <RoleBadge role={primaryRole} />
                          </td>

                          <td className="px-6 py-4">
                            <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold ${
                              user.locked ? "bg-red-500/10 text-red-500" : "bg-green-500/10 text-green-500"
                            }`}>
                              {user.locked ? "LOCKED" : "ACTIVE"}
                            </span>
                          </td>

                          <td className="px-6 py-4">
                            <div className="flex flex-wrap gap-1 max-w-sm">
                              {permissions.length > 0 ? permissions.map((permission) => (
                                <span key={permission} className="px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-650 dark:text-slate-400 rounded-md text-[10px] font-semibold font-mono">
                                  {permission}
                                </span>
                              )) : (
                                <span className="text-xs text-slate-400 dark:text-neutral-500">Không có permission được gắn.</span>
                              )}
                            </div>
                          </td>

                          <td className="px-6 py-4">
                            <div className="flex flex-wrap items-center justify-end gap-2">
                              <select
                                value={primaryRole}
                                onChange={(e) => void handleRoleChange(user.id, e.target.value)}
                                disabled={savingUserId === user.id}
                                className="bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 rounded-xl px-3 py-1.5 text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-60"
                              >
                                {roleOptions.map((role) => (
                                  <option key={role.name} value={role.name}>
                                    {role.name}
                                  </option>
                                ))}
                              </select>
                              <button
                                onClick={() => void handleToggleBan(user)}
                                disabled={togglingBanUserId === user.id}
                                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                                  user.locked
                                    ? "bg-green-500/10 text-green-600 hover:bg-green-500/20"
                                    : "bg-red-500/10 text-red-600 hover:bg-red-500/20"
                                } disabled:opacity-60`}
                              >
                                {user.locked ? "Mở khóa" : "Khóa"}
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function RoleBadge({ role }: { role: string }) {
  const roleClass =
    role === "ADMIN"
      ? "bg-red-500/10 text-red-500"
      : role === "MODERATOR"
        ? "bg-orange-500/10 text-orange-500"
        : role === "USER"
          ? "bg-blue-500/10 text-blue-500"
          : "bg-slate-500/10 text-slate-500";

  const Icon =
    role === "ADMIN"
      ? Shield
      : role === "MODERATOR"
        ? Award
        : role === "USER"
          ? UserCheck
          : HelpCircle;

  return (
    <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold ${roleClass}`}>
      <Icon className="w-3.5 h-3.5" />
      {role}
    </span>
  );
}
