import { useState, useEffect, useRef } from "react";
import {
  Search,
  Activity,
  Moon,
  Sun,
  Loader2,
  Plus,
  ChevronDown,
  Compass,
  Users,
  Hash,
} from "lucide-react";
import { toast } from "sonner";
import { useNavigate } from "react-router-dom";
import { PATHS } from "@/shared/constants/paths";
import { useAuth } from "@/shared/hooks/useAuth";
import { getMyProfile, type UserProfile } from "@/features/profiles/infrastructure/api/userService";
import { getTrendingHashtags, type TrendingHashtagResponse } from "@/features/discovery/infrastructure/api/discoveryService";
import { getBookmarks } from "@/features/bookmarks/infrastructure/api/bookmarkService";

import ReportModal from "@/shared/components/ReportModal";
import CreatePostModal from "@/features/feed/presentation/components/CreatePostModal";
import AppSidebar from "@/shared/components/AppSidebar";
import BottomNavBar from "@/shared/components/BottomNavBar";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";
import { PostCard } from "@/features/feed/presentation/components/PostCard";
import { useFeed } from "@/shared/hooks/useFeed";
import { usePostActions } from "@/shared/hooks/usePostActions";

export default function HomePage() {
  const navigate = useNavigate();
  const { accessToken, isLoading: authLoading } = useAuth();
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [bookmarkedPostIds, setBookmarkedPostIds] = useState<Set<number>>(() => new Set());
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
    toggleFollowTopic
  } = useFeed();

  const {
    reactingPostIds,
    editingPost,
    setEditingPost,
    reportPostId,
    setReportPostId,
    sharingPost,
    setSharingPost,
    handleReact,
    handleToggleBookmark,
    handleDeletePost,
    handleBlockUser
  } = usePostActions(feed, setFeed, bookmarkedPostIds, setBookmarkedPostIds);

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

  const loadInitialData = async () => {
    if (accessToken) {
      try {
        const [profileRes, bookmarksRes] = await Promise.all([
          getMyProfile(),
          getBookmarks(0, 100).catch(() => ({ ok: false, data: { items: [] } }))
        ]);
        if (profileRes.ok && profileRes.data) setCurrentUser(profileRes.data);
        if (bookmarksRes.ok && bookmarksRes.data) {
          setBookmarkedPostIds(new Set((bookmarksRes.data.items ?? []).map((item: any) => item.postId)));
        }
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

  const filteredTopics = topics.filter(
    (t) => t.label.toLowerCase().includes(dropdownSearch.toLowerCase()) || t.slug.toLowerCase().includes(dropdownSearch.toLowerCase())
  );

  return (
    <div className="bg-[#ffffff] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      {/* TOP BAR */}
      <header className="fixed top-0 left-0 w-full h-16 bg-white/95 dark:bg-[#1e1e1e]/95 backdrop-blur-xl border-b border-slate-200/80 dark:border-[#2a2a2a] z-40 flex items-center justify-between px-6 shadow-sm">
        <div className="flex items-center gap-3 cursor-pointer group" onClick={() => navigate(PATHS.HOME)}>
          <div className="w-10 h-10 bg-[#000000] dark:bg-white rounded-full flex items-center justify-center shadow-md group-hover:scale-105 transition-all duration-300">
            <Activity className="text-white dark:text-slate-900 w-5 h-5 stroke-[2.5px]" />
          </div>
          <span className="text-xl font-extrabold tracking-tight text-slate-900 dark:text-white hidden md:block">SocialPulse</span>
        </div>

        <div className="flex items-center gap-4">
          <button onClick={() => setIsDark((d) => !d)} className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-neutral-800 text-slate-500 transition-all">
            {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>

          <button
            onClick={() => {
              if (!accessToken) {
                toast.error("Vui lòng đăng nhập để đăng bài.");
                navigate(PATHS.LOGIN);
                return;
              }
              setShowCreateModal(true);
            }}
            className="flex items-center gap-2 px-5 py-2.5 rounded-full bg-[#000000] text-white dark:bg-white dark:text-slate-900 text-sm font-bold hover:opacity-90 transition-opacity shadow-sm"
          >
            <Plus className="w-4 h-4" /> Đăng bài
          </button>

          {accessToken ? (
            <div onClick={() => navigate(PATHS.PROFILE)} className="w-9 h-9 rounded-full overflow-hidden cursor-pointer border border-slate-200 dark:border-neutral-700 hover:opacity-80 transition-opacity">
              <SafeAvatar src={currentUser?.avatarUrl} alt="me" />
            </div>
          ) : (
            <button
              onClick={() => navigate(PATHS.LOGIN)}
              className="px-4 py-2 rounded-full bg-[#0064e0] text-white text-sm font-bold hover:opacity-90 transition-opacity"
            >
              Đăng nhập
            </button>
          )}
        </div>
      </header>

      {/* MAIN CONTAINER */}
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr_320px] xl:grid-cols-[280px_1fr_350px] gap-6 lg:gap-8 pt-20 lg:pt-24 px-4 sm:px-6 lg:px-10">
        <AppSidebar active="home" />

        <main className="flex flex-col gap-6 pb-24 lg:pb-10 min-w-0">
          {/* TAB CATEGORY PILLS (Meta DESIGN.md style) */}
          <div className="flex gap-2 items-center relative z-30 flex-wrap">
            <button
              onClick={() => {
                setFeedMode("discover");
                setSelectedTopic(null);
              }}
              className={`px-5 py-2.5 rounded-full text-sm font-bold flex items-center gap-2 transition-all ${
                feedMode === "discover"
                  ? "bg-[#000000] text-white dark:bg-white dark:text-slate-900 shadow-sm"
                  : "bg-white text-slate-600 dark:bg-[#1e1e1e] dark:text-neutral-300 border border-slate-200/80 dark:border-[#2a2a2a] hover:bg-slate-50"
              }`}
            >
              <Compass className="w-4 h-4" /> Khám phá
            </button>

            <button
              onClick={() => {
                setFeedMode("following");
                setSelectedTopic(null);
              }}
              className={`px-5 py-2.5 rounded-full text-sm font-bold flex items-center gap-2 transition-all ${
                feedMode === "following"
                  ? "bg-[#000000] text-white dark:bg-white dark:text-slate-900 shadow-sm"
                  : "bg-white text-slate-600 dark:bg-[#1e1e1e] dark:text-neutral-300 border border-slate-200/80 dark:border-[#2a2a2a] hover:bg-slate-50"
              }`}
            >
              <Users className="w-4 h-4" /> Đang theo dõi
            </button>

            {/* TOPIC DROPDOWN CHIP */}
            <div ref={dropdownRef} className="relative">
              <button
                onClick={() => setIsDropdownOpen((prev) => !prev)}
                className={`px-5 py-2.5 rounded-full text-sm font-bold flex items-center gap-2 transition-all ${
                  feedMode === "topic"
                    ? "bg-[#0064e0] text-white shadow-sm"
                    : "bg-white text-slate-600 dark:bg-[#1e1e1e] dark:text-neutral-300 border border-slate-200/80 dark:border-[#2a2a2a] hover:bg-slate-50"
                }`}
              >
                <Hash className="w-4 h-4" />
                <span>{selectedTopic ? topics.find((t) => t.slug === selectedTopic)?.label || selectedTopic : "Chủ đề..."}</span>
                <ChevronDown className={`w-4 h-4 transition-transform duration-200 ${isDropdownOpen ? "rotate-180" : ""}`} />
              </button>

              {isDropdownOpen && (
                <div className="absolute left-0 top-full mt-2 w-80 bg-white dark:bg-[#1c1c1e] border border-slate-200/80 dark:border-neutral-800 rounded-3xl shadow-2xl z-50 p-4 flex flex-col gap-3 animate-in fade-in duration-150">
                  <div className="relative">
                    <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                    <input
                      type="text"
                      value={dropdownSearch}
                      onChange={(e) => setDropdownSearch(e.target.value)}
                      placeholder="Tìm kiếm chủ đề..."
                      className="w-full bg-slate-50 dark:bg-neutral-900 border border-slate-200 dark:border-neutral-800 rounded-xl py-2 pl-10 pr-4 text-sm focus:outline-none focus:border-[#0064e0]"
                      autoFocus
                    />
                  </div>

                  <div className="max-h-60 overflow-y-auto pr-1 flex flex-col gap-1">
                    {filteredTopics.map((topic) => {
                      const isFollowing = followedTopicSlugs.has(topic.slug);
                      return (
                        <div
                          key={topic.slug}
                          className="flex items-center justify-between px-3 py-2 rounded-xl hover:bg-slate-50 dark:hover:bg-neutral-800 transition-colors"
                        >
                          <span
                            onClick={() => {
                              setSelectedTopic(topic.slug);
                              setFeedMode("topic");
                              setIsDropdownOpen(false);
                            }}
                            className="text-sm font-semibold text-slate-800 dark:text-neutral-200 cursor-pointer flex-1"
                          >
                            {topic.label}
                          </span>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              toggleFollowTopic(topic.slug);
                            }}
                            className={`px-3 py-1 rounded-full text-xs font-bold transition-all ${
                              isFollowing
                                ? "bg-slate-100 text-slate-600 dark:bg-neutral-800 dark:text-neutral-300"
                                : "bg-[#0064e0] text-white hover:bg-[#0457cb]"
                            }`}
                          >
                            {isFollowing ? "Đã Theo Dõi" : "+ Theo Dõi"}
                          </button>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* FEED POST LIST */}
          {feedLoading ? (
            <div className="flex justify-center py-16">
              <Loader2 className="w-8 h-8 animate-spin text-[#0064e0]" />
            </div>
          ) : feed.length === 0 ? (
            <div className="text-center py-20 bg-white dark:bg-[#1e1e1e] rounded-3xl border border-slate-200/80 dark:border-[#2a2a2a] p-8">
              <Activity className="w-12 h-12 mx-auto mb-3 text-slate-300 opacity-60" />
              <p className="text-lg font-bold text-slate-700 dark:text-neutral-300">Chưa có bài viết nào</p>
              <p className="text-sm text-slate-400 mt-1">Hãy theo dõi thêm chủ đề hoặc tạo bài viết đầu tiên!</p>
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              {feed.map((post, index) => (
                <PostCard
                  key={post.postId}
                  post={post}
                  rank={index + 1}
                  currentUserId={currentUser?.userId}
                  isBookmarked={bookmarkedPostIds.has(post.postId)}
                  isReacting={reactingPostIds.has(post.postId)}
                  onReact={handleReact}
                  onToggleBookmark={handleToggleBookmark}
                  onEdit={setEditingPost}
                  onDelete={handleDeletePost}
                  onReport={setReportPostId}
                  onBlockUser={handleBlockUser}
                  onShare={setSharingPost}
                />
              ))}

              {hasMore && (
                <div id="infinite-scroll-trigger" className="flex justify-center py-6">
                  {loadingMore && <Loader2 className="w-6 h-6 animate-spin text-[#0064e0]" />}
                </div>
              )}
            </div>
          )}
        </main>

        {/* RIGHT SIDEBAR */}
        <aside className="hidden lg:flex flex-col gap-6 sticky top-24 h-[calc(100vh-120px)]">
          <section className="bg-white dark:bg-[#1e1e1e] rounded-3xl border border-slate-200/80 dark:border-[#2a2a2a] p-5 shadow-sm">
            <h3 className="font-bold text-slate-900 dark:text-white text-base mb-3">Xu hướng thịnh hành</h3>
            {trendingTags.length === 0 ? (
              <p className="text-xs text-slate-400">Chưa có xu hướng thịnh hành</p>
            ) : (
              <div className="flex flex-col gap-2">
                {trendingTags.map((tag) => (
                  <div
                    key={tag.hashtag}
                    onClick={() => navigate(`${PATHS.DISCOVERY}?q=${encodeURIComponent(tag.hashtag)}&mode=posts&type=hashtag`)}
                    className="p-3 rounded-2xl hover:bg-slate-50 dark:hover:bg-neutral-800 cursor-pointer transition-colors"
                  >
                    <p className="text-xs font-semibold text-slate-400">#XuHướng</p>
                    <p className="font-bold text-slate-800 dark:text-neutral-200">#{tag.hashtag}</p>
                    <p className="text-xs text-slate-400 mt-0.5">{tag.count} bài đăng</p>
                  </div>
                ))}
              </div>
            )}
          </section>
        </aside>
      </div>

      {/* MODALS */}
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
      <ReportModal
        isOpen={reportPostId !== null}
        targetType="POST"
        targetId={reportPostId}
        title="bài viết"
        onClose={() => setReportPostId(null)}
        onReportSuccess={() => loadFeed(feedMode, selectedTopic || undefined)}
      />
      <BottomNavBar active="home" />
    </div>
  );
}
