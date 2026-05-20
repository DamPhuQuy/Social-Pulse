import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { Activity, LogIn, Moon, Sun } from "lucide-react";
import { PATHS } from "@/constants/paths";
import { InteractiveBackground } from "@/components/auth/InteractiveBackground";

type PublicNavLink = {
  label: string;
  to: string;
};

type PublicPageShellProps = {
  eyebrow: string;
  title: React.ReactNode;
  description: string;
  navLinks?: PublicNavLink[];
  children: React.ReactNode;
};

export default function PublicPageShell({
  eyebrow,
  title,
  description,
  navLinks = [
    { label: "Tính năng", to: `${PATHS.LEARN_MORE}#features` },
    { label: "Cộng đồng", to: `${PATHS.LEARN_MORE}#community` },
    { label: "Bảng giá", to: `${PATHS.LEARN_MORE}#pricing` },
  ],
  children,
}: PublicPageShellProps) {
  const [isDark, setIsDark] = useState(() => {
    if (typeof window === "undefined") {
      return false;
    }

    return localStorage.getItem("theme") === "dark" ||
      (!("theme" in localStorage) && window.matchMedia("(prefers-color-scheme: dark)").matches);
  });

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add("dark");
      localStorage.setItem("theme", "dark");
    } else {
      document.documentElement.classList.remove("dark");
      localStorage.setItem("theme", "light");
    }
  }, [isDark]);

  return (
    <div className="min-h-screen w-full relative overflow-hidden font-sans bg-white dark:bg-slate-950 transition-colors duration-500">
      <InteractiveBackground />

      <div className="fixed top-[-10%] left-[-10%] w-[60%] h-[60%] bg-blue-100/30 dark:bg-blue-900/10 rounded-full blur-[160px] -z-10" />
      <div className="fixed bottom-[-10%] right-[-10%] w-[60%] h-[60%] bg-purple-100/30 dark:bg-purple-900/10 rounded-full blur-[160px] -z-10" />

      <nav className="fixed top-0 left-0 w-full z-50 px-6 lg:px-8 py-5">
        <div className="mx-auto flex max-w-7xl items-center justify-between rounded-[1.75rem] border border-white/40 dark:border-white/10 bg-white/75 dark:bg-slate-900/70 backdrop-blur-2xl px-5 py-3 shadow-[0_10px_40px_rgba(15,23,42,0.08)] dark:shadow-[0_10px_40px_rgba(0,0,0,0.35)]">
          <Link to={PATHS.ONBOARDING} className="flex items-center gap-3 group">
            <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-cyan-500 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 group-hover:scale-110 transition-transform">
              <Activity className="text-white w-5 h-5" />
            </div>
            <span className="text-xl font-bold tracking-tight text-gray-900 dark:text-white">SocialPulse</span>
          </Link>

          <div className="hidden lg:flex items-center gap-8 text-sm font-medium text-gray-600 dark:text-slate-400">
            {navLinks.map((link) => (
              <Link key={link.to} to={link.to} className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
                {link.label}
              </Link>
            ))}
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsDark((value) => !value)}
              className="p-2.5 rounded-xl bg-white/60 dark:bg-slate-900/60 text-gray-500 dark:text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 transition-all border border-slate-200/80 dark:border-slate-800"
              aria-label="Toggle theme"
            >
              {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>

            <Link
              to={PATHS.LOGIN}
              className="hidden sm:inline-flex items-center gap-2 px-5 py-2.5 rounded-full bg-slate-900 dark:bg-white text-white dark:text-slate-900 font-bold text-sm hover:opacity-90 transition-opacity"
            >
              Đăng nhập <LogIn className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </nav>

      <main className="relative z-10 mx-auto max-w-7xl px-6 lg:px-8 pt-28 pb-16">
        <motion.section
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, ease: "easeOut" }}
          className="rounded-[2rem] border border-slate-200/70 dark:border-white/10 bg-white/75 dark:bg-slate-900/70 backdrop-blur-2xl p-8 lg:p-12 shadow-[0_20px_60px_rgba(15,23,42,0.08)] dark:shadow-[0_20px_60px_rgba(0,0,0,0.35)]"
        >
          <div className="max-w-4xl">
            <span className="inline-flex items-center px-4 py-1.5 rounded-full bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 border border-blue-100 dark:border-blue-900/30 text-xs font-bold tracking-[0.25em] uppercase">
              {eyebrow}
            </span>
            <h1 className="mt-6 text-4xl md:text-6xl font-bold tracking-tight leading-[1.05] text-gray-900 dark:text-white">
              {title}
            </h1>
            <p className="mt-6 max-w-3xl text-lg md:text-xl leading-relaxed text-gray-600 dark:text-slate-400">
              {description}
            </p>
          </div>
        </motion.section>

        <div className="mt-8 space-y-8">
          {children}
        </div>
      </main>
    </div>
  );
}
