import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  Home,
  Compass,
  BarChart3,
  Settings,
  Search,
  MoreHorizontal,
  MessageCircle,
  Heart,
  Share2,
  Bookmark,
  LogOut,
  Mic,
  Smile,
  Image as ImageIcon,
  Video,
  BarChart as PollIcon,
  Activity,
  Zap,
  TrendingUp,
  Moon,
  Sun,
  Plus,
  ArrowUpRight,
  UserPlus
} from "lucide-react";

// --- Dummy Data ---
const FEED_POSTS = [
  {
    id: 1,
    author: "Alex Rivera",
    username: "@arivera",
    time: "2h",
    content: "Just pushed the new design system update. We focused heavily on typography scale and reducing cognitive load. The results are incredibly clean. 🎨✨",
    likes: "1.2k",
    comments: 48,
    image: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&q=80&w=1200",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Alex"
  },
  {
    id: 2,
    author: "Elena Smith",
    username: "@elenacodes",
    time: "5h",
    content: "Optimization isn't just about speed; it's about making the complex feel simple. Spent the morning refactoring our core rendering engine and it feels so much smoother.",
    likes: "856",
    comments: 12,
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Elena"
  },
  {
    id: 3,
    author: "Design Hub",
    username: "@designhub",
    time: "1d",
    content: "Minimalism is not a lack of something. It's simply the perfect amount of something. Let the content breathe.",
    likes: "2.4k",
    comments: 156,
    image: "https://images.unsplash.com/photo-1558591710-4b4a1ae0f04d?auto=format&fit=crop&q=80&w=1200",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Design"
  }
];

