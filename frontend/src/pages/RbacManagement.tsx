import { useState } from "react";
import { toast } from "sonner";
import AppHeader from "@/components/social/AppHeader";
import AppSidebar from "@/components/social/AppSidebar";
import { ShieldAlert, Users, Search, UserCheck, Shield, Award, HelpCircle } from "lucide-react";

interface RbacUser {
  id: number;
  username: string;
  displayName: string;
  email: string;
  role: "ADMIN" | "MODERATOR" | "USER" | "GUEST";
  permissions: string[];
}

export default function RbacManagement() {
  const [searchQuery, setSearchQuery] = useState("");
  const [users, setUsers] = useState<RbacUser[]>([
    {
      id: 1,
      username: "admin_social",
      displayName: "Lê Minh Admin",
      email: "admin@socialpulse.com",
      role: "ADMIN",
      permissions: ["post:create", "post:delete", "user:manage", "report:manage", "ai:configure"],
    },
    {
      id: 2,
      username: "quy_dam",
      displayName: "Đàm Phú Quý",
      email: "damphuquy@socialpulse.com",
      role: "MODERATOR",
      permissions: ["post:create", "post:delete", "report:manage"],
    },
    {
      id: 3,
      username: "trung_chau",
      displayName: "Trung Châu",
      email: "trungchau@socialpulse.com",
      role: "USER",
      permissions: ["post:create", "post:read"],
    },
    {
      id: 4,
      username: "guest_tester",
      displayName: "Khách Thử Nghiệm",
      email: "guest@socialpulse.com",
      role: "GUEST",
      permissions: ["post:read"],
    },
  ]);

  const handleRoleChange = (userId: number, newRole: "ADMIN" | "MODERATOR" | "USER" | "GUEST") => {
    let nextPermissions: string[] = [];
    if (newRole === "ADMIN") {
      nextPermissions = ["post:create", "post:delete", "user:manage", "report:manage", "ai:configure"];
    } else if (newRole === "MODERATOR") {
      nextPermissions = ["post:create", "post:delete", "report:manage"];
    } else if (newRole === "USER") {
      nextPermissions = ["post:create", "post:read"];
    } else {
      nextPermissions = ["post:read"];
    }

    setUsers(prev => 
      prev.map(user => 
        user.id === userId 
          ? { ...user, role: newRole, permissions: nextPermissions } 
          : user
      )
    );

    toast.success(`Đã cập nhật vai trò của @${users.find(u => u.id === userId)?.username} thành ${newRole}!`);
  };

  const filteredUsers = users.filter(user => 
    user.displayName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    user.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
    user.email.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />
      
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-8 pt-24 px-6 lg:px-10">
        <AppSidebar active="settings" />

        {/* Main Content Area */}
        <div className="min-w-0 space-y-6 pb-10">
            
            {/* Header Card */}
            <div className="flex items-center space-x-3 bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm">
              <div className="p-3 bg-indigo-500/10 text-indigo-500 rounded-xl">
                <ShieldAlert className="w-8 h-8" />
              </div>
              <div>
                <h1 className="text-2xl font-bold tracking-tight">Quản lý Quyền và Vai trò (RBAC)</h1>
                <p className="text-slate-500 dark:text-slate-400 text-sm mt-0.5">
                  Phân quyền bảo mật RBAC, gán vai trò quản trị cho các tài khoản thành viên trong hệ thống.
                </p>
              </div>
            </div>

            {/* Quick stats and Search */}
            <div className="grid grid-cols-1 md:grid-cols-[1fr_300px] gap-4">
              {/* Search Bar */}
              <div className="relative">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 dark:text-neutral-500" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Tìm kiếm người dùng theo tên, username hoặc email..."
                  className="w-full bg-white dark:bg-[#1e1e1e] border border-slate-200/80 dark:border-[#2a2a2a] focus:border-indigo-500 rounded-2xl py-3 pl-12 pr-4 text-sm outline-none transition-all shadow-sm"
                />
              </div>
              
              {/* Stats Card */}
              <div className="bg-white dark:bg-[#1e1e1e] px-5 py-3 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Users className="w-5 h-5 text-indigo-500" />
                  <span className="text-sm font-semibold">Tổng số tài khoản</span>
                </div>
                <span className="text-xl font-bold text-indigo-500">{users.length}</span>
              </div>
            </div>

            {/* Users RBAC Table */}
            <div className="bg-white dark:bg-[#1e1e1e] rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-slate-200/80 dark:border-[#2a2a2a] text-slate-500 dark:text-neutral-400 text-xs font-semibold uppercase bg-slate-50/50 dark:bg-neutral-800/20">
                      <th className="px-6 py-4">Thành viên</th>
                      <th className="px-6 py-4">Vai trò hiện tại</th>
                      <th className="px-6 py-4">Quyền hạn kích hoạt</th>
                      <th className="px-6 py-4 text-right">Thay đổi vai trò</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200/80 dark:divide-[#2a2a2a] text-sm">
                    {filteredUsers.map((user) => (
                      <tr key={user.id} className="hover:bg-slate-50/50 dark:hover:bg-neutral-800/30 transition-colors">
                        {/* Member Details */}
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-full bg-indigo-500/10 text-indigo-500 flex items-center justify-center font-bold text-sm">
                              {user.displayName[0]}
                            </div>
                            <div>
                              <h4 className="font-bold text-slate-800 dark:text-slate-200">{user.displayName}</h4>
                              <p className="text-xs text-slate-500 dark:text-slate-400">@{user.username} · {user.email}</p>
                            </div>
                          </div>
                        </td>

                        {/* Current Role Badge */}
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold ${
                            user.role === "ADMIN" ? "bg-red-500/10 text-red-500" :
                            user.role === "MODERATOR" ? "bg-orange-500/10 text-orange-500" :
                            user.role === "USER" ? "bg-blue-500/10 text-blue-500" :
                            "bg-slate-500/10 text-slate-500"
                          }`}>
                            {user.role === "ADMIN" && <Shield className="w-3.5 h-3.5" />}
                            {user.role === "MODERATOR" && <Award className="w-3.5 h-3.5" />}
                            {user.role === "USER" && <UserCheck className="w-3.5 h-3.5" />}
                            {user.role === "GUEST" && <HelpCircle className="w-3.5 h-3.5" />}
                            {user.role}
                          </span>
                        </td>

                        {/* Allowed Permissions list */}
                        <td className="px-6 py-4">
                          <div className="flex flex-wrap gap-1 max-w-sm">
                            {user.permissions.map((perm) => (
                              <span key={perm} className="px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-650 dark:text-slate-400 rounded-md text-[10px] font-semibold font-mono">
                                {perm}
                              </span>
                            ))}
                          </div>
                        </td>

                        {/* Dropdown Action */}
                        <td className="px-6 py-4 text-right">
                          <select
                            value={user.role}
                            onChange={(e) => handleRoleChange(user.id, e.target.value as any)}
                            className="bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 rounded-xl px-3 py-1.5 text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500"
                          >
                            <option value="ADMIN">ADMIN</option>
                            <option value="MODERATOR">MODERATOR</option>
                            <option value="USER">USER</option>
                            <option value="GUEST">GUEST</option>
                          </select>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

          </div>
        </div>
      </div>
  );
}
