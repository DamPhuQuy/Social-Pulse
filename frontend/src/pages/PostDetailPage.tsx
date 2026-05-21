import { useEffect, useState } from "react";
import { Activity, ArrowLeft, Loader2, MessageCircle } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import AppHeader from "@/components/social/AppHeader";
import AppSidebar from "@/components/social/AppSidebar";
import CommentSection from "@/components/comment/CommentSection";
import { SafeAvatar } from "@/components/ui/SafeAvatar";
import { reactPost } from "@/services/post/postService";
import { getPostDetail, type ViewPostResponse } from "@/services/social/postDetailService";
import { nextPostPulseState } from "@/lib/postUtils";

export default function PostDetailPage() {
  const navigate = useNavigate();
  const { postId } = useParams<{ postId: string }>();
  const [post, setPost] = useState<ViewPostResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [reacting, setReacting] = useState(false);
  const [commentCount, setCommentCount] = useState(0);

  useEffect(() => {
    if (!postId) return;
    loadPost(Number(postId));

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

              <p className="whitespace-pre-line text-[15px] leading-7 text-slate-800 dark:text-neutral-200">{post.content}</p>

              {post.topicSlugs?.length > 0 && (
                <div className="mt-4 flex flex-wrap gap-2">
                  {post.topicSlugs.map((topic) => (
                    <span key={topic} className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500 dark:bg-neutral-800 dark:text-neutral-400">
                      #{topic}
                    </span>
                  ))}
                </div>
              )}

              <div className="mt-5 flex items-center gap-6 border-t border-slate-100 pt-4 dark:border-neutral-800">
                <button onClick={handleReact} disabled={reacting} className="flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-slate-900 dark:text-neutral-400 dark:hover:text-white">
                  <Activity className={`h-4 w-4 ${post.myVote === 1 ? "stroke-[2.5px]" : "stroke-2"}`} />
                  {post.upvoteCount}
                </button>
                <div className="flex items-center gap-2 text-sm font-semibold text-slate-600 dark:text-neutral-400">
                  <MessageCircle className="h-4 w-4" />
                  {commentCount}
                </div>
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
