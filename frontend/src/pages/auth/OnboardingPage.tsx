import Header from "@/components/Header";
import { PATHS } from "@/constants/paths";
import { ArrowRight02Icon, SparklesIcon, MagicWand01Icon, ChartHistogramIcon, ChatBotIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { Link } from "react-router-dom";

/**
 * The landing / home page of Social Pulse.
 * Redesigned with heavy typography, monochrome colors, a single accent color pop,
 * and realistic UI widgets in the bento layout.
 */
export default function OnboardingPage() {
  return (
    <div className="bg-surface font-body text-on-surface min-h-screen flex flex-col selection:bg-primary/20 selection:text-primary">
      <Header isHomePage={true} />

      <main className="grow flex items-center justify-center pt-32 pb-16 px-6 lg:px-12 overflow-hidden">
        <div className="max-w-[1400px] w-full grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16 items-center">
          
          {/* Left — Heavy Typography Hero */}
          <div className="lg:col-span-6 flex flex-col space-y-10 z-10">
            <div>
              {/* Pill Tag */}
              <div className="inline-flex items-center gap-2 bg-surface-container-low px-4 py-1.5 rounded-full border border-outline-variant text-sm font-bold text-on-surface mb-8 shadow-sm">
                <HugeiconsIcon icon={SparklesIcon} className="size-4 text-primary" />
                <span>AI-POWERED PLATFORM</span>
              </div>

              {/* Massive Headline */}
              <h1 className="font-headline text-6xl md:text-8xl lg:text-[5.5rem] font-black tracking-tighter text-on-surface leading-[0.95]">
                Find your <br />
                community <br/>
                pulse.
              </h1>

              <p className="text-xl md:text-2xl text-on-surface-variant max-w-lg leading-relaxed font-medium mt-8">
                Experience a social platform designed for friendly,
                meaningful interaction with people who share your interests.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-6">
              {/* Offset Hard Shadow Button */}
              <Link
                to={PATHS.REGISTER}
                className="group flex items-center gap-2 bg-on-surface text-surface px-8 py-4 rounded-full font-bold text-lg transition-transform duration-200 border-2 border-on-surface hover:-translate-y-1 hover:-translate-x-1 hover:shadow-[6px_6px_0px_#0052FF]"
              >
                Get Started
                <HugeiconsIcon icon={ArrowRight02Icon} strokeWidth={2.5} className="size-5" />
              </Link>

              <Link
                to={PATHS.LEARN_MORE}
                className="px-8 py-4 rounded-full font-bold text-lg text-on-surface hover:bg-surface-container transition-colors"
              >
                Learn more
              </Link>
            </div>
          </div>

          {/* Right — Realistic UI Bento Grid (Monochrome + Pop of Color) */}
          <div className="lg:col-span-6 grid grid-cols-2 grid-rows-2 gap-4 lg:gap-6 h-auto lg:h-[600px] z-10 perspective-1000">
            
            {/* Box 1: Chat UI (Real Component Feel) */}
            <div className="col-span-2 row-span-1 bg-surface-container-lowest border-2 border-outline-variant/60 rounded-3xl p-6 shadow-sm flex flex-col gap-4">
              <div className="flex items-center justify-between border-b border-outline-variant/40 pb-4">
                <div className="flex items-center gap-3">
                  <div className="size-10 rounded-full bg-surface-container flex items-center justify-center border border-outline-variant font-bold text-xl text-primary">
                    #
                  </div>
                  <div>
                    <h3 className="font-bold text-on-surface leading-tight">NextJS Masters</h3>
                    <p className="text-xs text-on-surface-variant font-medium">842 online</p>
                  </div>
                </div>
                <HugeiconsIcon icon={ChatBotIcon} className="size-6 text-outline" />
              </div>
              
              <div className="flex-1 flex flex-col justify-end gap-3">
                {/* Chat Bubble 1 */}
                <div className="self-start bg-surface-container px-4 py-3 rounded-2xl rounded-tl-sm text-sm font-medium text-on-surface border border-outline-variant/40 max-w-[80%]">
                  Anyone tried the new React compiler yet?
                </div>
                {/* Chat Bubble 2 (User/Accent) */}
                <div className="self-end bg-on-surface text-surface px-4 py-3 rounded-2xl rounded-tr-sm text-sm font-medium max-w-[80%] relative">
                  Yes, it's incredibly fast! ⚡
                  {/* Subtle accent dot */}
                  <div className="absolute -bottom-1 -left-2 size-3 bg-primary rounded-full border-2 border-surface" />
                </div>
              </div>
            </div>

            {/* Box 2: Leaderboard Snippet */}
            <div className="col-span-1 row-span-1 bg-surface-container-lowest border-2 border-outline-variant/60 rounded-3xl p-6 shadow-sm flex flex-col">
              <h3 className="font-bold text-on-surface mb-4 flex items-center gap-2">
                <HugeiconsIcon icon={MagicWand01Icon} className="size-5 text-primary" />
                Top Contributors
              </h3>
              
              <div className="flex flex-col gap-3">
                {[
                  { name: "Alex Chen", score: "9,240" },
                  { name: "Sarah J.", score: "8,105", highlight: true },
                  { name: "Mike T.", score: "7,490" }
                ].map((user, idx) => (
                  <div key={idx} className="flex items-center justify-between bg-surface-container p-3 rounded-xl border border-outline-variant/30">
                    <div className="flex items-center gap-3">
                      <div className="size-8 rounded-full bg-outline-variant/20 flex items-center justify-center font-bold text-xs bg-gray-200">
                         {idx + 1}
                      </div>
                      <span className={`text-sm font-bold ${user.highlight ? "text-primary" : "text-on-surface"}`}>
                        {user.name}
                      </span>
                    </div>
                    <span className="text-xs font-bold text-on-surface-variant tracking-tight">{user.score}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Box 3: AI Interest Graph */}
            <div className="col-span-1 row-span-1 bg-on-surface rounded-3xl p-6 shadow-lg flex flex-col relative overflow-hidden">
               <h3 className="font-bold text-surface mb-2 relative z-10 flex items-center gap-2">
                 <HugeiconsIcon icon={ChartHistogramIcon} className="size-5 text-primary" />
                 Pulse Analytics
               </h3>
               <p className="text-surface/60 text-xs font-medium z-10">AI-matched community interests</p>
               
               {/* Abstract chart visualization */}
               <div className="absolute bottom-4 left-4 right-4 h-24 flex items-end justify-between gap-2 z-10">
                  <div className="w-1/6 bg-surface/20 rounded-t-sm h-[40%]" />
                  <div className="w-1/6 bg-surface/20 rounded-t-sm h-[60%]" />
                  <div className="w-1/6 bg-primary rounded-t-sm h-[90%] relative">
                     {/* Accent spark */}
                     <div className="absolute -top-3 left-1/2 -translate-x-1/2 size-2 bg-surface rounded-full" />
                  </div>
                  <div className="w-1/6 bg-surface/20 rounded-t-sm h-[70%]" />
                  <div className="w-1/6 bg-surface/20 rounded-t-sm h-[50%]" />
               </div>
            </div>
          </div>
        </div>
      </main>

      <footer className="mt-auto py-8 px-6 lg:px-12 border-t border-outline-variant/40 bg-surface z-10">
        <div className="max-w-[1400px] mx-auto flex flex-col sm:flex-row justify-between items-center gap-4">
          <div className="flex gap-8 text-sm font-bold text-on-surface-variant">
            <Link to={PATHS.PRIVACY} className="hover:text-primary transition-colors">
              Privacy Policy
            </Link>
            <Link to={PATHS.TERMS} className="hover:text-primary transition-colors">
              Terms
            </Link>
          </div>

          <span className="text-sm font-bold text-on-surface-variant/60">
            &copy; {new Date().getFullYear()} Social Pulse.
          </span>
        </div>
      </footer>
    </div>
  );
}
