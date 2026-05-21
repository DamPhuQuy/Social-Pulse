import { useEffect, useState } from "react";
import { Loader2, ShieldAlert, X } from "lucide-react";
import { toast } from "sonner";
import { createReport, type ReportTargetType } from "@/services/social/reportService";

interface ReportModalProps {
  isOpen: boolean;
  targetType: ReportTargetType;
  targetId: number | null;
  title: string;
  onClose: () => void;
  onReportSuccess?: (options: { hidePost: boolean; hideUser: boolean }) => void;
}

const REPORT_REASONS = [
  "Spam hoặc quảng cáo rác",
  "Nội dung bạo lực hoặc gây thù ghét",
  "Quấy rối hoặc bắt nạt",
  "Nội dung khỏa thân hoặc khiêu dâm",
  "Thông tin sai lệch hoặc tin giả",
  "Khác (vui lòng mô tả bên dưới)"
];

export default function ReportModal({ isOpen, targetType, targetId, title, onClose, onReportSuccess }: ReportModalProps) {
  const [selectedReason, setSelectedReason] = useState("");
  const [customReason, setCustomReason] = useState("");
  const [hidePost, setHidePost] = useState(false);
  const [hideUser, setHideUser] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setSelectedReason("");
      setCustomReason("");
      setHidePost(false);
      setHideUser(false);
      setSubmitting(false);
    }
  }, [isOpen]);

  if (!isOpen || !targetId) return null;

  const handleReasonSelect = (reason: string) => {
    setSelectedReason(reason);
  };

  const getFinalReason = () => {
    if (selectedReason === "Khác (vui lòng mô tả bên dưới)") {
      return customReason.trim();
    }
    return selectedReason;
  };

  const handleSubmit = async () => {
    const finalReason = getFinalReason();
    if (!finalReason) {
      toast.warning("Vui lòng chọn hoặc mô tả lý do báo cáo.");
      return;
    }

    setSubmitting(false);
    setSubmitting(true);
    const res = await createReport({ targetType, targetId, reason: finalReason });
    setSubmitting(false);

    if (!res.ok) {
      toast.error(res.message ?? "Không thể gửi báo cáo.");
      return;
    }

    toast.success("Đã gửi báo cáo thành công.");
    
    // Call the success callback with hiding preferences
    if (onReportSuccess) {
      onReportSuccess({ hidePost, hideUser });
    }
    
    onClose();
  };

  const isCustomSelected = selectedReason === "Khác (vui lòng mô tả bên dưới)";
  const isSubmitDisabled = submitting || !selectedReason || (isCustomSelected && !customReason.trim());

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-lg rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-neutral-800 dark:bg-neutral-900 transition-all">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 dark:border-neutral-800">
          <div className="flex items-center gap-2">
            <ShieldAlert className="h-5 w-5 text-red-500" />
            <h2 className="font-bold text-slate-900 dark:text-white">Báo cáo {title}</h2>
          </div>
          <button onClick={onClose} className="rounded-full p-2 text-slate-400 hover:bg-slate-100 dark:hover:bg-neutral-800 transition-colors">
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-5 max-h-[70vh] overflow-y-auto flex flex-col gap-4">
          <div>
            <p className="text-sm text-slate-500 dark:text-neutral-400 mb-3">
              Vui lòng chọn lý do báo cáo để hệ thống và quản trị viên xử lý sớm nhất.
            </p>
            
            {/* Reasons List */}
            <div className="flex flex-col gap-2.5">
              {REPORT_REASONS.map((reason) => {
                const isSelected = selectedReason === reason;
                return (
                  <label
                    key={reason}
                    onClick={() => handleReasonSelect(reason)}
                    className={`flex items-center gap-3 px-4 py-2.5 rounded-xl border text-sm font-medium cursor-pointer transition-all ${
                      isSelected
                        ? "border-red-500 bg-red-50/50 text-red-700 dark:bg-red-500/10 dark:text-red-400 dark:border-red-500/30"
                        : "border-slate-200 hover:bg-slate-50 text-slate-700 dark:border-neutral-850 dark:hover:bg-neutral-800/40 dark:text-neutral-300"
                    }`}
                  >
                    <input
                      type="radio"
                      name="reportReason"
                      checked={isSelected}
                      onChange={() => {}}
                      className="accent-red-600 dark:accent-red-500 h-4 w-4 shrink-0"
                    />
                    <span>{reason}</span>
                  </label>
                );
              })}
            </div>
          </div>

          {/* Custom Description Textarea */}
          {isCustomSelected && (
            <div className="animate-in fade-in slide-in-from-top-2 duration-250">
              <textarea
                rows={3}
                value={customReason}
                onChange={(e) => setCustomReason(e.target.value)}
                placeholder="Vui lòng mô tả chi tiết vi phạm tại đây..."
                className="w-full resize-none rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none focus:border-red-500 focus:bg-white dark:border-neutral-800 dark:bg-neutral-950 dark:text-white dark:focus:border-red-500/50"
              />
            </div>
          )}

          {/* Hiding Options (Only for POST or COMMENT) */}
          {(targetType === "POST" || targetType === "COMMENT") && (
            <div className="mt-2 border-t border-slate-100 pt-4 dark:border-neutral-800 flex flex-col gap-3">
              <p className="text-xs font-bold text-slate-500 dark:text-neutral-500 uppercase tracking-wider">
                Lựa chọn hiển thị
              </p>
              
              <label className="flex items-center gap-3 cursor-pointer text-sm text-slate-700 dark:text-neutral-300 select-none">
                <input
                  type="checkbox"
                  checked={hidePost}
                  onChange={(e) => setHidePost(e.target.checked)}
                  className="rounded border-slate-300 text-red-600 focus:ring-red-500 h-4 w-4 dark:border-neutral-800 dark:bg-neutral-950 accent-red-600"
                />
                <div>
                  <p className="font-semibold text-slate-800 dark:text-white">Ẩn nội dung này</p>
                  <p className="text-xs text-slate-500 dark:text-neutral-400">Ẩn bài đăng này khỏi bảng tin của bạn.</p>
                </div>
              </label>

              <label className="flex items-center gap-3 cursor-pointer text-sm text-slate-700 dark:text-neutral-300 select-none">
                <input
                  type="checkbox"
                  checked={hideUser}
                  onChange={(e) => setHideUser(e.target.checked)}
                  className="rounded border-slate-300 text-red-600 focus:ring-red-500 h-4 w-4 dark:border-neutral-800 dark:bg-neutral-950 accent-red-600"
                />
                <div>
                  <p className="font-semibold text-slate-800 dark:text-white">Ẩn tất cả từ người dùng này</p>
                  <p className="text-xs text-slate-500 dark:text-neutral-400">Ẩn toàn bộ bài viết khác của người dùng này khỏi bảng tin.</p>
                </div>
              </label>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="flex justify-end gap-3 border-t border-slate-100 px-5 py-4 dark:border-neutral-800">
          <button
            onClick={onClose}
            className="rounded-xl border border-slate-200 hover:bg-slate-50 px-4 py-2 text-sm font-semibold dark:border-neutral-850 dark:hover:bg-neutral-800 transition-colors"
          >
            Hủy
          </button>
          <button
            onClick={handleSubmit}
            disabled={isSubmitDisabled}
            className="flex items-center gap-2 rounded-xl bg-red-600 hover:bg-red-700 px-5 py-2 text-sm font-semibold text-white disabled:opacity-50 transition-colors shadow-md shadow-red-500/10"
          >
            {submitting ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            Gửi báo cáo
          </button>
        </div>
      </div>
    </div>
  );
}
