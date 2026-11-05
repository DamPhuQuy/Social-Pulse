import { useState, useEffect, useRef } from "react";
import {
  Code2,
  Moon,
  Sun,
  Loader2,
  Plus,
  Flame,
  BookmarkCheck,
} from "lucide-react";
import { toast } from "sonner";
import { useNavigate, useSearchParams } from "react-router-dom";
import { PATHS } from "@/shared/constants/paths";
import { useAuth } from "@/shared/hooks/useAuth";
import { getMyProfile, type UserProfile } from "@/features/profiles/infrastructure/api/userService";
import { getTrendingHashtags, type TrendingHashtagResponse } from "@/features/discovery/infrastructure/api/discoveryService";

import CreatePostModal from "@/features/feed/presentation/components/CreatePostModal";
import AppSidebar from "@/shared/components/AppSidebar";
import BottomNavBar from "@/shared/components/BottomNavBar";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";
import { DevCard } from "@/features/feed/presentation/components/DevCard";
import { FeedTabs } from "@/features/feed/presentation/components/FeedTabs";
import { useFeed } from "@/shared/hooks/useFeed";
import { usePostActions } from "@/shared/hooks/usePostActions";

export default function HomePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { accessToken, isLoading: authLoading } = useAuth();
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [trendingTags, setTrendingTags] = useState<TrendingHashtagResponse[]>([]);
  const [isDark, setIsDark] = useState(() => {
    if (typeof window !== "undefined") {
      return localStorage.getItem("theme") === "dark" ||
        (!("theme" in localStorage) && window.matchMedia("(prefers-color-scheme: dark)").matches);
    }
    return false;
  });

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [dropdownSearch, setDropdownSearch] = useState("");
  const dropdownRef = useRef<HTMLDivElement>(null);

  const {
    feed,
    setFeed,
    feedLoading,
    loadingMore,
    hasMore,
    feedMode,
    setFeedMode,
    selectedTopic,
    setSelectedTopic,
    topics,
    followedTopicSlugs,
    loadFeed,
    loadMoreFeed,
    toggleFollowTopic,
  } = useFeed();

  const {
    reactingPostIds,
    editingPost,
    setEditingPost,
    sharingPost,
    setSharingPost,
    handleReact,
    handleToggleBookmark,
    handleDeletePost,
  } = usePostActions(feed, setFeed);

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add("dark");
      localStorage.setItem("theme", "dark");
    } else {
      document.documentElement.classList.remove("dark");
      localStorage.setItem("theme", "light");
    }
  }, [isDark]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Listen for query params e.g. ?tab=bookmarks
  useEffect(() => {
    const tab = searchParams.get("tab");
    if (tab === "bookmarks") {
      setFeedMode("bookmarks");
      setSelectedTopic(null);
    }
  }, [searchParams, setFeedMode, setSelectedTopic]);

  const loadInitialData = async () => {
    if (accessToken) {
      try {
        const profileRes = await getMyProfile();
        if (profileRes.ok && profileRes.data) setCurrentUser(profileRes.data);
      } catch (e) {
        console.error(e);
      }
    }
    try {
      const trendingRes = await getTrendingHashtags(7, 5);
      if (trendingRes.ok && trendingRes.data) setTrendingTags(trendingRes.data);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    if (!authLoading) {
      loadInitialData();
      loadFeed(feedMode, selectedTopic || undefined);
    }
  }, [authLoading, accessToken, feedMode, selectedTopic]);

  useEffect(() => {
    if (feedLoading || !hasMore) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          loadMoreFeed();
        }
      },
      { threshold: 0.1 }
    );
    const target = document.getElementById("infinite-scroll-trigger");
    if (target) observer.observe(target);
    return () => {
      if (target) observer.unobserve(target);
    };
  }, [feedLoading, hasMore, loadMoreFeed]);

  return (
    <div className="bg-slate-50 dark:bg-[#0f172a] min-h-screen font-sans text-slate-800 dark:text-slate-200 transition-colors duration-200">
      {/* ── TOP HEADER ──────────────────────────────────────────────────────── */}
      <header className="fixed top-0 left-0 w-full h-16 bg-white/90 dark:bg-slate-900/90 backdrop-blur-md border-b border-slate-200/80 dark:border-slate-800 z-40 flex items-center justify-between px-6 shadow-sm">
        <div className="flex items-center gap-3 cursor-pointer group" onClick={() => navigate(PATHS.HOME)}>
          <div className="w-9 h-9 bg-blue-600 rounded-xl flex items-center justify-center shadow-md shadow-blue-500/20 group-hover:scale-105 transition-transform">
            <Code2 className="text-white w-5 h-5" />
          </div>
          <div className="flex items-baseline gap-1.5">
            <span className="text-lg font-black tracking-tight text-slate-900 dark:text-white">DevPulse</span>
            <span className="text-[10px] font-mono font-semibold px-1.5 py-0.5 rounded bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400">DEV FEED</span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsDark((d) => !d)}
            className="p-2 rounded-xl text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            title="Đổi giao diện"
          >
            {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>

          <button
            onClick={() => {
              if (!accessToken) {
                toast.error("Vui lòng đăng nhập để chia sẻ bài viết.");
                navigate(PATHS.LOGIN);
                return;
              }
              setShowCreateModal(true);
            }}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs sm:text-sm font-bold shadow-sm shadow-blue-600/20 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>Đăng bài</span>
          </button>

          {accessToken ? (
            <div
              onClick={() => navigate(PATHS.PROFILE)}
              className="w-9 h-9 rounded-full overflow-hidden cursor-pointer border border-slate-200 dark:border-slate-700 hover:opacity-80 transition-opacity"
            >
              <SafeAvatar src={currentUser?.avatarUrl} alt="me" />
            </div>
          ) : (
            <button
              onClick={() => navigate(PATHS.LOGIN)}
              className="px-4 py-2 rounded-xl bg-slate-900 text-white dark:bg-white dark:text-slate-900 text-xs sm:text-sm font-bold hover:opacity-90 transition-opacity"
            >
              Đăng nhập
            </button>
          )}
        </div>
      </header>

      {/* ── MAIN 3-COLUMN LAYOUT ────────────────────────────────────────────── */}
      <div className="w-full grid grid-cols-1 lg:grid-cols-[240px_1fr_300px] xl:grid-cols-[260px_1fr_320px] gap-6 lg:gap-8 pt-20 lg:pt-24 px-4 sm:px-6 lg:px-10 max-w-7xl mx-auto">
        {/* Left: Navigation Sidebar */}
        <AppSidebar active="home" />

        {/* Center: Feed Stream */}
        <main className="flex flex-col gap-5 pb-24 lg:pb-12 min-w-0">
          {/* Feed Filter Tabs */}
          <FeedTabs
            feedMode={feedMode}
            onSelectMode={(mode) => {
              setFeedMode(mode);
              setSelectedTopic(null);
            }}
            selectedTopic={selectedTopic}
            onSelectTopic={(slug) => {
              setSelectedTopic(slug);
              setFeedMode("topic");
            }}
            topics={topics}
            followedTopicSlugs={followedTopicSlugs}
            onToggleFollowTopic={toggleFollowTopic}
            isDropdownOpen={isDropdownOpen}
            setIsDropdownOpen={setIsDropdownOpen}
            dropdownSearch={dropdownSearch}
            setDropdownSearch={setDropdownSearch}
            dropdownRef={dropdownRef}
          />

          {/* Feed Content Stream */}
          {feedLoading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <Loader2 className="w-7 h-7 animate-spin text-blue-600" />
              <p className="text-xs text-slate-400">Đang nạp bài viết công nghệ mới nhất...</p>
            </div>
          ) : feed.length === 0 ? (
            <div className="text-center py-20 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 p-8 shadow-sm">
              {feedMode === "bookmarks" ? (
                <>
                  <BookmarkCheck className="w-12 h-12 mx-auto mb-3 text-blue-500 opacity-60" />
                  <p className="text-base font-bold text-slate-800 dark:text-slate-200">Chưa có bài viết nào được lưu</p>
                  <p className="text-xs text-slate-400 mt-1">Bấm biểu tượng 🔖 trên bài viết bất kỳ để lưu vào danh sách đọc sau.</p>
                </>
              ) : (
                <>
                  <Code2 className="w-12 h-12 mx-auto mb-3 text-slate-400 opacity-60" />
                  <p className="text-base font-bold text-slate-800 dark:text-slate-200">Chưa có bài viết nào trong feed</p>
                  <p className="text-xs text-slate-400 mt-1">Hãy theo dõi thêm công nghệ hoặc là người đầu tiên chia sẻ!</p>
                </>
              )}
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              {feed.map((post) => (
                <DevCard
                  key={post.postId}
                  post={post}
                  currentUserId={currentUser?.userId}
                  isReacting={reactingPostIds.has(post.postId)}
                  onReact={handleReact}
                  onToggleBookmark={handleToggleBookmark}
                  onEdit={setEditingPost}
                  onDelete={handleDeletePost}
                  onShare={setSharingPost}
                />
              ))}

              {hasMore && (
                <div id="infinite-scroll-trigger" className="flex justify-center py-6">
                  {loadingMore && <Loader2 className="w-5 h-5 animate-spin text-blue-600" />}
                </div>
              )}
            </div>
          )}
        </main>

        {/* Right: Trending Topics & Dev Community */}
        <aside className="hidden lg:flex flex-col gap-5 sticky top-24 h-[calc(100vh-120px)]">
          <section className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 p-4 shadow-sm">
            <div className="flex items-center gap-2 mb-3">
              <Flame className="w-4 h-4 text-amber-500 fill-current" />
              <h3 className="font-bold text-slate-900 dark:text-white text-sm">Chủ đề thịnh hành</h3>
            </div>

            {trendingTags.length === 0 ? (
              <p className="text-xs text-slate-400">Đang cập nhật xu hướng...</p>
            ) : (
              <div className="flex flex-col gap-1.5">
                {trendingTags.map((tag) => (
                  <div
                    key={tag.hashtag}
                    onClick={() => navigate(`${PATHS.DISCOVERY}?q=${encodeURIComponent(tag.hashtag)}&mode=posts&type=hashtag`)}
                    className="p-2.5 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800/80 cursor-pointer transition-colors flex items-center justify-between"
                  >
                    <span className="font-mono text-xs font-semibold text-slate-700 dark:text-slate-300">
                      #{tag.hashtag}
                    </span>
                    <span className="text-[11px] text-slate-400 font-mono">
                      {tag.count} posts
                    </span>
                  </div>
                ))}
              </div>
            )}
          </section>
        </aside>
      </div>

      {/* ── MODALS ─────────────────────────────────────────────────────────── */}
      <CreatePostModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        currentUserAvatar={currentUser?.avatarUrl || undefined}
        onPostCreated={() => loadFeed(feedMode, selectedTopic || undefined)}
      />

      <CreatePostModal
        isOpen={!!editingPost}
        mode="edit"
        initialPost={editingPost}
        onClose={() => setEditingPost(null)}
        currentUserAvatar={currentUser?.avatarUrl || undefined}
        currentUsername={currentUser?.displayName || currentUser?.username}
        onPostUpdated={() => loadFeed(feedMode, selectedTopic || undefined)}
      />

      <CreatePostModal
        isOpen={!!sharingPost}
        mode="create"
        parentPostId={sharingPost?.postId}
        parentPostAuthor={sharingPost?.username}
        parentPostContent={sharingPost?.content}
        onClose={() => setSharingPost(null)}
        currentUserAvatar={currentUser?.avatarUrl || undefined}
        currentUsername={currentUser?.displayName || currentUser?.username}
        onPostCreated={() => {
          setSharingPost(null);
          loadFeed(feedMode, selectedTopic || undefined);
        }}
      />

      <BottomNavBar active="home" />
    </div>
  );
}
