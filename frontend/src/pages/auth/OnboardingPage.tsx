import React, { useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { Activity, Download, Globe, Zap, Share2, LogIn } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PATHS } from '@/constants/paths';

// --- Social Pulse Interactive Background ---
const InteractiveBackground: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const mouse = useRef({ x: 0, y: 0, lastX: 0, lastY: 0 });
  const ripples = useRef<{ x: number; y: number; r: number; maxR: number; opacity: number }[]>([]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animationFrameId: number;
    let particles: Particle[] = [];

    class Particle {
      x: number;
      y: number;
      originX: number;
      originY: number;
      vx: number;
      vy: number;
      size: number;
      color: string;
      pulseScale: number;

      constructor(w: number, h: number) {
        this.x = Math.random() * w;
        this.y = Math.random() * h;
        this.originX = this.x;
        this.originY = this.y;
        this.vx = (Math.random() - 0.5) * 0.5;
        this.vy = (Math.random() - 0.5) * 0.5;
        this.size = Math.random() * 2 + 1;
        this.pulseScale = 1;

        const colors = ['#3b82f6', '#8b5cf6', '#06b6d4', '#ec4899'];
        this.color = colors[Math.floor(Math.random() * colors.length)];
      }

      draw() {
        if (!ctx) return;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size * this.pulseScale, 0, Math.PI * 2);
        ctx.fillStyle = this.color;
        ctx.globalAlpha = 0.6;
        ctx.fill();
        ctx.globalAlpha = 1;
      }

      update(w: number, h: number) {
        // Natural drift
        this.x += this.vx;
        this.y += this.vy;

        // Bounce off walls
        if (this.x < 0 || this.x > w) this.vx *= -1;
        if (this.y < 0 || this.y > h) this.vy *= -1;

        // Interaction with ripples (The "Pulse" effect)
        this.pulseScale = 1;
        ripples.current.forEach(ripple => {
          const dx = this.x - ripple.x;
          const dy = this.y - ripple.y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          // If particle is on the edge of a ripple
          const edgeWidth = 30;
          if (dist > ripple.r - edgeWidth && dist < ripple.r + edgeWidth) {
            this.pulseScale = 2.0 * (ripple.opacity);
            // Reduced push from ripple for a more subtle effect
            this.x += (dx / dist) * 0.3;
            this.y += (dy / dist) * 0.3;
          }
        });

        // Return to origin faster (Social stability)
        const dxOrig = this.originX - this.x;
        const dyOrig = this.originY - this.y;
        this.x += dxOrig * 0.12; // Significantly increased for ultra-fast recovery
        this.y += dyOrig * 0.12;

        // Mouse attraction/connection
        const dxMouse = mouse.current.x - this.x;
        const dyMouse = mouse.current.y - this.y;
        const distMouse = Math.sqrt(dxMouse * dxMouse + dyMouse * dyMouse);
        if (distMouse < 150) {
          this.pulseScale += (150 - distMouse) / 100;
        }
      }
    }

    const init = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
      particles = [];
      // High-density particle system
      const count = Math.floor((canvas.width * canvas.height) / 6000);
      for (let i = 0; i < count; i++) {
        particles.push(new Particle(canvas.width, canvas.height));
      }
    };

    const drawConnections = () => {
      ctx.lineWidth = 0.7;
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (dist < 120) {
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.strokeStyle = `rgba(59, 130, 246, ${0.2 * (1 - dist / 100)})`;
            ctx.stroke();
          }
        }
      }
    };

    const animate = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      // Update and draw ripples
      ripples.current = ripples.current.filter(r => r.opacity > 0.01);
      ripples.current.forEach(r => {
        r.r += 3; // Reduced from 4 for smaller radius
        r.opacity *= 0.96; // Increased from 0.98 for faster fade
        ctx.beginPath();
        ctx.arc(r.x, r.y, r.r, 0, Math.PI * 2);
        ctx.strokeStyle = `rgba(6, 182, 212, ${r.opacity * 0.3})`;
        ctx.lineWidth = 2;
        ctx.stroke();
      });

      drawConnections();
      particles.forEach(p => {
        p.update(canvas.width, canvas.height);
        p.draw();
      });

      animationFrameId = requestAnimationFrame(animate);
    };

    const handleMouseMove = (e: MouseEvent) => {
      mouse.current.x = e.clientX;
      mouse.current.y = e.clientY;

      // Mouse movement creates ripples ("Social Ripple")
      const distMoved = Math.sqrt(
        Math.pow(mouse.current.x - mouse.current.lastX, 2) +
        Math.pow(mouse.current.y - mouse.current.lastY, 2)
      );

      if (distMoved > 100) {
        ripples.current.push({
          x: e.clientX,
          y: e.clientY,
          r: 0,
          maxR: 200,
          opacity: 1
        });
        mouse.current.lastX = e.clientX;
        mouse.current.lastY = e.clientY;
      }
    };

    const handleClick = (e: MouseEvent) => {
      ripples.current.push({
        x: e.clientX,
        y: e.clientY,
        r: 0,
        maxR: 300,
        opacity: 1
      });
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mousedown', handleClick);
    window.addEventListener('resize', init);
    init();
    animate();

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mousedown', handleClick);
      window.removeEventListener('resize', init);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return <canvas ref={canvasRef} className="absolute inset-0 z-0 pointer-events-none" />;
};

const OnboardingPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen w-full relative overflow-x-hidden font-['Outfit'] bg-white selection:bg-blue-100">
      {/* Social Pulse Background */}
      <InteractiveBackground />

      {/* Header/Nav */}
      <nav className="fixed top-0 left-0 w-full z-50 flex items-center justify-between px-8 py-6 backdrop-blur-md bg-white/40">
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          onClick={() => navigate(PATHS.ONBOARDING)}
          className="flex items-center gap-3 cursor-pointer group"
        >
          <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-purple-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20">
            <Activity className="text-white w-5 h-5" />
          </div>
          <span className="text-xl font-bold tracking-tight text-gray-900">SocialPulse</span>
        </motion.div>

        <div className="hidden md:flex items-center gap-10 text-sm font-medium text-gray-600">
          {['About', 'Terms', 'Privacy', 'Contact'].map((item) => (
            <a 
              key={item} 
              href={`#${item.toLowerCase()}`} 
              onClick={(e) => {
                if (item === 'About') {
                  e.preventDefault();
                  navigate(PATHS.ONBOARDING);
                }
              }}
              className="hover:text-blue-600 transition-colors"
            >
              {item}
            </a>
          ))}
        </div>

        <motion.button
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          onClick={() => navigate(PATHS.REGISTER)}
          className="flex items-center gap-2 px-8 py-2.5 rounded-full bg-blue-600 hover:bg-blue-700 transition-all font-bold text-white text-sm shadow-xl shadow-blue-500/30"
        >
          Join the Pulse <Share2 className="w-4 h-4" />
        </motion.button>
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
            className="inline-flex items-center px-4 py-1.5 rounded-full bg-blue-50 text-blue-600 border border-blue-100 mb-10"
          >
            <span className="text-sm font-bold tracking-wide uppercase">WHERE YOUR NETWORK MOVES</span>
          </motion.div>

          <h1 className="text-6xl md:text-8xl lg:text-[6.5rem] font-bold text-gray-900 leading-[0.95] mb-12 tracking-wide">
            Feel the <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-cyan-500">ripple</span> <br />
            Control the <span className="text-transparent bg-clip-text bg-gradient-to-r from-purple-600 to-pink-500 tracking-wide">pulse</span>
          </h1>

          <p className="text-xl md:text-2xl text-gray-500 max-w-4xl mx-auto mb-12 leading-relaxed">
            Experience a network that adapts to you. Your vibe, perfectly amplified.
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
              onClick={() => navigate('/home')}
              className="px-12 py-5 bg-gray-900 text-white rounded-full font-bold text-xl transition-all shadow-2xl shadow-black/20"
            >
              Start Exploring
            </motion.button>

            <button 
              onClick={() => navigate(PATHS.LOGIN)}
              className="flex items-center gap-2 px-10 py-5 bg-white border border-gray-100 hover:bg-gray-50 text-gray-900 rounded-full font-bold text-xl transition-all shadow-sm group"
            >
              Log in <LogIn className="w-5 h-5 text-blue-600 group-hover:translate-x-1 transition-transform" />
            </button>
          </motion.div>
        </motion.div>
      </main>


      <footer className="fixed bottom-8 w-full flex justify-center z-10 pointer-events-none">
        <p className="text-gray-400 text-[10px] font-bold tracking-[0.3em] uppercase opacity-60">
          © 2026 SocialPulse. All rights reserved.
        </p>
      </footer>

      {/* Decorative Glows */}
      <div className="fixed top-[-10%] left-[-10%] w-[60%] h-[60%] bg-blue-100/30 rounded-full blur-[160px] -z-10" />
      <div className="fixed bottom-[-10%] right-[-10%] w-[60%] h-[60%] bg-purple-100/30 rounded-full blur-[160px] -z-10" />
    </div>
  );
};

export default OnboardingPage;
