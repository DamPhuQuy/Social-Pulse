import { motion } from "framer-motion";
import { Database, Lock, Share2, ShieldCheck, ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { PATHS } from "@/shared/constants/paths";
import PublicPageShell from "@/shared/layouts/PublicPageShell";

const updatedAt = "20/05/2026";

const privacySections = [
  {
    icon: Database,
    title: "Dữ liệu chúng tôi thu thập",
    body: "Tên hiển thị, email, nội dung bài đăng, tương tác, ảnh đại diện và các dữ liệu vận hành cần thiết cho đăng nhập, theo dõi và realtime.",
  },
  {
    icon: ShieldCheck,
    title: "Cách chúng tôi sử dụng",
    body: "Dữ liệu được dùng để xác thực, cá nhân hóa bảng tin, hiển thị thông báo, bảo vệ tài khoản và quản trị hệ thống.",
  },
  {
    icon: Share2,
    title: "Chia sẻ dữ liệu",
    body: "Chúng tôi không bán dữ liệu cá nhân. Một số thông tin được hiển thị công khai theo phạm vi bạn chọn khi đăng nội dung hoặc hồ sơ.",
  },
  {
    icon: Lock,
    title: "Lưu trữ và bảo vệ",
    body: "Dữ liệu được bảo vệ bằng các cơ chế truy cập, xác thực và kiểm soát quyền ở mức ứng dụng và backend.",
  },
];

export default function PrivacyPage() {
  return (
    <PublicPageShell
      eyebrow="Quyền riêng tư"
      title={
        <>
          Chính sách bảo mật của <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-cyan-500">SocialPulse</span>
        </>
      }
      description="Mục tiêu của chúng tôi là làm rõ dữ liệu nào được thu thập, vì sao nó được dùng và cách bạn kiểm soát thông tin cá nhân trong ứng dụng."
    >
      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="rounded-[1.75rem] border border-slate-200/70 dark:border-white/10 bg-white/75 dark:bg-slate-900/70 backdrop-blur-xl p-6 lg:p-8"
      >
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-blue-500/10 p-3 text-blue-500">
            <Lock className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.25em] text-slate-400 dark:text-slate-500">Cập nhật lần cuối</p>
            <p className="text-base font-semibold text-slate-900 dark:text-white">{updatedAt}</p>
          </div>
        </div>
        <p className="mt-4 text-sm leading-6 text-slate-600 dark:text-slate-400">
          Bản này mô tả cách SocialPulse xử lý dữ liệu ở mức sản phẩm. Nếu triển khai ngoài môi trường nội bộ, bạn nên bổ sung rà soát pháp lý riêng.
        </p>
      </motion.section>

      <section className="grid gap-4 md:grid-cols-2">
        {privacySections.map((section) => (
          <InfoCard key={section.title} {...section} />
        ))}
      </section>

      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="rounded-[1.75rem] border border-slate-200/70 dark:border-white/10 bg-slate-900 text-white p-8 lg:p-10"
      >
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
          <div className="max-w-2xl space-y-3">
            <p className="text-xs font-bold uppercase tracking-[0.28em] text-cyan-300">Kiểm soát</p>
            <h2 className="text-3xl font-bold tracking-tight">Bạn vẫn có thể quản lý mật khẩu, ảnh và danh sách chặn trong cài đặt.</h2>
            <p className="text-slate-300 leading-relaxed">
              Phần lớn điều khiển riêng tư đã được gom vào hồ sơ và cài đặt tài khoản để người dùng không phải tìm nhiều nơi.
            </p>
          </div>
          <Link to={PATHS.SETTINGS} className="inline-flex items-center gap-2 rounded-full bg-white px-5 py-3 text-sm font-bold text-slate-900 hover:opacity-90 transition-opacity">
            Tới cài đặt <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </motion.section>
    </PublicPageShell>
  );
}

function InfoCard({
  icon: Icon,
  title,
  body,
}: {
  icon: React.FC<{ className?: string }>;
  title: string;
  body: string;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.45 }}
      className="rounded-[1.5rem] border border-slate-200/70 dark:border-white/10 bg-white/75 dark:bg-slate-900/70 backdrop-blur-xl p-6 shadow-[0_10px_40px_rgba(15,23,42,0.06)]"
    >
      <div className="mb-4 inline-flex rounded-2xl bg-blue-500/10 p-3 text-blue-500">
        <Icon className="w-5 h-5" />
      </div>
      <h3 className="text-lg font-bold text-slate-900 dark:text-white">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-400">{body}</p>
    </motion.div>
  );
}
