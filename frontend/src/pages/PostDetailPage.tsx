import { useEffect, useState } from "react";
import { Activity, ArrowLeft, Loader2, MessageCircle, Link, Bookmark, Share2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import AppHeader from "@/components/social/AppHeader";
import AppSidebar from "@/components/social/AppSidebar";
import CommentSection from "@/components/comment/CommentSection";
import { SafeAvatar } from "@/components/ui/SafeAvatar";
import { PostMedia } from "@/components/post/PostMedia";
import { reactPost, type OriginalPostData } from "@/services/post/postService";
import { getPostDetail, type ViewPostResponse } from "@/services/social/postDetailService";
import { nextPostPulseState } from "@/lib/postUtils";
import { timeAgo } from "@/lib/dateUtils";
import { createBookmark, deleteBookmark, getBookmarks } from "@/services/social/bookmarkService";

export default function PostDetailPage() {
  const navigate = useNavigate();
  const { postId } = useParams<{ postId: string }>();
  const [post, setPost] = useState<ViewPostResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [reacting, setReacting] = useState(false);
  const [commentCount, setCommentCount] = useState(0);
  const [bookmarkedPostIds, setBookmarkedPostIds] = useState<Set<number>>(() => new Set());
  const [bookmarkingPostIds, setBookmarkingPostIds] = useState<Set<number>>(() => new Set());

  useEffect(() => {
    if (!postId) return;
    loadPost(Number(postId));

    getBookmarks(0, 100).then((res) => {
      if (res.ok && res.data) {
        setBookmarkedPostIds(
          new Set((res.data.items ?? []).map((item) => item.postId))
        );
      }
    });

    const handleRealtimePostStats = (e: Event) => {
      const customEvent = e as CustomEvent;
      const stats = customEvent.detail;
      if (!stats || stats.postId !== Number(postId)) return;

      setPost((prev) => {
        if (!prev) return null;
        return {
          ...prev,
          upvoteCount: typeof stats.upvoteCount === "number" ? stats.upvoteCount : prev.upvoteCount,
          downvoteCount: typeof stats.downvoteCount === "number" ? stats.downvoteCount : prev.downvoteCount,
        };
      });

      if (typeof stats.cmtCount === "number") {
        setCommentCount(stats.cmtCount);
      }
    };

    window.addEventListener("realtime:post_stats", handleRealtimePostStats);
    return () => {
      window.removeEventListener("realtime:post_stats", handleRealtimePostStats);
    };
  }, [postId]);

  const loadPost = async (id: number) => {
    setLoading(true);
    const res = await getPostDetail(id);
    setLoading(false);
    if (res.ok && res.data) {
      setPost(res.data);
      setCommentCount(res.data.cmtCount);
    } else {
      toast.error(res.message ?? "Không thể tải bài viết.");
    }
  };

  const handleReact = async () => {
    if (!post || reacting) return;
    const previous = post;
    setReacting(true);
    setPost(nextPostPulseState(post));
    const res = await reactPost({ postId: post.id, reactionType: "UPVOTE" });
    setReacting(false);
    if (!res.ok) {
      setPost(previous);
      toast.error(res.message ?? "Không thể pulse bài viết.");
    }
  };

  const handleToggleBookmark = async () => {
    if (!post || bookmarkingPostIds.has(post.id)) return;

    const wasBookmarked = bookmarkedPostIds.has(post.id);
    setBookmarkingPostIds((prev) => new Set(prev).add(post.id));
    setBookmarkedPostIds((prev) => {
      const next = new Set(prev);
      if (wasBookmarked) next.delete(post.id);
      else next.add(post.id);
      return next;
    });

    const res = wasBookmarked
      ? await deleteBookmark(post.id)
      : await createBookmark(post.id);
    if (!res.ok) {
      setBookmarkedPostIds((prev) => {
        const next = new Set(prev);
        if (wasBookmarked) next.add(post.id);
        else next.delete(post.id);
        return next;
      });
      toast.error(res.message ?? "Không thể cập nhật bookmark.");
    }

    setBookmarkingPostIds((prev) => {
      const next = new Set(prev);
      next.delete(post.id);
      return next;
    });
  };

  const handleShare = () => {
    if (!post) return;
    navigator.clipboard.writeText(window.location.href);
    toast.success("Đã sao chép liên kết bài viết vào bộ nhớ tạm!");
  };

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-8 pt-24 px-6 lg:px-10">
        <AppSidebar active="home" />

        <div className="min-w-0">
          <button onClick={() => navigate(-1)} className="mb-5 flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-slate-900 dark:text-neutral-400 dark:hover:text-white">
          <ArrowLeft className="h-4 w-4" />
          Quay lại
          </button>
          <section className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-[#2a2a2a] dark:bg-[#1e1e1e]">
          {loading ? (
            <div className="flex justify-center py-20">
              <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
            </div>
          ) : !post ? (
            <div className="py-20 text-center text-slate-500 dark:text-neutral-500">Không tìm thấy bài viết.</div>
          ) : (
            <>
              <div className="mb-4 flex items-center gap-3">
                <div className="h-11 w-11 overflow-hidden rounded-full border border-slate-200 dark:border-neutral-800">
                  <SafeAvatar src={post.userAvatar} alt={post.username} />
                </div>
                <div>
                  <button onClick={() => navigate(`/profile/${post.username}`)} className="font-semibold text-slate-900 hover:underline dark:text-white">
                    {post.username}
                  </button>
                  <p className="text-xs text-slate-500 dark:text-neutral-400">{new Date(post.createdAt).toLocaleString("vi-VN")}</p>
                </div>
              </div>

              {post.content && post.content.trim() && (
                <p className="whitespace-pre-line text-[15px] leading-7 text-slate-800 dark:text-neutral-200">{post.content}</p>
              )}

              <div className="mt-3">
                <PostMedia urls={post.imageUrl ? post.imageUrl.split(",") : []} variant="feed" />
              </div>

              {/* ── Quoted original post for SHARE type ── */}
              {post.type === "SHARE" && (
                <DetailOriginalPostBlock originalPost={post.originalPost} />
              )}

              {post.topicSlugs?.length > 0 && (
                <div className="mt-4 flex flex-wrap gap-2">
                  {post.topicSlugs.map((topic) => (
                    <span key={topic} className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500 dark:bg-neutral-800 dark:text-neutral-400">
                      #{topic}
                    </span>
                  ))}
                </div>
              )}

              <div className="mt-5 flex items-center gap-8 border-t border-slate-100 pt-4 dark:border-neutral-800 select-none text-slate-500 dark:text-neutral-500">
                {/* Upvote */}
                <button
                  onClick={handleReact}
                  disabled={reacting}
                  className={`flex items-center gap-2 text-sm font-semibold hover:text-slate-900 dark:hover:text-white transition-colors group ${
                    post.myVote === 1
                      ? "text-slate-900 dark:text-white font-semibold"
                      : "text-slate-600 dark:text-neutral-400"
                  }`}
                >
                  <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                    <Activity className={`w-5 h-5 ${post.myVote === 1 ? "stroke-[2.5px]" : "stroke-2"}`} />
                  </div>
                  <span>{post.upvoteCount}</span>
                </button>

                {/* Comment */}
                <div className="flex items-center gap-2 text-sm font-semibold text-slate-600 dark:text-neutral-400">
                  <div className="p-1.5 rounded-full">
                    <MessageCircle className="w-5 h-5 stroke-2" />
                  </div>
                  <span>{commentCount}</span>
                </div>

                {/* Bookmark */}
                <button
                  onClick={handleToggleBookmark}
                  title={bookmarkedPostIds.has(post.id) ? "Bỏ lưu bài viết" : "Lưu bài viết"}
                  className={`flex items-center gap-2 hover:text-slate-900 dark:hover:text-white transition-colors group ${
                    bookmarkedPostIds.has(post.id)
                      ? "text-slate-900 dark:text-white"
                      : "text-slate-600 dark:text-neutral-400"
                  }`}
                >
                  <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                    <Bookmark className={`w-5 h-5 stroke-2 ${bookmarkedPostIds.has(post.id) ? "fill-current" : ""}`} />
                  </div>
                </button>

                {/* Share */}
                <button
                  onClick={handleShare}
                  title="Chia sẻ liên kết bài viết"
                  className="flex items-center gap-2 text-slate-600 hover:text-slate-900 dark:text-neutral-400 dark:hover:text-white transition-colors group"
                >
                  <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                    <Share2 className="w-5 h-5 stroke-2" />
                  </div>
                </button>
              </div>

              <CommentSection postId={post.id} initialCmtCount={commentCount} onCommentCountChange={setCommentCount} />
            </>
          )}
          </section>
        </div>
      </div>
    </div>
  );
}

// ─── DetailOriginalPostBlock ───────────────────────────────────────────────────
function DetailOriginalPostBlock({ originalPost }: { originalPost: OriginalPostData | null }) {
  if (!originalPost) {
    return (
      <div className="mt-2 mb-3 px-4 py-3 rounded-xl border border-dashed border-slate-300 dark:border-neutral-700 bg-slate-50 dark:bg-neutral-900/40 flex items-center gap-2 text-slate-400 dark:text-neutral-500">
        <Link className="w-4 h-4 shrink-0" />
        <span className="text-sm italic">Bài viết gốc không còn khả dụng.</span>
      </div>
    );
  }

  const imageUrls = originalPost.imageUrl
    ? originalPost.imageUrl.split(",").map((u) => u.trim()).filter(Boolean)
    : [];

  return (
    <div className="mt-2 mb-3 rounded-xl border border-slate-200 dark:border-neutral-700 bg-slate-50/60 dark:bg-neutral-900/30 overflow-hidden hover:border-slate-300 dark:hover:border-neutral-600 transition-colors">
      {/* Original post header */}
      <div className="flex items-center gap-2 px-4 pt-3 pb-1">
        <div className="w-7 h-7 rounded-full overflow-hidden bg-slate-100 dark:bg-neutral-800 shrink-0">
          <SafeAvatar src={originalPost.userAvatar} alt={originalPost.username ?? "user"} />
        </div>
        <span className="text-sm font-bold text-slate-700 dark:text-neutral-300 truncate">
          {originalPost.username ?? "Người dùng"}
        </span>
        <span className="text-xs text-slate-400 dark:text-neutral-500 shrink-0">
          · {timeAgo(originalPost.createdAt)}
        </span>
      </div>

      {/* Original post content */}
      {originalPost.content && (
        <p className="px-4 py-1 text-sm text-slate-700 dark:text-neutral-300 whitespace-pre-line break-words leading-relaxed line-clamp-5">
          {originalPost.content}
        </p>
      )}

      {/* Original post media */}
      {imageUrls.length > 0 && (
        <div className="px-4 pb-3 pt-1">
          <PostMedia urls={imageUrls} variant="feed" />
        </div>
      )}
      {!originalPost.content && imageUrls.length === 0 && (
        <p className="px-4 pb-3 text-sm text-slate-400 dark:text-neutral-500 italic">Không có nội dung.</p>
      )}
    </div>
  );
}
