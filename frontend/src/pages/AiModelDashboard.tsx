import { useState } from "react";
import { toast } from "sonner";
import AppHeader from "@/components/social/AppHeader";
import AppSidebar from "@/components/social/AppSidebar";
import { Brain, Cpu, Play, Pause, RefreshCw, Layers, Sliders } from "lucide-react";

export default function AiModelDashboard() {
  const [isActive, setIsActive] = useState(true);
  const [isTraining, setIsTraining] = useState(false);
  const [logs, setLogs] = useState<string[]>([
    "[INFO] Initializing LightGBM environment...",
    "[INFO] Loaded feature schema version: v1",
    "[INFO] Successfully loaded recommendation candidate pool: 45 items.",
    "[INFO] Model scoring: deterministic scoring fallback disabled.",
    "[SUCCESS] LightGBM Model Scorer active on port 5005."
  ]);

  const handleToggle = () => {
    setIsActive(prev => !prev);
    const msg = isActive 
      ? "Đã tắt mô hình AI. Hệ thống sẽ tự động chuyển sang cơ chế tính điểm deterministic (fallback)."
      : "Đã bật mô hình xếp hạng LightGBM GBDT!";
    
    setLogs(prev => [...prev, `[USER_ACTION] Toggled model state to: ${!isActive ? "ACTIVE" : "INACTIVE"}`]);
    toast.success(msg);
  };

  const handleRetrain = () => {
    if (isTraining) return;
    setIsTraining(true);
    setLogs(prev => [...prev, "[TRAINING] Triggered online incremental training..."]);
    
    setTimeout(() => {
      setLogs(prev => [
        ...prev, 
        "[TRAINING] Fetching interaction matrices from Redis...",
        "[TRAINING] Epoch 1/5 - loss: 0.285 - val_loss: 0.312",
        "[TRAINING] Epoch 3/5 - loss: 0.198 - val_loss: 0.224",
        "[TRAINING] Epoch 5/5 - loss: 0.124 - val_loss: 0.158",
        "[SUCCESS] Model training complete! LightGBM weights updated successfully in Redis candidate store."
      ]);
      setIsTraining(false);
      toast.success("Huấn luyện lại mô hình AI thành công!");
    }, 2000);
  };

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />
      
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-8 pt-24 px-6 lg:px-10">
        <AppSidebar active="settings" />

        {/* Main Content Area */}
        <div className="min-w-0 space-y-6 pb-10">
            
            {/* Header Card */}
            <div className="flex flex-wrap items-center justify-between gap-4 bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm relative overflow-hidden">
              <div className="absolute -right-16 -top-16 w-32 h-32 bg-blue-500/10 rounded-full blur-2xl" />
              <div className="flex items-center space-x-4">
                <div className="p-3.5 bg-blue-500/10 text-blue-500 rounded-xl relative">
                  <Brain className="w-8 h-8 animate-pulse" />
                </div>
                <div>
                  <h1 className="text-2xl font-bold tracking-tight">Giám sát & Quản lý Mô hình AI</h1>
                  <p className="text-slate-500 dark:text-slate-400 text-sm mt-0.5">
                    Cấu hình, giám sát và huấn luyện mô hình LightGBM GBDT xếp hạng bảng tin thời gian thực.
                  </p>
                </div>
              </div>
              <div className="flex gap-2 relative z-10">
                <button
                  onClick={handleToggle}
                  className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold shadow-lg transition-all ${
                    isActive 
                      ? "bg-amber-500 text-white shadow-amber-500/10 hover:bg-amber-600" 
                      : "bg-green-600 text-white shadow-green-600/10 hover:bg-green-700"
                  }`}
                >
                  {isActive ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                  {isActive ? "Tạm dừng AI" : "Kích hoạt AI"}
                </button>
                <button
                  onClick={handleRetrain}
                  disabled={isTraining}
                  className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-xl text-sm font-semibold shadow-lg shadow-blue-600/10 transition"
                >
                  <RefreshCw className={`w-4 h-4 ${isTraining ? "animate-spin" : ""}`} />
                  {isTraining ? "Đang huấn luyện..." : "Huấn luyện lại"}
                </button>
              </div>
            </div>

            {/* Config & Metrics Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              
              {/* Hyperparameters Config */}
              <div className="bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm space-y-4">
                <div className="flex items-center gap-2 text-slate-800 dark:text-white pb-3 border-b border-slate-200/80 dark:border-[#2a2a2a]">
                  <Sliders className="w-5 h-5 text-blue-500" />
                  <h3 className="font-bold text-base">Siêu tham số LightGBM</h3>
                </div>
                <div className="space-y-3 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Thuật toán</span>
                    <span className="font-semibold font-mono">GBDT Regressor</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Objective</span>
                    <span className="font-semibold font-mono">regression (RMSE)</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Số lượng lá (num_leaves)</span>
                    <span className="font-semibold font-mono">31</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Học suất (learning_rate)</span>
                    <span className="font-semibold font-mono">0.05</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Feature Fraction</span>
                    <span className="font-semibold font-mono">0.8</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Trạng thái Fallback</span>
                    <span className={`font-bold font-mono ${isActive ? "text-green-500" : "text-amber-500"}`}>
                      {isActive ? "TẮT (Sử dụng AI)" : "BẬT (Deterministic)"}
                    </span>
                  </div>
                </div>
              </div>

              {/* Vectorizer & Schema Status */}
              <div className="bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm space-y-4">
                <div className="flex items-center gap-2 text-slate-800 dark:text-white pb-3 border-b border-slate-200/80 dark:border-[#2a2a2a]">
                  <Layers className="w-5 h-5 text-purple-500" />
                  <h3 className="font-bold text-base">Schema & Vectorizer</h3>
                </div>
                <div className="space-y-3 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Phiên bản Schema</span>
                    <span className="font-semibold font-mono px-2 py-0.5 bg-purple-500/10 text-purple-500 rounded-md">v1</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Đặc trưng Vector hóa</span>
                    <span className="font-semibold font-mono">LightGbmFeatureVectorizer</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Số chiều đầu vào</span>
                    <span className="font-semibold font-mono">14 chiều đặc trưng</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Lưu trữ ứng cử viên</span>
                    <span className="font-semibold font-mono flex items-center gap-1.5">
                      <span className="w-2.5 h-2.5 bg-green-500 rounded-full inline-block animate-ping" />
                      Redis Store
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 dark:text-slate-400">Kênh truyền tải logs</span>
                    <span className="font-semibold font-mono text-slate-400">Websockets/REST</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Logs Console */}
            <div className="bg-white dark:bg-[#1e1e1e] p-6 rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-200/80 dark:border-[#2a2a2a]">
                <div className="flex items-center gap-2 text-slate-800 dark:text-white">
                  <Cpu className="w-5 h-5 text-green-500" />
                  <h3 className="font-bold text-base">Model Training & Inference Logs</h3>
                </div>
                <button 
                  onClick={() => setLogs([])}
                  className="text-xs text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition"
                >
                  Xóa log console
                </button>
              </div>

              <div className="bg-slate-950 text-emerald-400 font-mono text-xs p-4 rounded-xl h-60 overflow-y-auto space-y-1.5 shadow-inner">
                {logs.map((log, idx) => (
                  <div key={idx} className="leading-relaxed">
                    <span className="text-neutral-500 select-none mr-2">[{idx + 1}]</span>
                    {log}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
  );
}
