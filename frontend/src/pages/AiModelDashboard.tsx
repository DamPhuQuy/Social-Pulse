import { useEffect, useState } from "react";
import { toast } from "sonner";
import AppHeader from "@/components/social/AppHeader";
import AppSidebar from "@/components/social/AppSidebar";
import { Brain, Cpu, Layers, Link2, Loader2, ShieldCheck } from "lucide-react";
import { getAiStatus, getAdminMetrics, type AiStatusResponse, type MetricsPeriod, type SystemMetricsResponse } from "@/services/admin/adminService";

export default function AiModelDashboard() {
  const [status, setStatus] = useState<AiStatusResponse | null>(null);
  const [metrics, setMetrics] = useState<SystemMetricsResponse | null>(null);
  const [period, setPeriod] = useState<MetricsPeriod>("LAST_30_DAYS");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void loadData(period);
  }, [period]);

  const loadData = async (nextPeriod: MetricsPeriod) => {
    setLoading(true);
    const [statusRes, metricsRes] = await Promise.all([
      getAiStatus(),
      getAdminMetrics(nextPeriod),
    ]);
    setLoading(false);

    if (statusRes.ok && statusRes.data) {
      setStatus(statusRes.data);
    } else {
      toast.error(statusRes.message ?? "Không thể tải trạng thái AI.");
    }

    if (metricsRes.ok && metricsRes.data) {
      setMetrics(metricsRes.data);
    } else {
      toast.error(metricsRes.message ?? "Không thể tải thống kê hệ thống.");
    }
  };

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />

      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-8 pt-24 px-6 lg:px-10">
        <AppSidebar active="admin-ai" />

        <div className="min-w-0 space-y-6 pb-10">
          <div className="flex flex-wrap items-center justify-between gap-4 bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm">
            <div className="flex items-center space-x-4">
              <div className="p-3.5 bg-blue-500/10 text-blue-500 rounded-xl">
                <Brain className="w-8 h-8" />
              </div>
              <div>
                <h1 className="text-2xl font-bold tracking-tight">Giám sát AI Feed Ranking</h1>
                <p className="text-slate-500 dark:text-slate-400 text-sm mt-0.5">
                  Theo dõi trạng thái AI pipeline, schema model và các chỉ số vận hành từ backend.
                </p>
              </div>
            </div>

            <select
              value={period}
              onChange={(e) => setPeriod(e.target.value as MetricsPeriod)}
              className="bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 rounded-xl px-3 py-2 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="LAST_7_DAYS">7 ngày</option>
              <option value="LAST_30_DAYS">30 ngày</option>
              <option value="LAST_90_DAYS">90 ngày</option>
              <option value="ALL_TIME">Toàn thời gian</option>
            </select>
          </div>

          {loading ? (
            <div className="bg-white dark:bg-[#1e1e1e] p-10 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm flex items-center justify-center text-slate-500 dark:text-neutral-400">
              <Loader2 className="w-6 h-6 animate-spin mr-2" />
              Đang tải trạng thái AI...
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm space-y-4">
                  <div className="flex items-center gap-2 text-slate-800 dark:text-white pb-3 border-b border-slate-200/80 dark:border-[#2a2a2a]">
                    <ShieldCheck className="w-5 h-5 text-green-500" />
                    <h3 className="font-bold text-base">Runtime AI Pipeline</h3>
                  </div>
                  <MetricRow label="AI enabled" value={status?.enabled ? "BẬT" : "TẮT"} valueClass={status?.enabled ? "text-green-500" : "text-amber-500"} />
                  <MetricRow label="Health check" value={status?.healthReachable ? "REACHABLE" : "UNREACHABLE"} valueClass={status?.healthReachable ? "text-green-500" : "text-red-500"} />
                  <MetricRow label="Feature schema" value={status?.featureSchemaVersion ?? "N/A"} />
                  <MetricRow label="Training controls" value={status?.trainingControlsAvailable ? "AVAILABLE" : "NOT EXPOSED"} valueClass={status?.trainingControlsAvailable ? "text-green-500" : "text-slate-500"} />
                </div>

                <div className="bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm space-y-4">
                  <div className="flex items-center gap-2 text-slate-800 dark:text-white pb-3 border-b border-slate-200/80 dark:border-[#2a2a2a]">
                    <Link2 className="w-5 h-5 text-purple-500" />
                    <h3 className="font-bold text-base">Connection Details</h3>
                  </div>
                  <MetricRow label="Base URL" value={status?.baseUrl ?? "N/A"} mono />
                  <MetricRow label="Metrics period" value={metrics?.period ?? period} mono />
                  <MetricRow label="Generated at" value={metrics?.generatedAt ? new Date(metrics.generatedAt).toLocaleString("vi-VN") : "N/A"} />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <StatCard icon={Cpu} label="Tổng user" value={metrics?.totalUsers ?? 0} />
                <StatCard icon={Layers} label="User mới" value={metrics?.newUsers ?? 0} />
                <StatCard icon={Brain} label="Tổng post" value={metrics?.totalPosts ?? 0} />
                <StatCard icon={ShieldCheck} label="Post mới" value={metrics?.newPosts ?? 0} />
              </div>

              <div className="bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm space-y-4">
                <div className="flex items-center gap-2 text-slate-800 dark:text-white pb-3 border-b border-slate-200/80 dark:border-[#2a2a2a]">
                  <Cpu className="w-5 h-5 text-blue-500" />
                  <h3 className="font-bold text-base">Trạng thái vận hành</h3>
                </div>
                <ul className="text-sm text-slate-600 dark:text-slate-300 space-y-2 list-disc pl-5">
                  <li>Feed ranking đang gọi AI pipeline qua HTTP từ backend bằng `ai.pipeline.base-url`.</li>
                  <li>Schema version hiện hành phải khớp giữa backend và artifact/model server trước khi train hoặc deploy model mới.</li>
                  <li>Training được thực thi qua pipeline offline, còn UI này chỉ hiển thị trạng thái runtime và chỉ số hệ thống.</li>
                </ul>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function MetricRow({
  label,
  value,
  valueClass = "",
  mono = false,
}: {
  label: string;
  value: string;
  valueClass?: string;
  mono?: boolean;
}) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-slate-500 dark:text-slate-400 text-sm">{label}</span>
      <span className={`text-sm font-semibold ${mono ? "font-mono" : ""} ${valueClass}`}>{value}</span>
    </div>
  );
}

function StatCard({
  icon: Icon,
  label,
  value,
}: {
  icon: React.FC<{ className?: string }>;
  label: string;
  value: number;
}) {
  return (
    <div className="bg-white dark:bg-[#1e1e1e] p-5 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm flex items-center justify-between">
      <div>
        <span className="text-slate-500 dark:text-slate-400 text-sm font-medium">{label}</span>
        <h3 className="text-2xl font-bold mt-1 text-slate-900 dark:text-white">{value}</h3>
      </div>
      <div className="p-3 bg-blue-500/10 text-blue-500 rounded-xl">
        <Icon className="w-6 h-6" />
      </div>
    </div>
  );
}
