import { useEffect, useState } from "react";
import { X, ShieldCheck, UserX, MessageSquareWarning, XCircle, Loader2, AlertTriangle, FileText } from "lucide-react";
import { toast } from "sonner";
import { getReportDetail, reviewReport, type ReportResponse, type ReviewAction } from "@/services/social/reportService";

interface ReportDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  reportId: number | null;
  onActionComplete: () => void;
}

export default function ReportDetailModal({ isOpen, onClose, reportId, onActionComplete }: ReportDetailModalProps) {
  const [report, setReport] = useState<ReportResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState<ReviewAction | null>(null);

  useEffect(() => {
    if (isOpen && reportId) {
      loadDetail(reportId);
    } else {
      setReport(null);
      setActionLoading(null);
    }
  }, [isOpen, reportId]);

  const loadDetail = async (id: number) => {
    setLoading(true);
    const res = await getReportDetail(id);
    setLoading(false);
    if (res.ok && res.data) {
      setReport(res.data);
    } else {
      toast.error(res.message ?? "Không thể tải chi tiết báo cáo.");
      onClose();
    }
  };

  const handleReview = async (action: ReviewAction) => {
    if (!reportId) return;
    setActionLoading(action);
    const res = await reviewReport(reportId, action);
    setActionLoading(null);
    
    if (!res.ok) {
      toast.error(res.message ?? "Xử lý báo cáo thất bại.");
      return;
    }

    toast.success("Đã xử lý báo cáo thành công.");
    onActionComplete();
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white dark:bg-[#1e1e1e] rounded-2xl shadow-2xl w-full max-w-2xl overflow-hidden flex flex-col max-h-[90vh] border border-slate-200 dark:border-neutral-800">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 dark:border-neutral-800 bg-slate-50/50 dark:bg-[#1e1e1e]">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-500/10 text-blue-500 rounded-xl">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-slate-800 dark:text-neutral-100 leading-tight">Chi tiết báo cáo</h2>
              <p className="text-xs text-slate-500 dark:text-neutral-400 font-medium">#{reportId}</p>
            </div>
          </div>
          <button 
            onClick={onClose}
            className="p-2 hover:bg-slate-200 dark:hover:bg-neutral-800 rounded-full transition-colors text-slate-500"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {loading || !report ? (
            <div className="flex flex-col items-center justify-center py-12 text-slate-500">
              <Loader2 className="w-8 h-8 animate-spin mb-4 text-blue-500" />
              <p>Đang tải chi tiết nội dung...</p>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-slate-50 dark:bg-neutral-800/50 p-4 rounded-xl border border-slate-100 dark:border-neutral-800">
                  <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Loại mục tiêu</span>
                  <div className="font-medium text-slate-800 dark:text-neutral-200">{report.targetType}</div>
                </div>
                <div className="bg-slate-50 dark:bg-neutral-800/50 p-4 rounded-xl border border-slate-100 dark:border-neutral-800">
                  <span className="block text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">ID mục tiêu</span>
                  <div className="font-mono text-slate-800 dark:text-neutral-200">#{report.targetId}</div>
                </div>
              </div>

              <div className="bg-red-50 dark:bg-red-500/10 p-4 rounded-xl border border-red-100 dark:border-red-500/20">
                <span className="block text-xs font-semibold text-red-600 dark:text-red-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                  <AlertTriangle className="w-3.5 h-3.5" />
                  Lý do báo cáo
                </span>
                <p className="text-slate-800 dark:text-red-100 font-medium whitespace-pre-wrap">
                  {report.reason}
                </p>
              </div>

              <div className="space-y-3">
                <h3 className="text-sm font-semibold text-slate-800 dark:text-neutral-200 flex justify-between items-end">
                  Nội dung bị báo cáo:
                  {report.targetOwnerUsername && (
                    <span className="text-xs font-medium text-slate-500 bg-slate-100 dark:bg-neutral-800 px-2 py-1 rounded-md">
                      Tác giả: @{report.targetOwnerUsername}
                    </span>
                  )}
                </h3>
                <div className="bg-slate-100 dark:bg-[#121212] p-5 rounded-xl border border-slate-200 dark:border-neutral-800 shadow-inner">
                  <p className="text-slate-700 dark:text-neutral-300 whitespace-pre-wrap leading-relaxed">
                    {report.targetContent?.trim() || <span className="italic text-slate-400">Không có nội dung.</span>}
                  </p>
                </div>
              </div>
            </>
          )}
        </div>

        {/* Footer Actions */}
        {report && report.status === "PENDING" && (
          <div className="p-4 border-t border-slate-100 dark:border-neutral-800 bg-slate-50/80 dark:bg-[#1e1e1e]">
            <p className="text-xs font-semibold text-slate-500 mb-3 text-center uppercase tracking-wider">Hành động kiểm duyệt</p>
            <div className="flex flex-wrap justify-center gap-3">
              <ActionButton
                icon={MessageSquareWarning}
                label="Ẩn nội dung"
                intent="success"
                loading={actionLoading === "DELETE_CONTENT"}
                disabled={actionLoading !== null}
                onClick={() => handleReview("DELETE_CONTENT")}
              />
              <ActionButton
                icon={UserX}
                label="Khóa tác giả"
                intent="warning"
                loading={actionLoading === "BAN_USER"}
                disabled={actionLoading !== null}
                onClick={() => handleReview("BAN_USER")}
              />
              <ActionButton
                icon={ShieldCheck}
                label="Ẩn & Khóa"
                intent="danger"
                loading={actionLoading === "DELETE_CONTENT_AND_BAN_USER"}
                disabled={actionLoading !== null}
                onClick={() => handleReview("DELETE_CONTENT_AND_BAN_USER")}
              />
              <ActionButton
                icon={XCircle}
                label="Bác bỏ"
                intent="neutral"
                loading={actionLoading === "REJECT"}
                disabled={actionLoading !== null}
                onClick={() => handleReview("REJECT")}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function ActionButton({
  icon: Icon,
  label,
  intent,
  loading,
  disabled,
  onClick,
}: {
  icon: React.FC<{ className?: string }>;
  label: string;
  intent: "success" | "warning" | "danger" | "neutral";
  loading?: boolean;
  disabled?: boolean;
  onClick: () => void;
}) {
  const classes =
    intent === "success"
      ? "bg-green-600 hover:bg-green-700 text-white shadow-lg shadow-green-600/20"
      : intent === "warning"
        ? "bg-orange-500 hover:bg-orange-600 text-white shadow-lg shadow-orange-500/20"
        : intent === "danger"
          ? "bg-red-600 hover:bg-red-700 text-white shadow-lg shadow-red-600/20"
          : "bg-slate-200 dark:bg-neutral-800 hover:bg-slate-300 dark:hover:bg-neutral-700 text-slate-700 dark:text-neutral-200";

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`relative inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 disabled:opacity-60 disabled:cursor-not-allowed ${classes}`}
    >
      {loading ? (
        <Loader2 className="w-4 h-4 animate-spin" />
      ) : (
        <Icon className="w-4 h-4" />
      )}
      {label}
    </button>
  );
}
