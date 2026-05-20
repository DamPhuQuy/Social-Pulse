import { motion } from "framer-motion";
import { ArrowRight, ShieldCheck, Sparkles, Users, Zap, MessageSquare, ChartNoAxesCombined, LockKeyhole, BadgeCheck } from "lucide-react";
import { Link } from "react-router-dom";
import { PATHS } from "@/constants/paths";
import PublicPageShell from "@/components/public/PublicPageShell";

const featureCards = [
  {
    icon: Zap,
    title: "Bảng tin thích ứng",
    body: "Feed ranking ưu tiên nội dung phù hợp theo tín hiệu tương tác, thời điểm và chủ đề bạn theo dõi.",
  },
  {
    icon: MessageSquare,
    title: "Tương tác tức thì",
    body: "Pulse, bình luận, bookmark và thông báo realtime được gom vào một trải nghiệm mạch lạc.",
  },
  {
    icon: ShieldCheck,
    title: "An toàn mặc định",
    body: "Cơ chế báo cáo, moderation và chặn người dùng giúp giữ môi trường sạch ngay từ UI.",
  },
];

const communityCards = [
  {
    icon: Users,
    title: "Kết nối có kiểm soát",
    body: "Theo dõi, bỏ theo dõi, danh sách followers/following và xem hồ sơ đều nằm trong một luồng rõ ràng.",
  },
  {
    icon: LockKeyhole,
    title: "Quyền riêng tư rõ ràng",
    body: "Chế độ đăng PUBLIC, FRIENDS_ONLY và PRIVATE cho phép người dùng chủ động kiểm soát phạm vi hiển thị.",
  },
  {
    icon: BadgeCheck,
    title: "Quản trị có trách nhiệm",
    body: "Khu vực admin tập trung vào báo cáo, RBAC và giám sát AI thay vì các thao tác mơ hồ.",
  },
];

const pricingCards = [
  {
    icon: BadgeCheck,
    title: "Miễn phí trong giai đoạn hiện tại",
    body: "SocialPulse đang mở theo mô hình thử nghiệm nội bộ, chưa có tầng thanh toán hay gói trả phí.",
  },
  {
    icon: ShieldCheck,
    title: "Không có phí ẩn",
    body: "Trải nghiệm lõi gồm đăng ký, kết nối, tìm kiếm và tương tác đều được triển khai trực tiếp trong app.",
  },
  {
    icon: ChartNoAxesCombined,
    title: "Sẵn sàng mở rộng",
    body: "Cấu trúc UI đã chia rõ auth, social, admin và realtime để dễ thêm gói dịch vụ sau này.",
  },
];

export default function LearnMorePage() {
  return (
    <PublicPageShell
      eyebrow="Tổng quan sản phẩm"
      title={
        <>
          SocialPulse được thiết kế để <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-cyan-500">kết nối</span> nhanh,
          <br className="hidden md:block" /> rõ và có kiểm soát.
        </>
      }
      description="Một mạng xã hội tối giản về mặt thao tác nhưng có chiều sâu ở tương tác, realtime và kiểm duyệt. Từ bảng tin đến admin, mọi thứ đều được giữ trong một ngôn ngữ UI thống nhất."
    >
      <Section id="features" title="Tính năng chính" icon={Sparkles}>
        <div className="grid gap-4 md:grid-cols-3">
          {featureCards.map((card) => (
            <InfoCard key={card.title} {...card} />
          ))}
        </div>
      </Section>

      <Section id="community" title="Cộng đồng và an toàn" icon={ShieldCheck}>
        <div className="grid gap-4 md:grid-cols-3">
          {communityCards.map((card) => (
            <InfoCard key={card.title} {...card} />
          ))}
        </div>
      </Section>

      <Section id="pricing" title="Bảng giá hiện tại" icon={ChartNoAxesCombined}>
        <div className="grid gap-4 md:grid-cols-3">
          {pricingCards.map((card) => (
            <InfoCard key={card.title} {...card} />
          ))}
        </div>
      </Section>

      <motion.section
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15, duration: 0.5 }}
        className="rounded-[1.75rem] border border-slate-200/70 dark:border-white/10 bg-slate-900 text-white p-8 lg:p-10 shadow-[0_16px_50px_rgba(15,23,42,0.18)]"
      >
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
          <div className="max-w-2xl space-y-3">
            <p className="text-xs font-bold uppercase tracking-[0.28em] text-cyan-300">Bắt đầu</p>
            <h2 className="text-3xl font-bold tracking-tight">Mở tài khoản và thử toàn bộ luồng sản phẩm.</h2>
            <p className="text-slate-300 leading-relaxed">
              Nếu bạn chỉ muốn xem cách SocialPulse hoạt động, đăng nhập để trải nghiệm feed, discovery, profile và admin theo đúng vai trò của mình.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link to={PATHS.REGISTER} className="inline-flex items-center gap-2 rounded-full bg-white px-5 py-3 text-sm font-bold text-slate-900 hover:opacity-90 transition-opacity">
              Tạo tài khoản <ArrowRight className="w-4 h-4" />
            </Link>
            <Link to={PATHS.LOGIN} className="inline-flex items-center gap-2 rounded-full border border-white/20 px-5 py-3 text-sm font-bold text-white hover:bg-white/10 transition-colors">
              Đăng nhập
            </Link>
          </div>
        </div>
      </motion.section>
    </PublicPageShell>
  );
}

function Section({
  id,
  title,
  icon: Icon,
  children,
}: {
  id: string;
  title: string;
  icon: React.FC<{ className?: string }>;
  children: React.ReactNode;
}) {
  return (
    <motion.section
      id={id}
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="rounded-[1.75rem] border border-slate-200/70 dark:border-white/10 bg-white/75 dark:bg-slate-900/70 backdrop-blur-xl p-6 lg:p-8 shadow-[0_10px_40px_rgba(15,23,42,0.06)] dark:shadow-[0_10px_40px_rgba(0,0,0,0.2)]"
    >
      <div className="mb-5 flex items-center gap-3">
        <div className="p-2.5 rounded-xl bg-blue-500/10 text-blue-500">
          <Icon className="w-5 h-5" />
        </div>
        <h2 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">{title}</h2>
      </div>
      {children}
    </motion.section>
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
    <div className="rounded-3xl border border-slate-200/70 dark:border-slate-800 bg-white/90 dark:bg-slate-950/40 p-5 shadow-sm">
      <div className="mb-4 inline-flex rounded-2xl bg-blue-500/10 p-3 text-blue-500">
        <Icon className="w-5 h-5" />
      </div>
      <h3 className="text-lg font-bold text-slate-900 dark:text-white">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-600 dark:text-slate-400">{body}</p>
    </div>
  );
}
