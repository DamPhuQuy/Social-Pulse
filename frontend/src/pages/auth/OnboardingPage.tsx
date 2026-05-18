import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Activity, Share2, LogIn, Sun, Moon } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PATHS } from '@/constants/paths';
import { InteractiveBackground } from '@/components/auth/InteractiveBackground';

const OnboardingPage: React.FC = () => {
  const navigate = useNavigate();

  const [isDark, setIsDark] = useState(() => {
    return localStorage.getItem('theme') === 'dark' || 
      (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches);
  });

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
  }, [isDark]);

  return (
    <div className="min-h-screen w-full relative overflow-x-hidden font-sans bg-white dark:bg-slate-950 transition-colors duration-500 selection:bg-blue-100">
      {/* Social Pulse Background */}
      <InteractiveBackground />

      {/* Header/Nav */}
      <nav className="fixed top-0 left-0 w-full z-50 flex items-center justify-between px-8 py-6 bg-transparent">

        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          onClick={() => navigate(PATHS.ONBOARDING)}
          className="flex items-center gap-3 cursor-pointer group"
        >
          <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-cyan-500 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 group-hover:scale-110 transition-transform">
            <Activity className="text-white w-5 h-5" />
          </div>
          <span className="text-xl font-bold tracking-tight text-gray-900 dark:text-white">SocialPulse</span>
        </motion.div>

        <div className="hidden md:flex absolute left-1/2 -translate-x-1/2 items-center gap-10 text-sm font-medium text-gray-600 dark:text-slate-400">
          {[
            { id: 'about', label: 'Giới thiệu' },
            { id: 'terms', label: 'Điều khoản' },
            { id: 'privacy', label: 'Bảo mật' },
            { id: 'contact', label: 'Liên hệ' }
          ].map((item) => (
            <a
              key={item.id}
              href={`#${item.id}`}
              onClick={(e) => {
                if (item.id === 'about') {
                  e.preventDefault();
                  navigate(PATHS.ONBOARDING);
                }
              }}
              className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
            >
              {item.label}
            </a>
          ))}
        </div>

        <div className="flex items-center gap-4">
          <motion.button 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            onClick={() => setIsDark(!isDark)}
            className="p-2.5 rounded-xl bg-white/50 dark:bg-slate-900/50 text-gray-500 dark:text-slate-400 hover:text-blue-600 dark:hover:text-blue-400 transition-all border border-slate-300 dark:border-slate-800"
          >
            {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </motion.button>
          
          <motion.button
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            onClick={() => navigate(PATHS.REGISTER)}
            className="flex items-center gap-2 px-8 py-2.5 rounded-full bg-blue-600 hover:bg-blue-700 transition-all font-bold text-white text-sm shadow-xl shadow-blue-500/30"
          >
            Tham gia ngay <Share2 className="w-4 h-4" />
          </motion.button>
        </div>
      </nav>

      {/* Hero Section */}
      <main className="relative z-10 min-h-screen flex flex-col items-center justify-center px-4 text-center">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, ease: "easeOut" }}
          className="max-w-5xl mx-auto"
        >
          {/* Pulsing Badge */}
          <motion.div
            animate={{ scale: [1, 1.1, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
            className="inline-flex items-center px-4 py-1.5 rounded-full bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 border border-blue-100 dark:border-blue-900/30 mb-10"
          >
            <span className="text-sm font-bold tracking-wide uppercase">NƠI KẾT NỐI MỌI NHỊP ĐẬP</span>
          </motion.div>

          <h1 className="text-5xl md:text-7xl lg:text-[5.5rem] font-bold text-gray-900 dark:text-white leading-[1.1] mb-12 tracking-tight">
            Cảm nhận sự <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-cyan-500 whitespace-nowrap">lan tỏa</span> <br className="hidden md:block" />
            Kiểm soát <span className="text-transparent bg-clip-text bg-gradient-to-r from-purple-600 to-pink-500 tracking-wide whitespace-nowrap">nhịp đập</span>
          </h1>

          <p className="text-xl md:text-2xl text-gray-500 dark:text-slate-400 max-w-4xl mx-auto mb-12 leading-relaxed">
            Trải nghiệm một mạng lưới thích ứng với bạn. Cảm xúc của bạn, được khuếch đại hoàn hảo.
          </p>

          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.5, duration: 1 }}
            className="flex flex-col sm:flex-row items-center justify-center gap-6"
          >
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => navigate(PATHS.HOME)}
              className="px-12 py-5 bg-gray-900 dark:bg-white text-white dark:text-slate-900 rounded-full font-bold text-xl transition-all shadow-2xl shadow-black/20"
            >
              Khám phá ngay
            </motion.button>

            <button
              onClick={() => navigate(PATHS.LOGIN)}
              className="flex items-center gap-2 px-10 py-5 bg-white dark:bg-slate-900 border border-slate-300 dark:border-slate-800 hover:bg-gray-50 dark:hover:bg-slate-800 text-gray-900 dark:text-white rounded-full font-bold text-xl transition-all shadow-sm group"
            >
              Đăng nhập <LogIn className="w-5 h-5 text-blue-600 group-hover:translate-x-1 transition-transform" />
            </button>
          </motion.div>
        </motion.div>
      </main>


      <footer className="fixed bottom-8 w-full flex justify-center z-10 pointer-events-none">
        <p className="text-gray-400 dark:text-slate-600 text-[10px] font-bold tracking-[0.3em] uppercase opacity-60">
          © 2026 SocialPulse. Bảo lưu mọi quyền.
        </p>
      </footer>

      {/* Decorative Glows */}
      <div className="fixed top-[-10%] left-[-10%] w-[60%] h-[60%] bg-blue-100/30 dark:bg-blue-900/10 rounded-full blur-[160px] -z-10" />
      <div className="fixed bottom-[-10%] right-[-10%] w-[60%] h-[60%] bg-purple-100/30 dark:bg-purple-900/10 rounded-full blur-[160px] -z-10" />
    </div>
  );
};

export default OnboardingPage;