export default function HomePage() {
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
    <div className="bg-[#f8f9fa] dark:bg-black min-h-screen font-['Outfit'] text-slate-900 dark:text-neutral-100 transition-colors duration-300 selection:bg-blue-100">

      {/* --- TOP NAV --- */}
      <header className="fixed top-0 left-0 w-full h-16 bg-white dark:bg-black/80 backdrop-blur-xl border-b border-slate-300 dark:border-neutral-800 z-50 flex items-center justify-between px-6">
        <div className="flex items-center gap-8">
          <div className="flex items-center gap-3 cursor-pointer group">
            <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-cyan-500 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 group-hover:scale-110 transition-transform">
              <Activity className="text-white w-5 h-5" />
            </div>
            <span className="text-xl font-bold tracking-tight text-gray-900 dark:text-white hidden md:block">SocialPulse</span>
          </div>
          <div className="relative w-64 md:w-[400px]">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 dark:text-neutral-500" />
            <input
              type="text"
              placeholder="Search..."
              className="w-full bg-gray-100 dark:bg-neutral-900 border-none rounded-full py-2 pl-12 pr-4 text-sm focus:ring-2 focus:ring-blue-500/20 focus:bg-white dark:focus:bg-black outline-none transition-all dark:text-white dark:placeholder-neutral-500"
            />
          </div>
        </div>

        <div className="flex items-center gap-4">
          <button
            onClick={() => setIsDark(!isDark)}
            className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-neutral-900 text-gray-500 dark:text-neutral-400 transition-all"
          >
            {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>
          <button className="hidden sm:flex items-center gap-2 px-5 py-2 bg-slate-900 dark:bg-white hover:bg-slate-800 dark:hover:bg-neutral-200 text-white dark:text-black rounded-full text-sm font-semibold transition-all">
            <Plus className="w-4 h-4" /> New Post
          </button>
          <div className="w-9 h-9 rounded-full bg-gray-200 dark:bg-neutral-800 overflow-hidden cursor-pointer border border-slate-300 dark:border-neutral-700">
            <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="user" />
          </div>
        </div>
      </header>

      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr_320px] xl:grid-cols-[280px_1fr_350px] gap-8 pt-24 px-6 lg:px-10">

        {/* --- LEFT SIDEBAR --- */}
        <aside className="hidden lg:flex flex-col gap-8 sticky top-24 h-[calc(100vh-120px)]">
          <nav className="flex flex-col gap-1">
            <NavItem icon={Home} label="Home" active />
            <NavItem icon={Compass} label="Explore" />
            <NavItem icon={Zap} label="Notifications" />
            <NavItem icon={Bookmark} label="Bookmarks" />
            <NavItem icon={Settings} label="Settings" />
          </nav>

          <div className="pt-6 border-t border-slate-300 dark:border-neutral-800">
            <h4 className="text-xs font-semibold text-gray-500 dark:text-neutral-500 mb-4 px-3">Communities</h4>
            <div className="flex flex-col gap-2">
              <CommunityItem name="Design Systems" members="12k" />
              <CommunityItem name="Frontend Devs" members="8.4k" />
              <CommunityItem name="UI/UX Inspiration" members="5.1k" />
            </div>
          </div>

          <div className="mt-auto">
            <button className="flex items-center gap-3 px-3 py-2.5 text-sm font-medium text-gray-500 dark:text-neutral-400 hover:text-gray-900 dark:hover:text-white transition-colors w-full rounded-xl hover:bg-gray-100 dark:hover:bg-neutral-900">
              <LogOut className="w-5 h-5" /> Logout
            </button>
          </div>
        </aside>

        {/* --- MIDDLE FEED --- */}
        <main className="flex flex-col gap-6">

          {/* Functional Composer */}
          <section className="bg-white dark:bg-black rounded-2xl border border-slate-300 dark:border-neutral-800 p-4 shadow-sm">
            <div className="flex gap-4">
              <div className="w-10 h-10 rounded-full bg-gray-200 dark:bg-neutral-800 overflow-hidden shrink-0">
                <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="user" />
              </div>
              <div className="flex-1">
                <textarea
                  placeholder="What's happening?"
                  className="w-full bg-transparent border-none p-2 text-lg focus:ring-0 resize-none h-14 placeholder:text-gray-400 dark:placeholder:text-neutral-600 text-slate-900 dark:text-white outline-none"
                />
                <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-neutral-800 mt-2">
                  <div className="flex gap-1">
                    <ActionBtn icon={ImageIcon} />
                    <ActionBtn icon={Video} />
                    <ActionBtn icon={PollIcon} />
                    <ActionBtn icon={Smile} />
                  </div>
                  <button className="px-5 py-1.5 bg-blue-600 text-white rounded-full text-sm font-semibold hover:bg-blue-700 transition-colors">
                    Post
                  </button>
                </div>
              </div>
            </div>
          </section>

          {/* Feed Items */}
          <div className="flex flex-col gap-4">
            {FEED_POSTS.map(post => (
              <SocialPost key={post.id} post={post} />
            ))}
          </div>
        </main>

        {/* --- RIGHT SIDEBAR --- */}
        <aside className="hidden lg:flex flex-col gap-6 sticky top-24 h-[calc(100vh-120px)]">

          {/* Trends */}
          <section className="bg-white dark:bg-black rounded-2xl border border-slate-300 dark:border-neutral-800 shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100 dark:border-neutral-800">
              <h3 className="font-bold text-gray-900 dark:text-white">Trends for you</h3>
            </div>
            <div className="flex flex-col">
              <TrendRow tag="#DesignSystems" posts="12.4K" />
              <TrendRow tag="#ReactJS" posts="8.2K" />
              <TrendRow tag="#Minimalism" posts="5.1K" />
              <TrendRow tag="#TechNews" posts="2.8K" />
            </div>
            <button className="w-full px-5 py-3 text-left text-sm text-blue-600 hover:bg-gray-50 dark:hover:bg-neutral-900 transition-colors font-medium">
              Show more
            </button>
          </section>

          {/* Who to follow */}
          <section className="bg-white dark:bg-black rounded-2xl border border-slate-300 dark:border-neutral-800 shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100 dark:border-neutral-800">
              <h3 className="font-bold text-gray-900 dark:text-white">Who to follow</h3>
            </div>
            <div className="flex flex-col p-2">
              <UserFollowRow name="Sarah Drasner" username="@sarah_edo" img="https://api.dicebear.com/7.x/avataaars/svg?seed=Sarah" />
              <UserFollowRow name="David Khourshid" username="@davidkpiano" img="https://api.dicebear.com/7.x/avataaars/svg?seed=David" />
              <UserFollowRow name="Cassidy Williams" username="@cassidoo" img="https://api.dicebear.com/7.x/avataaars/svg?seed=Cassidy" />
            </div>
          </section>

        </aside>

      </div>
    </div>
  );
}

// --- SUB-COMPONENTS ---

function NavItem({ icon: Icon, label, active = false }: any) {
  return (
    <button className={`flex items-center gap-4 px-3 py-3 rounded-xl transition-all ${active ? "bg-gray-100 dark:bg-neutral-900 text-gray-900 dark:text-white font-bold" : "text-gray-600 dark:text-neutral-400 hover:bg-gray-50 dark:hover:bg-neutral-900 hover:text-gray-900 dark:hover:text-white font-medium"}`}>
      <Icon className={`w-6 h-6 ${active ? "stroke-[2.5px]" : "stroke-2"}`} />
      <span className="text-base">{label}</span>
    </button>
  );
}

function CommunityItem({ name, members }: any) {
  return (
    <div className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-gray-50 dark:hover:bg-neutral-900 cursor-pointer group transition-colors">
      <div className="flex items-center gap-3">
        <div className={`w-8 h-8 bg-gray-100 dark:bg-neutral-900 rounded-lg flex items-center justify-center text-xs font-bold text-gray-600 dark:text-neutral-300 border border-slate-200 dark:border-neutral-800`}>
          {name[0]}
        </div>
        <div className="flex flex-col items-start">
          <span className="text-sm font-semibold text-gray-900 dark:text-neutral-200 truncate max-w-[120px]">{name}</span>
          <span className="text-xs text-gray-500 dark:text-neutral-500">{members} members</span>
        </div>
      </div>
    </div>
  );
}

