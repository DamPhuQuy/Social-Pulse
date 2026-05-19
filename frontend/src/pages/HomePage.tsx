import { useState, useEffect, useRef } from 'react';
import {
  Search, MoreHorizontal,
  MessageCircle, Share2, Bookmark,
  Activity, Moon, Sun, Loader2, Plus, Edit3, Trash2,
  ChevronDown, X
} from "lucide-react";
import { toast } from "sonner";
import { useNavigate } from "react-router-dom";
import { PATHS } from "@/constants/paths";
import { useAuth } from "@/hooks/useAuth";
import { deletePost, getFeed, reactPost, getPostTopics, type FeedItem, type Privacy, type PostTopic } from "@/services/post/postService";
import { getMyProfile, type UserProfile } from "@/services/user/userService";
import { getTrendingHashtags, type TrendingHashtagResponse } from "@/services/social/discoveryService";
import { createBookmark, deleteBookmark, getBookmarks } from "@/services/social/bookmarkService";

import ReportModal from "@/components/social/ReportModal";
import CreatePostModal from "@/components/post/CreatePostModal";
import AppSidebar from "@/components/social/AppSidebar";
import { SafeAvatar } from "@/pages/ProfilePage";
import CommentSection from "@/components/comment/CommentSection";

// ─── Time formatter ────────────────────────────────────────────────────────────
function timeAgo(dateStr: string): string {
  if (!dateStr) return "";
  const parsedDate = !dateStr.endsWith("Z") && !dateStr.includes("+")
    ? new Date(dateStr.includes("T") ? dateStr + "Z" : dateStr.replace(" ", "T") + "Z")
    : new Date(dateStr);
  const diff = (Date.now() - parsedDate.getTime()) / 1000;
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
      <div className="rounded-2xl overflow-hidden bg-slate-100 dark:bg-neutral-900 border border-slate-200 dark:border-neutral-800 max-h-[500px] flex items-center justify-center">
        {isVideo(url) ? (
          <video src={url} controls className="w-full h-full object-contain max-h-[500px]" />
        ) : (
          <img src={url} alt="post-media" className="w-full h-full object-cover max-h-[500px]" />
        )}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-2 rounded-2xl overflow-hidden bg-slate-100 dark:bg-neutral-900 border border-slate-200 dark:border-neutral-800">
      {urls.map((url, idx) => (
        <div key={idx} className="aspect-video relative overflow-hidden bg-gray-100 dark:bg-neutral-800">
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
  const navigate = useNavigate();
  const { accessToken, isLoading: authLoading } = useAuth();
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [feed, setFeed] = useState<FeedItem[]>([]);
  const [feedLoading, setFeedLoading] = useState(true);
  const [reactingPostIds, setReactingPostIds] = useState<Set<number>>(() => new Set());
  const [bookmarkingPostIds, setBookmarkingPostIds] = useState<Set<number>>(() => new Set());
  const [bookmarkedPostIds, setBookmarkedPostIds] = useState<Set<number>>(() => new Set());
  const [editingPost, setEditingPost] = useState<FeedItem | null>(null);
  const [reportPostId, setReportPostId] = useState<number | null>(null);
  const [trendingTags, setTrendingTags] = useState<TrendingHashtagResponse[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [topics, setTopics] = useState<PostTopic[]>([]);
  const [selectedTopic, setSelectedTopic] = useState<string | null>(null);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [dropdownSearch, setDropdownSearch] = useState("");
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const filteredDropdownTopics = topics.filter(topic =>
    topic.label.toLowerCase().includes(dropdownSearch.toLowerCase()) ||
    topic.slug.toLowerCase().includes(dropdownSearch.toLowerCase())
  );
  
  const [isDark, setIsDark] = useState(() => {
    if (typeof window !== "undefined") {
      return localStorage.getItem("theme") === "dark" || 
        (!("theme" in localStorage) && window.matchMedia("(prefers-color-scheme: dark)").matches);
    }
    return false;
  });
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add("dark");
      localStorage.setItem("theme", "dark");
    } else {
      document.documentElement.classList.remove("dark");
      localStorage.setItem("theme", "light");
    }
  }, [isDark]);

  const loadMyProfile = async () => {
    try {
      const res = await getMyProfile();
      if (res.ok && res.data) {
        setCurrentUser(res.data);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const loadFeed = async (topicSlug?: string) => {
    try {
      setFeedLoading(true);
      setPage(0);
      setHasMore(true);
      const res = await getFeed(0, 20, topicSlug);
      if (res.ok && res.data) {
        setFeed(res.data);
        if (res.data.length < 20) {
          setHasMore(false);
        }
      }
    } catch (err) {
      console.error(err);
    } finally {
      setFeedLoading(false);
    }
  };

  const loadMoreFeed = async () => {
    if (loadingMore || !hasMore) return;
    try {
      setLoadingMore(true);
      const nextPage = page + 1;
      const res = await getFeed(nextPage, 20, selectedTopic || undefined);
      if (res.ok && res.data) {
        if (res.data.length < 20) {
          setHasMore(false);
        }
        const existingIds = new Set(feed.map(item => item.postId));
        const newItems = res.data.filter(item => !existingIds.has(item.postId));
        setFeed((prev) => [...prev, ...newItems]);
        setPage(nextPage);
      } else {
        setHasMore(false);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingMore(false);
    }
  };

  const loadTopics = async () => {
    try {
      const res = await getPostTopics();
      if (res.ok && res.data) {
        setTopics(res.data);
      }
    } catch (err) {
      console.error(err);
    }
  };

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
  }, [feedLoading, hasMore, page, feed, loadingMore]);

  const loadBookmarks = async () => {
    const res = await getBookmarks(0, 100);
    if (res.ok && res.data) {
      setBookmarkedPostIds(new Set((res.data.items ?? []).map((item: any) => item.postId)));
    }
  };

  const loadTrending = async () => {
    try {
      const res = await getTrendingHashtags(7, 5);
      if (res.ok && res.data) {
        setTrendingTags(res.data);
      }
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    if (authLoading || !accessToken) {
      return;
    }

    loadMyProfile();
    loadFeed(selectedTopic || undefined);
    loadBookmarks();
    loadTrending();
    loadTopics();
    
  }, [authLoading, accessToken, selectedTopic]);

  useEffect(() => {
    const handleRealtimePostStats = (e: Event) => {
      const customEvent = e as CustomEvent;
      const stats = customEvent.detail;
      if (!stats || typeof stats.postId !== "number") return;

      setFeed((prevFeed) =>
        prevFeed.map((post) => {
          if (post.postId === stats.postId) {
            return {
              ...post,
              upvoteCount: typeof stats.upvoteCount === "number" ? stats.upvoteCount : post.upvoteCount,
              downvoteCount: typeof stats.downvoteCount === "number" ? stats.downvoteCount : post.downvoteCount,
              cmtCount: typeof stats.cmtCount === "number" ? stats.cmtCount : post.cmtCount,
            };
          }
          return post;
        })
      );
    };

    window.addEventListener("realtime:post_stats", handleRealtimePostStats);
    return () => {
      window.removeEventListener("realtime:post_stats", handleRealtimePostStats);
    };
  }, []);

  const handleReact = async (postId: number, type: "UPVOTE" | "DOWNVOTE") => {
    if (reactingPostIds.has(postId)) return;

    const previousPost = feed.find((post) => post.postId === postId);
    if (!previousPost) return;

    setReactingPostIds((prev) => new Set(prev).add(postId));

    setFeed((prevFeed) =>
      prevFeed.map((post) => {
        if (post.postId === postId) {
          return nextPostPulseState(post, type);
        }
        return post;
      })
    );

    try {
      const res = await reactPost({ postId, reactionType: type });
      if (!res.ok) {
        setFeed((prevFeed) => prevFeed.map((post) => post.postId === postId ? previousPost : post));
        toast.error(res.message ?? "Thả cảm xúc thất bại.");
      }
    } catch (err) {
      console.error(err);
      setFeed((prevFeed) => prevFeed.map((post) => post.postId === postId ? previousPost : post));
    } finally {
      setReactingPostIds((prev) => {
        const next = new Set(prev);
        next.delete(postId);
        return next;
      });
    }
  };

  const handlePostUpdated = (updated: {
    postId: number;
    content: string;
    imageUrl: string | null;
    topicSlugs: string[];
    privacy: Privacy;
    updatedAt: string;
  }) => {
    setFeed((prevFeed) =>
      prevFeed.map((post) =>
        post.postId === updated.postId
          ? {
              ...post,
              content: updated.content,
              imageUrl: updated.imageUrl,
              topicSlugs: updated.topicSlugs,
              privacy: updated.privacy,
              updatedAt: updated.updatedAt,
              createdAt: updated.updatedAt,
            }
          : post
      )
    );
    setEditingPost(null);
  };

  const handleDeletePost = async (postId: number) => {
    const previousFeed = feed;
    setFeed((prevFeed) => prevFeed.filter((post) => post.postId !== postId));

    const result = await deletePost(postId);
    if (!result.ok) {
      setFeed(previousFeed);
      toast.error(result.message ?? "Xóa bài viết thất bại.");
      return;
    }
    toast.success("Đã xóa bài viết.");
  };

  const handleToggleBookmark = async (postId: number) => {
    if (bookmarkingPostIds.has(postId)) return;

    const wasBookmarked = bookmarkedPostIds.has(postId);
    setBookmarkingPostIds((prev) => new Set(prev).add(postId));
    setBookmarkedPostIds((prev) => {
      const next = new Set(prev);
      if (wasBookmarked) next.delete(postId);
      else next.add(postId);
      return next;
    });

    const res = wasBookmarked ? await deleteBookmark(postId) : await createBookmark(postId);
    if (!res.ok) {
      setBookmarkedPostIds((prev) => {
        const next = new Set(prev);
        if (wasBookmarked) next.add(postId);
        else next.delete(postId);
        return next;
      });
      toast.error(res.message ?? "Không thể cập nhật bookmark.");
    }

    setBookmarkingPostIds((prev) => {
      const next = new Set(prev);
      next.delete(postId);
      return next;
    });
  };

  const handleReportSuccess = (options: { hidePost: boolean; hideUser: boolean }) => {
    if (reportPostId === null) return;
    
    const reportedPost = feed.find((p) => p.postId === reportPostId);
    if (!reportedPost) return;

    setFeed((prevFeed) => {
      let nextFeed = prevFeed;
      if (options.hidePost) {
        nextFeed = nextFeed.filter((p) => p.postId !== reportPostId);
      }
      if (options.hideUser) {
        nextFeed = nextFeed.filter((p) => p.userId !== reportedPost.userId);
      }
      return nextFeed;
    });
  };

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <header className="fixed top-0 left-0 w-full h-16 bg-white/95 dark:bg-[#1e1e1e]/95 backdrop-blur-xl border-b border-slate-200/80 dark:border-[#2a2a2a] z-40 flex items-center justify-between px-6 shadow-sm dark:shadow-none">
        <div className="flex items-center gap-8">
          <div className="flex items-center gap-3 cursor-pointer group" onClick={() => navigate(PATHS.HOME)}>
            <div className="w-10 h-10 bg-slate-900 dark:bg-white rounded-xl flex items-center justify-center shadow-md group-hover:scale-110 group-active:scale-95 transition-all duration-300">
              <Activity className="text-white dark:text-slate-900 w-5 h-5 stroke-[2.5px]" />
            </div>
            <span className="text-xl font-extrabold tracking-tight text-slate-900 dark:text-white hidden md:block">SocialPulse</span>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <button onClick={() => setIsDark(d => !d)} className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-neutral-850 text-slate-500 dark:text-slate-400 transition-all">
            {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>
          <button onClick={() => setShowModal(true)}
            className="hidden sm:flex items-center gap-2 px-4 py-2 rounded-full bg-slate-900 text-white dark:bg-white dark:text-slate-900 text-sm font-bold hover:opacity-90 transition-opacity">
            <Plus className="w-4 h-4" /> Đăng bài
          </button>
          <div onClick={() => navigate(PATHS.PROFILE)} className="w-9 h-9 rounded-full overflow-hidden cursor-pointer border border-slate-200 dark:border-neutral-700 hover:opacity-80 transition-opacity">
            <SafeAvatar src={currentUser?.avatarUrl} alt="me" />
          </div>
        </div>
      </header>

      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr_320px] xl:grid-cols-[280px_1fr_350px] gap-8 pt-24 px-6 lg:px-10">

        {/* LEFT SIDEBAR */}
        <AppSidebar active="home" />

        {/* FEED */}
        <main className="flex flex-col gap-6 pb-10 min-w-0">

          {/* Topic Selector & Search Dropdown */}
          <div className="flex gap-3 items-center relative z-30 pb-2">
            <button
              onClick={() => {
                setSelectedTopic(null);
                setIsDropdownOpen(false);
              }}
              className={`px-5 py-2.5 rounded-full text-sm font-bold whitespace-nowrap transition-all shadow-sm ${
                selectedTopic === null
                  ? "bg-slate-900 text-white dark:bg-white dark:text-slate-900"
                  : "bg-white text-slate-600 hover:bg-slate-50 dark:bg-[#1e1e1e] dark:text-neutral-400 dark:hover:bg-neutral-850 border border-slate-200/60 dark:border-[#2a2a2a]"
              }`}
            >
              Dành cho bạn
            </button>

            {/* Dropdown Container */}
            <div ref={dropdownRef} className="relative">
              {selectedTopic === null ? (
                <button
                  onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                  className="flex items-center gap-2 px-5 py-2.5 rounded-full text-sm font-semibold bg-white hover:bg-slate-50 dark:bg-[#1e1e1e] dark:hover:bg-neutral-850 text-slate-600 dark:text-neutral-400 border border-slate-200/60 dark:border-[#2a2a2a] transition-all shadow-sm cursor-pointer"
                >
                  <Search className="w-4 h-4 text-slate-400" />
                  <span>Khám phá chủ đề...</span>
                  <ChevronDown className={`w-4 h-4 text-slate-400 transition-transform duration-200 ${isDropdownOpen ? "rotate-180" : ""}`} />
                </button>
              ) : (
                <div className="flex items-center gap-1.5 bg-blue-50 dark:bg-blue-950/40 border border-blue-100 dark:border-blue-900/60 rounded-full pl-5 pr-2.5 py-1.5 shadow-sm transition-all animate-in zoom-in-95 duration-200">
                  <span className="text-sm font-bold text-blue-700 dark:text-blue-300">
                    Chủ đề: {topics.find(t => t.slug === selectedTopic)?.label ?? selectedTopic}
                  </span>
                  <button
                    onClick={() => {
                      setSelectedTopic(null);
                      setIsDropdownOpen(false);
                    }}
                    className="p-1 rounded-full bg-blue-100/60 dark:bg-blue-900/50 hover:bg-blue-200/80 dark:hover:bg-blue-800 text-blue-600 dark:text-blue-300 transition-colors"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}

              {/* Popover Dropdown Panel */}
              {isDropdownOpen && (
                <div className="absolute top-full left-0 mt-2 w-80 bg-white dark:bg-[#1c1c1e] border border-slate-200/80 dark:border-neutral-800 rounded-2xl shadow-[0_12px_30px_rgba(0,0,0,0.1)] dark:shadow-[0_15px_45px_rgba(0,0,0,0.6)] z-50 p-4 gap-3 flex flex-col animate-in fade-in slide-in-from-top-2 duration-200">
                  {/* Search Header */}
                  <div className="relative">
                    <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                    <input
                      type="text"
                      value={dropdownSearch}
                      onChange={(e) => setDropdownSearch(e.target.value)}
                      placeholder="Tìm kiếm chủ đề..."
                      className="w-full bg-slate-50 dark:bg-neutral-900 border border-slate-200/60 dark:border-neutral-800 rounded-xl py-2 pl-10 pr-4 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 text-slate-800 dark:text-white"
                      autoFocus
                    />
                    {dropdownSearch && (
                      <button
                        onClick={() => setDropdownSearch("")}
                        className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full hover:bg-slate-200 dark:hover:bg-neutral-800 text-slate-400"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    )}
                  </div>

                  {/* Scrollable List */}
                  <div className="max-h-60 overflow-y-auto pr-1 flex flex-col gap-1 scrollbar-none">
                    {filteredDropdownTopics.length === 0 ? (
                      <div className="text-center py-6 text-sm text-slate-400 dark:text-neutral-500">
                        Không tìm thấy chủ đề nào
                      </div>
                    ) : (
                      filteredDropdownTopics.map((topic) => (
                        <button
                          key={topic.slug}
                          onClick={() => {
                            setSelectedTopic(topic.slug);
                            setIsDropdownOpen(false);
                            setDropdownSearch("");
                          }}
                          className={`w-full text-left px-3.5 py-2.5 rounded-xl text-sm font-semibold transition-all flex items-center justify-between hover:bg-slate-50 dark:hover:bg-neutral-800/60 ${
                            selectedTopic === topic.slug
                              ? "text-blue-600 dark:text-blue-400 bg-blue-50/50 dark:bg-blue-950/20"
                              : "text-slate-700 dark:text-neutral-300"
                          }`}
                        >
                          <span>{topic.label}</span>
                          <span className="text-[10px] uppercase font-bold text-slate-400 bg-slate-100 dark:bg-neutral-800 px-2 py-0.5 rounded">
                            {topic.category}
                          </span>
                        </button>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>

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
                <FeedPost
                  key={post.postId}
                  post={post}
                  onReact={handleReact}
                  isReacting={reactingPostIds.has(post.postId)}
                  currentUserId={currentUser?.userId}
                  onEdit={setEditingPost}
                  onDelete={handleDeletePost}
                  isBookmarked={bookmarkedPostIds.has(post.postId)}
                  onToggleBookmark={handleToggleBookmark}
                  onReport={setReportPostId}
                />
              ))}
              {hasMore && (
                <div id="infinite-scroll-trigger" className="flex justify-center py-6">
                  {loadingMore && <Loader2 className="w-6 h-6 animate-spin text-blue-500" />}
                </div>
              )}
            </div>
          )}
        </main>

        {/* RIGHT SIDEBAR */}
        <aside className="hidden lg:flex flex-col gap-6 sticky top-24 h-[calc(100vh-120px)]">
          
          <section className="bg-white dark:bg-[#1e1e1e] rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-[0_4px_20px_rgba(0,0,0,0.03)] dark:shadow-[0_4px_25px_rgba(0,0,0,0.4)] overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100 dark:border-[#2a2a2a]">
              <h3 className="font-bold text-slate-800 dark:text-[#e4e6eb]">Thịnh hành cho bạn</h3>
            </div>
            {trendingTags.length === 0 ? (
              <div className="px-5 py-6 text-center text-xs text-slate-500 dark:text-neutral-500">
                Chưa có xu hướng thịnh hành
              </div>
            ) : (
              trendingTags.map((tag) => (
                <div key={tag.hashtag} className="px-5 py-3 hover:bg-slate-50 dark:hover:bg-neutral-800/40 cursor-pointer transition-colors">
                  <p className="text-xs text-slate-500 dark:text-neutral-400 mb-0.5">Xu hướng thịnh hành</p>
                  <button
                    onClick={() => navigate(`${PATHS.DISCOVERY}?q=${encodeURIComponent(tag.hashtag)}&mode=posts&type=hashtag`)}
                    className="font-bold text-slate-800 dark:text-[#e4e6eb] hover:underline"
                  >
                    #{tag.hashtag}
                  </button>
                  <p className="text-xs text-slate-500 dark:text-neutral-400 mt-0.5">{tag.count} bài đăng</p>
                </div>
              ))
            )}
            <button onClick={() => navigate(PATHS.DISCOVERY)} className="w-full px-5 py-3 text-left text-sm text-blue-600 dark:text-blue-400 hover:bg-slate-50 dark:hover:bg-neutral-800/40 transition-colors font-medium">
              Xem thêm
            </button>
          </section>
        </aside>
      </div>

      {/* New Post Modal */}
      <CreatePostModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        currentUserAvatar={currentUser?.avatarUrl || undefined}
        onPostCreated={() => { loadFeed(); }}
      />
      <CreatePostModal
        isOpen={!!editingPost}
        mode="edit"
        initialPost={editingPost}
        onClose={() => setEditingPost(null)}
        currentUserAvatar={currentUser?.avatarUrl || undefined}
        currentUsername={currentUser?.displayName || currentUser?.username}
        onPostUpdated={handlePostUpdated}
      />
      <ReportModal
        isOpen={reportPostId !== null}
        targetType="POST"
        targetId={reportPostId}
        title="bài viết"
        onClose={() => setReportPostId(null)}
        onReportSuccess={handleReportSuccess}
      />
    </div>
  );
}

// ─── Sub-components ────────────────────────────────────────────────────────────

function nextPostPulseState<T extends { myVote: number | null; upvoteCount: number; downvoteCount: number; myReaction: string | null }>(
  post: T,
  type: "UPVOTE" | "DOWNVOTE",
): T {
  const currentVote = post.myVote ?? 0;
  const targetVote = type === "UPVOTE" ? 1 : -1;
  const nextVote = currentVote === targetVote ? 0 : targetVote;

  return {
    ...post,
    myVote: nextVote,
    myReaction: nextVote === 1 ? "UPVOTE" : nextVote === -1 ? "DOWNVOTE" : null,
    upvoteCount: Math.max(0, post.upvoteCount + (nextVote === 1 ? 1 : 0) - (currentVote === 1 ? 1 : 0)),
    downvoteCount: Math.max(0, post.downvoteCount + (nextVote === -1 ? 1 : 0) - (currentVote === -1 ? 1 : 0)),
  };
}

function FeedPost({
  post,
  onReact,
  isReacting,
  currentUserId,
  onEdit,
  onDelete,
  isBookmarked,
  onToggleBookmark,
  onReport,
}: {
  post: FeedItem;
  onReact: (id: number, type: "UPVOTE" | "DOWNVOTE") => void;
  isReacting: boolean;
  currentUserId?: number;
  onEdit: (post: FeedItem) => void;
  onDelete: (postId: number) => void;
  isBookmarked: boolean;
  onToggleBookmark: (postId: number) => void;
  onReport: (postId: number) => void;
}) {
  const navigate = useNavigate();
  const isUpvoted = post.myVote === 1;
  const [showComments, setShowComments] = useState(false);
  const [cmtCount, setCmtCount] = useState(post.cmtCount);
  const [showMenu, setShowMenu] = useState(false);
  const isAuthor = currentUserId === post.userId;

  useEffect(() => {
    setCmtCount(post.cmtCount);
  }, [post.cmtCount]);

  const navigateToProfile = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigate(`/profile/${post.username}`);
  };

  return (
    <article className="bg-white dark:bg-[#1e1e1e] border border-slate-200/80 dark:border-[#2a2a2a] rounded-2xl p-5 shadow-[0_4px_20px_rgba(0,0,0,0.03)] dark:shadow-[0_4px_25px_rgba(0,0,0,0.4)] hover:border-slate-300 dark:hover:border-neutral-700 hover:shadow-[0_8px_30px_rgba(0,0,0,0.04)] dark:hover:shadow-[0_8px_35px_rgba(0,0,0,0.5)] transition-all duration-300">
      <div className="flex gap-4">
        <div onClick={navigateToProfile} className="shrink-0 w-10 h-10 rounded-full overflow-hidden bg-slate-100 dark:bg-neutral-850 cursor-pointer hover:opacity-85 transition-opacity">
          <SafeAvatar src={post.userAvatar} alt={post.username} />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-1">
            <div className="flex items-center gap-2 truncate">
              <span onClick={navigateToProfile} className="font-bold text-slate-800 dark:text-[#e4e6eb] truncate cursor-pointer hover:underline">{post.username}</span>
              <span className="text-slate-500 dark:text-neutral-400 text-sm">· {timeAgo(post.createdAt)}</span>
            </div>
            <div className="relative shrink-0">
              <button onClick={() => setShowMenu((value) => !value)} className="text-slate-400 dark:text-neutral-500 hover:text-slate-600 dark:hover:text-neutral-300 p-1 rounded-full">
                <MoreHorizontal className="w-5 h-5" />
              </button>
              {showMenu && (
                <div className="absolute right-0 top-8 z-20 w-44 overflow-hidden rounded-xl border border-slate-200 dark:border-neutral-800 bg-white dark:bg-neutral-900 shadow-xl">
                  {isAuthor ? (
                    <>
                      <button onClick={() => { setShowMenu(false); onEdit(post); }} className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm font-semibold hover:bg-slate-50 dark:hover:bg-neutral-800">
                        <Edit3 className="w-4 h-4" /> Chỉnh sửa
                      </button>
                      <button onClick={() => { setShowMenu(false); onDelete(post.postId); }} className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm font-semibold text-red-600 hover:bg-red-50 dark:hover:bg-red-500/10">
                        <Trash2 className="w-4 h-4" /> Xóa bài viết
                      </button>
                    </>
                  ) : (
                    <button onClick={() => { setShowMenu(false); onReport(post.postId); }} className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm font-semibold text-red-600 hover:bg-red-50 dark:hover:bg-red-500/10">
                      <Trash2 className="w-4 h-4" /> Báo cáo
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>

          <p className="text-slate-800 dark:text-[#e4e6eb] text-[18px] leading-relaxed mb-3 whitespace-pre-line break-words">
            {post.content}
          </p>

          <FeedMedia urls={post.imageUrl ? post.imageUrl.split(",") : []} />

          {post.topicSlugs?.length > 0 && (
            <div className="mb-3 flex flex-wrap gap-2">
              {post.topicSlugs.map((topic) => (
                <span key={topic} className="rounded-full bg-slate-100 dark:bg-neutral-800 px-2.5 py-1 text-xs font-semibold text-slate-500 dark:text-neutral-400">
                  #{topic}
                </span>
              ))}
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center gap-8 text-gray-500 dark:text-neutral-500 border-b border-transparent pb-1">
            {/* Upvote */}
            <button disabled={isReacting} onClick={() => onReact(post.postId, "UPVOTE")}
              className={`flex items-center gap-2 hover:text-slate-900 dark:hover:text-white transition-colors group ${isUpvoted ? "text-blue-600 dark:text-blue-400" : ""}`}>
              <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                <Activity className={`w-5 h-5 ${isUpvoted ? "stroke-[2.5px]" : "stroke-2"}`} />
              </div>
              <span className="text-sm">{post.upvoteCount}</span>
            </button>

            {/* Comment */}
            <button
              onClick={() => setShowComments(!showComments)}
              className={`flex items-center gap-2 hover:text-slate-900 dark:hover:text-white transition-colors group ${showComments ? "text-slate-905 dark:text-white font-bold" : ""}`}
            >
              <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                <MessageCircle className={`w-5 h-5 ${showComments ? "stroke-[2.5px]" : "stroke-2"}`} />
              </div>
              <span className="text-sm">{cmtCount}</span>
            </button>

            <button onClick={() => onToggleBookmark(post.postId)} className={`flex items-center gap-2 hover:text-slate-900 dark:hover:text-white transition-colors group ${isBookmarked ? "text-blue-600 dark:text-blue-400" : ""}`}>
              <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                <Bookmark className={`w-5 h-5 stroke-2 ${isBookmarked ? "fill-current" : ""}`} />
              </div>
            </button>

            <button
              onClick={() => {
                navigator.clipboard.writeText(`${window.location.origin}/posts/${post.postId}`);
                toast.success("Đã sao chép liên kết bài viết.");
              }}
              className="flex items-center gap-2 hover:text-slate-900 dark:hover:text-white transition-colors group"
            >
              <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                <Share2 className="w-5 h-5 stroke-2" />
              </div>
            </button>
          </div>

          {/* Expanded Comment Section */}
          {showComments && (
            <CommentSection
              postId={post.postId}
              initialCmtCount={cmtCount}
              onCommentCountChange={setCmtCount}
            />
          )}
        </div>
      </div>
    </article>
  );
}
