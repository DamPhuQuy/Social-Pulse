import React, { useEffect, useRef } from 'react';

export const InteractiveBackground: React.FC = () => {
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
        this.x += this.vx;
        this.y += this.vy;

        if (this.x < 0 || this.x > w) this.vx *= -1;
        if (this.y < 0 || this.y > h) this.vy *= -1;

        this.pulseScale = 1;
        ripples.current.forEach(ripple => {
          const dx = this.x - ripple.x;
          const dy = this.y - ripple.y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          const edgeWidth = 30;
          if (dist > ripple.r - edgeWidth && dist < ripple.r + edgeWidth) {
            this.pulseScale = 2.0 * (ripple.opacity);
            this.x += (dx / dist) * 0.3;
            this.y += (dy / dist) * 0.3;
          }
        });

        const dxOrig = this.originX - this.x;
        const dyOrig = this.originY - this.y;
        this.x += dxOrig * 0.12;
        this.y += dyOrig * 0.12;

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
      ripples.current = ripples.current.filter(r => r.opacity > 0.01);
      ripples.current.forEach(r => {
        r.r += 3;
        r.opacity *= 0.96;
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
      const distMoved = Math.sqrt(
        Math.pow(mouse.current.x - mouse.current.lastX, 2) +
        Math.pow(mouse.current.y - mouse.current.lastY, 2)
      );
      if (distMoved > 100) {
        ripples.current.push({ x: e.clientX, y: e.clientY, r: 0, maxR: 200, opacity: 1 });
        mouse.current.lastX = e.clientX;
        mouse.current.lastY = e.clientY;
      }
    };

    const handleClick = (e: MouseEvent) => {
      ripples.current.push({ x: e.clientX, y: e.clientY, r: 0, maxR: 300, opacity: 1 });
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
