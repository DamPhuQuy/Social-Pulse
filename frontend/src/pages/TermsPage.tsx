import { motion } from "framer-motion";
import { ShieldCheck, FileText, Users, Scale, ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { PATHS } from "@/constants/paths";
import PublicPageShell from "@/components/public/PublicPageShell";

const updatedAt = "20/05/2026";

const termsSections = [
  {
    icon: Users,
    title: "Tài khoản và trách nhiệm",
    body: "Bạn chịu trách nhiệm về thông tin đăng ký, hành vi đăng nhập và nội dung bạn đăng tải lên hệ thống.",
  },
  {
    icon: FileText,
    title: "Nội dung người dùng",
    body: "Bạn chỉ nên đăng nội dung mà mình có quyền sử dụng. SocialPulse có thể ẩn hoặc gỡ nội dung vi phạm quy định.",
  },
  {
    icon: Scale,
    title: "Sử dụng hợp lệ",
    body: "Không được lạm dụng hệ thống để spam, quấy rối, phá hoại, khai thác trái phép hoặc cố tình vượt qua kiểm soát.",
  },
  {
    icon: ShieldCheck,
    title: "Thực thi và khóa tài khoản",
    body: "Tài khoản có thể bị giới hạn, khóa hoặc xóa nếu vi phạm nghiêm trọng, theo dõi liên tục hoặc gây rủi ro cho cộng đồng.",
  },
];

export default function TermsPage() {
  return (
    <PublicPageShell
      eyebrow="Điều khoản sử dụng"
      title={
        <>
          Điều khoản dịch vụ <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-cyan-500">SocialPulse</span>
        </>
      }
      description="Các điều khoản dưới đây mô tả cách bạn sử dụng sản phẩm, quyền và trách nhiệm của từng bên, cùng cách chúng tôi duy trì một môi trường rõ ràng và an toàn."
    >
      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="rounded-[1.75rem] border border-slate-200/70 dark:border-white/10 bg-white/75 dark:bg-slate-900/70 backdrop-blur-xl p-6 lg:p-8"
      >
        <div className="flex items-center gap-3">
          <div className="rounded-xl bg-blue-500/10 p-3 text-blue-500">
            <FileText className="w-5 h-5" />
          </div>
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.25em] text-slate-400 dark:text-slate-500">Cập nhật lần cuối</p>
            <p className="text-base font-semibold text-slate-900 dark:text-white">{updatedAt}</p>
          </div>
        </div>
        <p className="mt-4 text-sm leading-6 text-slate-600 dark:text-slate-400">
          Đây là bản mô tả sản phẩm cho dự án hiện tại. Nó phản ánh cách hệ thống đang vận hành trên frontend và backend, không thay thế tư vấn pháp lý chính thức.
        </p>
      </motion.section>

      <section className="grid gap-4 md:grid-cols-2">
        {termsSections.map((section) => (
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
            <p className="text-xs font-bold uppercase tracking-[0.28em] text-cyan-300">Tiếp theo</p>
            <h2 className="text-3xl font-bold tracking-tight">Xem cách chúng tôi xử lý dữ liệu cá nhân.</h2>
            <p className="text-slate-300 leading-relaxed">
              Nếu bạn quan tâm đến dữ liệu được lưu trữ và luân chuyển trong hệ thống, hãy đọc chính sách bảo mật để nắm đầy đủ bức tranh.
            </p>
          </div>
          <Link to={PATHS.PRIVACY} className="inline-flex items-center gap-2 rounded-full bg-white px-5 py-3 text-sm font-bold text-slate-900 hover:opacity-90 transition-opacity">
            Chính sách bảo mật <ArrowRight className="w-4 h-4" />
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