function ActionBtn({ icon: Icon }: any) {
  return (
    <button className="w-8 h-8 rounded-full flex items-center justify-center text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors">
      <Icon className="w-5 h-5" />
    </button>
  );
}

function SocialPost({ post }: { post: any }) {
  return (
    <article className="bg-white dark:bg-black border border-slate-300 dark:border-neutral-800 rounded-2xl p-4 shadow-sm hover:border-slate-400 dark:hover:border-neutral-700 transition-colors cursor-pointer">
      <div className="flex gap-4">
        {/* Avatar Column */}
        <div className="shrink-0">
          <div className="w-10 h-10 rounded-full bg-gray-100 dark:bg-neutral-800 overflow-hidden">
            <img src={post.avatar} alt={post.author} />
          </div>
        </div>

        {/* Content Column */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-1">
            <div className="flex items-center gap-2 truncate">
              <span className="font-bold text-gray-900 dark:text-white truncate">{post.author}</span>
              <span className="text-gray-500 dark:text-neutral-500 text-sm truncate">{post.username}</span>
              <span className="text-gray-500 dark:text-neutral-500 text-sm">· {post.time}</span>
            </div>
            <button className="text-gray-400 dark:text-neutral-500 hover:text-gray-600 dark:hover:text-neutral-300 shrink-0">
              <MoreHorizontal className="w-5 h-5" />
            </button>
          </div>

          <p className="text-slate-800 dark:text-neutral-200 text-[15px] leading-relaxed mb-3">
            {post.content}
          </p>

          {post.image && (
            <div className="rounded-xl overflow-hidden border border-slate-200 dark:border-neutral-800 mb-3">
              <img src={post.image} alt="post content" className="w-full h-auto object-cover max-h-[500px]" />
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex items-center justify-between text-gray-500 dark:text-neutral-500 max-w-md">
            <button className="flex items-center gap-2 hover:text-blue-500 dark:hover:text-blue-400 transition-colors group">
              <div className="p-1.5 rounded-full group-hover:bg-blue-50 dark:group-hover:bg-blue-900/30">
                <MessageCircle className="w-4 h-4" />
              </div>
              <span className="text-xs">{post.comments}</span>
            </button>
            <button className="flex items-center gap-2 hover:text-green-500 dark:hover:text-green-400 transition-colors group">
              <div className="p-1.5 rounded-full group-hover:bg-green-50 dark:group-hover:bg-green-900/30">
                <Activity className="w-4 h-4" />
              </div>
              <span className="text-xs">124</span>
            </button>
            <button className="flex items-center gap-2 hover:text-rose-500 dark:hover:text-rose-400 transition-colors group">
              <div className="p-1.5 rounded-full group-hover:bg-rose-50 dark:group-hover:bg-rose-900/30">
                <Heart className="w-4 h-4" />
              </div>
              <span className="text-xs">{post.likes}</span>
            </button>
            <button className="flex items-center gap-2 hover:text-blue-500 dark:hover:text-blue-400 transition-colors group">
              <div className="p-1.5 rounded-full group-hover:bg-blue-50 dark:group-hover:bg-blue-900/30">
                <Share2 className="w-4 h-4" />
              </div>
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}

function TrendRow({ tag, posts }: any) {
  return (
    <div className="px-5 py-3 hover:bg-gray-50 dark:hover:bg-neutral-900 cursor-pointer transition-colors">
      <p className="text-xs text-gray-500 dark:text-neutral-500 mb-0.5">Trending</p>
      <p className="font-bold text-gray-900 dark:text-neutral-200">{tag}</p>
      <p className="text-xs text-gray-500 dark:text-neutral-500 mt-0.5">{posts} posts</p>
    </div>
  );
}

function UserFollowRow({ name, username, img }: any) {
  return (
    <div className="flex items-center justify-between px-3 py-2 hover:bg-gray-50 dark:hover:bg-neutral-900 rounded-xl cursor-pointer transition-colors">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-full bg-gray-100 dark:bg-neutral-800 overflow-hidden">
          <img src={img} alt={name} />
        </div>
        <div className="flex flex-col">
          <span className="text-sm font-bold text-gray-900 dark:text-neutral-200">{name}</span>
          <span className="text-sm text-gray-500 dark:text-neutral-500">{username}</span>
        </div>
      </div>
      <button className="px-4 py-1.5 bg-gray-900 dark:bg-white text-white dark:text-black text-sm font-bold rounded-full hover:bg-gray-800 dark:hover:bg-neutral-200 transition-colors">
        Follow
      </button>
    </div>
  );
}

