import { useState, useEffect, useCallback } from 'react';
import {
  Home, Compass, Settings, Search, MoreHorizontal,
  MessageCircle, Share2, Bookmark, LogOut,
  Activity, Zap, Moon, Sun, Plus, Loader2
} from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/hooks/useAuth";
import { getFeed, reactPost, type FeedItem } from "@/services/post/postService";
import CreatePostModal from "@/components/post/CreatePostModal";

// ─── Time formatter ────────────────────────────────────────────────────────────
function timeAgo(dateStr: string): string {
  const diff = (Date.now() - new Date(dateStr).getTime()) / 1000;
  if (diff < 60) return `Vừa xong`;
  if (diff < 3600) return `${Math.floor(diff / 60)} phút trước`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} giờ trước`;
  return `${Math.floor(diff / 86400)} ngày trước`;
}

// ─── Media Renderer ────────────────────────────────────────────────────────────
function FeedMedia({ urls }: { urls: string[] }) {
  if (!urls || urls.length === 0) return null;

  const isVideo = (url: string) => url.match(/\.(mp4|webm|ogg|mov)$/i) || url.includes("video/upload");

  if (urls.length === 1) {
    const url = urls[0];
    return (
      <div className="rounded-xl overflow-hidden border border-slate-200 dark:border-neutral-800 mb-3 bg-black">
        {isVideo(url) ? (
          <video src={url} controls className="w-full h-auto max-h-[500px]" />
        ) : (
          <img src={url} alt="post" className="w-full h-auto object-cover max-h-[500px]" />
        )}
      </div>
    );
  }

  return (
    <div className={`grid gap-2 mb-3 ${urls.length === 2 ? 'grid-cols-2' : urls.length === 3 ? 'grid-cols-2' : 'grid-cols-2'}`}>
      {urls.map((url, idx) => (
        <div key={idx} className={`rounded-xl overflow-hidden border border-slate-200 dark:border-neutral-800 bg-black ${urls.length === 3 && idx === 0 ? 'col-span-2' : ''}`}>
          {isVideo(url) ? (
            <video src={url} controls className="w-full h-48 object-cover" />
          ) : (
            <img src={url} alt={`post-media-${idx}`} className="w-full h-48 object-cover" />
          )}
        </div>
      ))}
    </div>
  );
}

// ─── HomePage ──────────────────────────────────────────────────────────────────
export default function HomePage() {
  const { accessToken, logout } = useAuth();

  const [isDark, setIsDark] = useState(() =>
    localStorage.getItem('theme') === 'dark' ||
    (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)
  );
  const [showModal, setShowModal] = useState(false);
  const [feed, setFeed] = useState<FeedItem[]>([]);
  const [feedLoading, setFeedLoading] = useState(true);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', isDark);
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
  }, [isDark]);

  const loadFeed = useCallback(async () => {
    if (!accessToken) { setFeedLoading(false); return; }
    setFeedLoading(true);
    const res = await getFeed(0, 20);
    if (res.ok && res.data) setFeed(res.data);
    else if (res.message) toast.error(res.message);
    setFeedLoading(false);
  }, [accessToken]);

  useEffect(() => { loadFeed(); }, [loadFeed]);

  const handleReact = async (postId: number, type: "UPVOTE" | "DOWNVOTE") => {
    if (!accessToken) { toast.error("Vui lòng đăng nhập trước."); return; }
    const res = await reactPost({ postId, reactionType: type });
    if (res.ok) loadFeed();
    else toast.error(res.message ?? "Thả cảm xúc thất bại.");
  };

  const avatar = `https://api.dicebear.com/7.x/avataaars/svg?seed=user`;

  return (
    <div className="bg-[#f8f9fa] dark:bg-neutral-950 min-h-screen font-sans text-slate-900 dark:text-neutral-100 transition-colors duration-300 selection:bg-blue-200 dark:selection:bg-blue-900 dark:selection:text-white">

      {/* TOP NAV */}
      <header className="fixed top-0 left-0 w-full h-16 bg-white dark:bg-neutral-950/80 backdrop-blur-xl border-b border-slate-300 dark:border-neutral-800 z-50 flex items-center justify-between px-6">
        <div className="flex items-center gap-8">
          <div className="flex items-center gap-3 cursor-pointer group">
            <div className="w-10 h-10 bg-gradient-to-br from-blue-600 to-cyan-500 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 group-hover:scale-110 transition-transform">
              <Activity className="text-white w-5 h-5" />
            </div>
            <span className="text-xl font-bold tracking-tight text-gray-900 dark:text-white hidden md:block">SocialPulse</span>
          </div>
          <div className="relative w-64 md:w-[400px]">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 dark:text-neutral-500" />
            <input type="text" placeholder="Tìm kiếm..."
              className="w-full bg-gray-100 dark:bg-neutral-900 border-none rounded-full py-2 pl-12 pr-4 text-sm focus:ring-2 focus:ring-blue-500/20 focus:bg-white dark:focus:bg-black outline-none transition-all dark:text-white dark:placeholder-neutral-500" />
          </div>
        </div>
        <div className="flex items-center gap-4">
          <button onClick={() => setIsDark(d => !d)}
            className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-neutral-900 text-gray-500 dark:text-neutral-400 transition-all">
            {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>
          <button id="new-post-btn" onClick={() => setShowModal(true)}
            className="hidden sm:flex items-center gap-2 px-5 py-2 bg-slate-900 dark:bg-white hover:bg-slate-800 dark:hover:bg-neutral-200 text-white dark:text-black rounded-full text-sm font-semibold transition-all">
            <Plus className="w-4 h-4" /> Đăng bài
          </button>
          <div className="w-9 h-9 rounded-full bg-gray-200 dark:bg-neutral-800 overflow-hidden cursor-pointer border border-slate-300 dark:border-neutral-700">
            <img src={avatar} alt="me" />
          </div>
        </div>
      </header>

      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr_320px] xl:grid-cols-[280px_1fr_350px] gap-8 pt-24 px-6 lg:px-10">

        {/* LEFT SIDEBAR */}
        <aside className="hidden lg:flex flex-col gap-8 sticky top-24 h-[calc(100vh-120px)]">
          <nav className="flex flex-col gap-1">
            <NavItem icon={Home} label="Trang chủ" active />
            <NavItem icon={Compass} label="Khám phá" />
            <NavItem icon={Zap} label="Thông báo" />
            <NavItem icon={Bookmark} label="Dấu trang" />
            <NavItem icon={Settings} label="Cài đặt" />
          </nav>
          
          <div className="pt-6 border-t border-slate-300 dark:border-neutral-800">
            <h4 className="text-xs font-semibold text-gray-500 dark:text-neutral-500 mb-4 px-3">Cộng đồng</h4>
            <div className="flex flex-col gap-2">
              <CommunityItem name="Hệ thống Thiết kế" members="12k" />
              <CommunityItem name="Lập trình viên Frontend" members="8.4k" />
              <CommunityItem name="Cảm hứng UI/UX" members="5.1k" />
            </div>
          </div>

          <div className="mt-auto">
            <button onClick={logout}
              className="flex items-center gap-3 px-3 py-2.5 text-sm font-medium text-gray-500 dark:text-neutral-400 hover:text-gray-900 dark:hover:text-white transition-colors w-full rounded-xl hover:bg-gray-100 dark:hover:bg-neutral-900">
              <LogOut className="w-5 h-5" /> Đăng xuất
            </button>
          </div>
        </aside>

        {/* FEED */}
        <main className="flex flex-col gap-6 pb-10">

          {/* Feed Items */}
          {feedLoading ? (
            <div className="flex justify-center py-16">
              <Loader2 className="w-8 h-8 animate-spin text-blue-500" />
            </div>
          ) : feed.length === 0 ? (
            <div className="text-center py-20 text-gray-400 dark:text-neutral-600">
              <Activity className="w-12 h-12 mx-auto mb-3 opacity-30" />
              <p className="text-lg font-semibold">Chưa có bài viết nào</p>
              <p className="text-sm mt-1">Hãy là người đầu tiên chia sẻ điều gì đó!</p>
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              {feed.map(post => (
                <FeedPost key={post.postId} post={post} onReact={handleReact} />
              ))}
            </div>
          )}
        </main>

        {/* RIGHT SIDEBAR */}
        <aside className="hidden lg:flex flex-col gap-6 sticky top-24 h-[calc(100vh-120px)]">
          
          <section className="bg-white dark:bg-neutral-900 rounded-2xl border border-slate-300 dark:border-neutral-800 shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100 dark:border-neutral-800">
              <h3 className="font-bold text-gray-900 dark:text-white">Thịnh hành cho bạn</h3>
            </div>
            {["#DesignSystems", "#ReactJS", "#Minimalism", "#TechNews"].map((tag, i) => (
              <div key={tag} className="px-5 py-3 hover:bg-gray-50 dark:hover:bg-neutral-900 cursor-pointer transition-colors">
                <p className="text-xs text-gray-500 dark:text-neutral-500 mb-0.5">
                  {["Chủ đề Thiết kế", "Chủ đề Lập trình", "Phong cách sống", "Tin tức Công nghệ"][i]}
                </p>
                <p className="font-bold text-gray-900 dark:text-neutral-200">{tag}</p>
                <p className="text-xs text-gray-500 dark:text-neutral-500 mt-0.5">{["12.4K","8.2K","5.1K","2.8K"][i]} bài viết</p>
              </div>
            ))}
            <button className="w-full px-5 py-3 text-left text-sm text-blue-600 hover:bg-gray-50 dark:hover:bg-neutral-900 transition-colors font-medium">
              Xem thêm
            </button>
          </section>
        </aside>
      </div>

      {/* New Post Modal */}
      <CreatePostModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        currentUserAvatar={avatar}
        onPostCreated={() => { loadFeed(); }}
      />
    </div>
  );
}

