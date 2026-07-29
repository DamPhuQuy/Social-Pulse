import { useEffect, useState } from "react";
import { toast } from "sonner";
import AppHeader from "@/shared/components/AppHeader";
import AppSidebar from "@/shared/components/AppSidebar";
import {
  getReports,
  reviewReport,
  type ReportResponse,
  type ReportStatus,
  type ReviewAction,
} from "@/features/social-relations/infrastructure/api/reportService";
import {
  ShieldCheck,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Clock,
  UserX,
  MessageSquareWarning,
} from "lucide-react";

export default function AdminDashboard() {
  const [reports, setReports] = useState<ReportResponse[]>([]);
  const [statusFilter, setStatusFilter] = useState<ReportStatus | "ALL">("PENDING");
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [reviewingId, setReviewingId] = useState<number | null>(null);
  const [stats, setStats] = useState({ pending: 0, resolved: 0, rejected: 0 });

  const loadReports = async () => {
    setLoading(true);
    const filter = statusFilter === "ALL" ? undefined : statusFilter;
    const res = await getReports(filter, page, 10);
    setLoading(false);
    if (res.ok && res.data) {
      setReports(res.data.items);
      setTotalPages(res.data.totalPages);
    } else {
      toast.error(res.message ?? "Không thể tải danh sách báo cáo.");
    }
  };

  const loadStats = async () => {
    const [pendingRes, resolvedRes, rejectedRes] = await Promise.all([
      getReports("PENDING", 0, 1),
      getReports("RESOLVED", 0, 1),
      getReports("REJECTED", 0, 1),
    ]);

    setStats({
      pending: pendingRes.data?.totalElements ?? 0,
      resolved: resolvedRes.data?.totalElements ?? 0,
      rejected: rejectedRes.data?.totalElements ?? 0,
    });
  };

  useEffect(() => {
    void loadReports();
    void loadStats();
  }, [statusFilter, page]);

  const handleReview = async (reportId: number, action: ReviewAction) => {
    setReviewingId(reportId);
    const res = await reviewReport(reportId, action);
    setReviewingId(null);
    if (!res.ok) {
      toast.error(res.message ?? "Xử lý báo cáo thất bại.");
      return;
    }

    toast.success(getReviewSuccessMessage(action));
    void loadReports();
    void loadStats();
  };

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />

      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-8 pt-24 px-6 lg:px-10">
        <AppSidebar active="admin-reports" />

        <div className="min-w-0 space-y-6 pb-10">
          <div className="flex items-center space-x-3 bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm">
            <div className="p-3 bg-red-500/10 text-red-500 rounded-xl">
              <ShieldCheck className="w-8 h-8" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight">Hệ thống Quản trị & Báo cáo</h1>
              <p className="text-slate-500 dark:text-slate-400 text-sm mt-0.5">
                Giao diện này đang gọi trực tiếp luồng moderation từ backend để ẩn nội dung, khóa tài khoản hoặc bác bỏ báo cáo.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white dark:bg-[#1e1e1e] p-5 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm flex items-center justify-between">
              <div>
                <span className="text-slate-500 dark:text-slate-400 text-sm font-medium">Báo cáo chờ duyệt</span>
                <h3 className="text-2xl font-bold mt-1 text-yellow-500">{stats.pending}</h3>
              </div>
              <div className="p-3 bg-yellow-500/10 text-yellow-500 rounded-xl">
                <Clock className="w-6 h-6" />
              </div>
            </div>
            <div className="bg-white dark:bg-[#1e1e1e] p-5 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm flex items-center justify-between">
              <div>
                <span className="text-slate-500 dark:text-slate-400 text-sm font-medium">Đã xử lý</span>
                <h3 className="text-2xl font-bold mt-1 text-green-500">{stats.resolved}</h3>
              </div>
              <div className="p-3 bg-green-500/10 text-green-500 rounded-xl">
                <CheckCircle className="w-6 h-6" />
              </div>
            </div>
            <div className="bg-white dark:bg-[#1e1e1e] p-5 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm flex items-center justify-between">
              <div>
                <span className="text-slate-500 dark:text-slate-400 text-sm font-medium">Đã bác bỏ</span>
                <h3 className="text-2xl font-bold mt-1 text-red-500">{stats.rejected}</h3>
              </div>
              <div className="p-3 bg-red-500/10 text-red-500 rounded-xl">
                <XCircle className="w-6 h-6" />
              </div>
            </div>
          </div>

          <div className="flex flex-wrap gap-2 items-center justify-between bg-white dark:bg-[#1e1e1e] p-4 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm">
            <div className="flex flex-wrap gap-2">
              {(["PENDING", "RESOLVED", "REJECTED", "ALL"] as const).map((status) => (
                <button
                  key={status}
                  onClick={() => {
                    setStatusFilter(status);
                    setPage(0);
                  }}
                  className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-200 ${
                    statusFilter === status
                      ? "bg-blue-600 text-white shadow-lg shadow-blue-600/20"
                      : "bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300"
                  }`}
                >
                  {status === "PENDING" && "Chờ xử lý"}
                  {status === "RESOLVED" && "Đã giải quyết"}
                  {status === "REJECTED" && "Đã bác bỏ"}
                  {status === "ALL" && "Tất cả báo cáo"}
                </button>
              ))}
            </div>
          </div>

          <div className="bg-white dark:bg-[#1e1e1e] rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm overflow-hidden">
            {loading ? (
              <div className="p-12 text-center text-slate-500 dark:text-neutral-400">
                <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                Đang tải danh sách báo cáo...
              </div>
            ) : reports.length === 0 ? (
              <div className="p-12 text-center text-slate-500 dark:text-neutral-400 flex flex-col items-center">
                <AlertTriangle className="w-12 h-12 text-slate-300 dark:text-neutral-600 mb-3" />
                Không tìm thấy báo cáo nào trong danh mục này.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-slate-200/80 dark:border-[#2a2a2a] text-slate-500 dark:text-neutral-400 text-xs font-semibold uppercase bg-slate-50/50 dark:bg-neutral-800/20">
                      <th className="px-6 py-4">ID</th>
                      <th className="px-6 py-4">Loại mục</th>
                      <th className="px-6 py-4">ID mục</th>
                      <th className="px-6 py-4">Lý do</th>
                      <th className="px-6 py-4">Nội dung bị nhắm</th>
                      <th className="px-6 py-4">Trạng thái</th>
                      <th className="px-6 py-4">Thời gian</th>
                      <th className="px-6 py-4 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200/80 dark:divide-[#2a2a2a] text-sm">
                    {reports.map((report) => (
                      <tr key={report.id} className="hover:bg-slate-50/50 dark:hover:bg-neutral-800/30 transition-colors">
                        <td className="px-6 py-4 font-mono font-medium text-xs">#{report.id}</td>
                        <td className="px-6 py-4">
                          <span className={`px-2.5 py-1 rounded-lg text-xs font-semibold ${
                            report.targetType === "POST"
                              ? "bg-purple-500/10 text-purple-500"
                              : report.targetType === "COMMENT"
                                ? "bg-indigo-500/10 text-indigo-500"
                                : "bg-pink-500/10 text-pink-500"
                          }`}>
                            {report.targetType}
                          </span>
                        </td>
                        <td className="px-6 py-4 font-mono text-xs">#{report.targetId}</td>
                        <td className="px-6 py-4 max-w-xs">
                          <p className="font-medium text-slate-700 dark:text-slate-300 line-clamp-2">{report.reason}</p>
                        </td>
                        <td className="px-6 py-4 max-w-sm">
                          <div className="space-y-1">
                            <p className="text-xs text-slate-700 dark:text-slate-300 line-clamp-2">
                              {report.targetContent?.trim() || "Backend chưa enrich nội dung mục bị báo cáo."}
                            </p>
                            {report.targetOwnerUsername && (
                              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                                Owner: @{report.targetOwnerUsername}
                              </p>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold ${
                            report.status === "PENDING"
                              ? "bg-yellow-500/10 text-yellow-500"
                              : report.status === "RESOLVED"
                                ? "bg-green-500/10 text-green-500"
                                : "bg-red-500/10 text-red-500"
                          }`}>
                            {report.status === "PENDING" && <Clock className="w-3.5 h-3.5" />}
                            {report.status === "RESOLVED" && <CheckCircle className="w-3.5 h-3.5" />}
                            {report.status === "REJECTED" && <XCircle className="w-3.5 h-3.5" />}
                            {report.status === "PENDING" ? "Chờ xử lý" : report.status === "RESOLVED" ? "Đã xử lý" : "Bác bỏ"}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-xs text-slate-500 dark:text-slate-400">
                          {new Date(report.createdAt).toLocaleString("vi-VN")}
                        </td>
                        <td className="px-6 py-4">
                          {report.status === "PENDING" ? (
                            <div className="flex flex-wrap justify-end gap-2">
                              <ActionButton
                                icon={MessageSquareWarning}
                                label="Ẩn nội dung"
                                intent="success"
                                disabled={reviewingId === report.id}
                                onClick={() => handleReview(report.id, "DELETE_CONTENT")}
                              />
                              <ActionButton
                                icon={UserX}
                                label="Khóa user"
                                intent="warning"
                                disabled={reviewingId === report.id}
                                onClick={() => handleReview(report.id, "BAN_USER")}
                              />
                              <ActionButton
                                icon={ShieldCheck}
                                label="Ẩn + khóa"
                                intent="danger"
                                disabled={reviewingId === report.id}
                                onClick={() => handleReview(report.id, "DELETE_CONTENT_AND_BAN_USER")}
                              />
                              <ActionButton
                                icon={XCircle}
                                label="Bác bỏ"
                                intent="neutral"
                                disabled={reviewingId === report.id}
                                onClick={() => handleReview(report.id, "REJECT")}
                              />
                            </div>
                          ) : (
                            <div className="text-right text-xs text-slate-400 dark:text-slate-500">
                              Đã chốt moderation
                            </div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {totalPages > 1 && (
              <div className="flex items-center justify-between px-6 py-4 border-t border-slate-200/80 dark:border-[#2a2a2a]">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  className="px-3.5 py-1.5 bg-slate-100 dark:bg-neutral-800 disabled:opacity-50 text-xs font-semibold rounded-lg"
                >
                  Trang trước
                </button>
                <span className="text-xs text-slate-500">
                  Trang {page + 1} / {totalPages}
                </span>
                <button
                  disabled={page === totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  className="px-3.5 py-1.5 bg-slate-100 dark:bg-neutral-800 disabled:opacity-50 text-xs font-semibold rounded-lg"
                >
                  Trang sau
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function ActionButton({
  icon: Icon,
  label,
  intent,
  disabled,
  onClick,
}: {
  icon: React.FC<{ className?: string }>;
  label: string;
  intent: "success" | "warning" | "danger" | "neutral";
  disabled?: boolean;
  onClick: () => void;
}) {
  const classes =
    intent === "success"
      ? "bg-green-600 hover:bg-green-700 text-white shadow-green-600/10"
      : intent === "warning"
        ? "bg-orange-500 hover:bg-orange-600 text-white"
        : intent === "danger"
          ? "bg-red-600 hover:bg-red-700 text-white"
          : "bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200";

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition disabled:opacity-60 ${classes}`}
    >
      <Icon className="w-3.5 h-3.5" />
      {label}
    </button>
  );
}

function getReviewSuccessMessage(action: ReviewAction) {
  switch (action) {
    case "DELETE_CONTENT":
      return "Đã ẩn nội dung bị báo cáo.";
    case "BAN_USER":
      return "Đã khóa tài khoản liên quan.";
    case "DELETE_CONTENT_AND_BAN_USER":
      return "Đã ẩn nội dung và khóa tài khoản liên quan.";
    case "REJECT":
    default:
      return "Đã bác bỏ báo cáo.";
  }
}