// ─── Sub-components ────────────────────────────────────────────────────────────

function NavItem({ icon: Icon, label, active = false }: { icon: React.FC<{className?: string}>, label: string, active?: boolean }) {
  return (
    <button className={`flex items-center gap-4 px-3 py-3 rounded-xl transition-all text-left ${
      active ? "bg-gray-100 dark:bg-neutral-900 text-gray-900 dark:text-white font-bold"
             : "text-gray-600 dark:text-neutral-400 hover:bg-gray-50 dark:hover:bg-neutral-900 hover:text-gray-900 dark:hover:text-white font-medium"}`}>
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
          <span className="text-xs text-gray-500 dark:text-neutral-500">{members} thành viên</span>
        </div>
      </div>
    </div>
  );
}

function FeedPost({ post, onReact }: { post: FeedItem; onReact: (id: number, type: "UPVOTE" | "DOWNVOTE") => void }) {
  const avatar = post.userAvatar ?? `https://api.dicebear.com/7.x/avataaars/svg?seed=${post.username}`;
  const isUpvoted = post.myVote === 1;

  return (
    <article className="bg-white dark:bg-neutral-900 border border-slate-300 dark:border-neutral-800 rounded-2xl p-4 shadow-sm hover:border-slate-400 dark:hover:border-neutral-700 transition-colors">
      <div className="flex gap-4">
        <div className="shrink-0 w-10 h-10 rounded-full overflow-hidden bg-gray-100 dark:bg-neutral-800">
          <img src={avatar} alt={post.username} />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-1">
            <div className="flex items-center gap-2 truncate">
              <span className="font-bold text-gray-900 dark:text-white truncate">{post.username}</span>
              <span className="text-gray-500 dark:text-neutral-500 text-sm">· {timeAgo(post.createdAt)}</span>
            </div>
            <button className="text-gray-400 dark:text-neutral-500 hover:text-gray-600 dark:hover:text-neutral-300 shrink-0">
              <MoreHorizontal className="w-5 h-5" />
            </button>
          </div>

          <p className="text-slate-800 dark:text-neutral-200 text-[19px] leading-relaxed mb-3 whitespace-pre-line break-words">
            {post.content}
          </p>

          <FeedMedia urls={post.imageUrl ? post.imageUrl.split(",") : []} />

          {/* Actions */}
          <div className="flex items-center gap-8 text-gray-500 dark:text-neutral-500">
            {/* Upvote */}
            <button onClick={() => onReact(post.postId, "UPVOTE")}
              className={`flex items-center gap-2 hover:text-blue-500 transition-colors group ${isUpvoted ? "text-blue-600 dark:text-blue-400" : ""}`}>
              <div className="p-1.5 rounded-full group-hover:bg-blue-50 dark:group-hover:bg-blue-900/30">
                <Activity className={`w-5 h-5 ${isUpvoted ? "stroke-[2.5px]" : "stroke-2"}`} />
              </div>
              <span className="text-sm">{post.upvoteCount}</span>
            </button>

            {/* Comment */}
            <button className="flex items-center gap-2 hover:text-blue-500 transition-colors group">
              <div className="p-1.5 rounded-full group-hover:bg-blue-50 dark:group-hover:bg-blue-900/30">
                <MessageCircle className="w-5 h-5 stroke-2" />
              </div>
              <span className="text-sm">{post.cmtCount}</span>
            </button>

            {/* Share */}
            <button className="flex items-center gap-2 hover:text-blue-500 transition-colors group">
              <div className="p-1.5 rounded-full group-hover:bg-blue-50 dark:group-hover:bg-blue-900/30">
                <Share2 className="w-5 h-5 stroke-2" />
              </div>
            </button>
          </div>
        </div>
      </div>
    </article>
  );
}
