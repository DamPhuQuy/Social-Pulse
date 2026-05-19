import { useState, useEffect } from "react";
import { Loader2, MessageCircle, Send } from "lucide-react";
import { toast } from "sonner";
import { getMyProfile, type UserProfile } from "@/services/user/userService";
import CommentNode from "./CommentNode";
import { 
  type CommentResponse, 
  getTopLevelComments, 
  createComment 
} from "@/services/post/commentService";

interface CommentSectionProps {
  postId: number;
  initialCmtCount: number;
  onCommentCountChange?: (newCount: number) => void;
}

const COMMENTS_LIMIT = 10;

export default function CommentSection({ 
  postId, 
  initialCmtCount, 
  onCommentCountChange 
}: CommentSectionProps) {
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  
  // ─── Comments State ───────────────────────────────────────────────────────────
  const [comments, setComments] = useState<CommentResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [cmtCount, setCmtCount] = useState(initialCmtCount);

  // ─── Input Form State ─────────────────────────────────────────────────────────
  const [content, setContent] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load user profile and initial comments on mount
  useEffect(() => {
    getMyProfile().then((res) => {
      if (res.ok && res.data) {
        setCurrentUser(res.data);
      }
    });
    loadInitialComments();
  }, [postId]);

  useEffect(() => {
    setCmtCount(initialCmtCount);
  }, [initialCmtCount]);

  // Keep count synced
  useEffect(() => {
    if (onCommentCountChange) {
      onCommentCountChange(cmtCount);
    }
  }, [cmtCount, onCommentCountChange]);

  // ─── 1. LOAD INITIAL COMMENTS ─────────────────────────────────────────────────
  const loadInitialComments = async () => {
    setLoading(true);
    const res = await getTopLevelComments(postId, 0, COMMENTS_LIMIT);
    setLoading(false);
    if (res.ok && res.data) {
      setComments(res.data);
      setHasMore(res.data.length === COMMENTS_LIMIT);
    } else {
      toast.error(res.message || "Không thể tải bình luận.");
    }
  };

  // ─── 2. LOAD MORE COMMENTS (CURSOR PAGINATION) ────────────────────────────────
  const handleLoadMore = async () => {
    if (loadingMore || !hasMore) return;
    setLoadingMore(true);

    // Use the ID of the last (oldest) comment in our current list as lastId cursor
    const lastId = comments[comments.length - 1]?.id ?? 0;
    const res = await getTopLevelComments(postId, lastId, COMMENTS_LIMIT);
    
    setLoadingMore(false);
    if (res.ok && res.data) {
      setComments((prev) => [...prev, ...(res.data || [])]);
      setHasMore(res.data.length === COMMENTS_LIMIT);
    } else {
      toast.error(res.message || "Lỗi khi tải thêm bình luận.");
    }
  };

  // ─── 3. SUBMIT NEW TOP-LEVEL COMMENT ──────────────────────────────────────────
  const handleSubmitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitting || !content.trim()) return;

    setIsSubmitting(true);
    const res = await createComment(postId, content.trim(), null);
    setIsSubmitting(false);

    if (res.ok && res.data) {
      setContent("");
      
      // Update count
      setCmtCount((prev) => prev + 1);

      // The creation endpoint returns the saved comment payload, so prepend it without refetching.
      const newComment: CommentResponse = {
        id: res.data.id,
        user: {
          id: currentUser?.userId || 0,
          username: currentUser?.username || "me",
          avatarUrl: currentUser?.avatarUrl || null,
        },
        content: res.data.content,
        createdAt: res.data.createdAt,
        edited: false,
        upvoteCount: 0,
        downvoteCount: 0,
        replyCount: 0,
        myReaction: null,
      };

      setComments((prev) => [newComment, ...prev]);
    } else {
      toast.error(res.message || "Không thể đăng bình luận.");
    }
  };

  return (
    <div className="w-full mt-4 pt-4 border-t border-slate-100 dark:border-neutral-800/80 flex flex-col gap-4 animate-fadeIn">
      {/* 1. INPUT BOX (Current User Avatar + Form) */}
      <form 
        onSubmit={handleSubmitComment}
        className="flex items-end"
      >
        <div className="w-full bg-slate-50 dark:bg-neutral-900 border border-slate-100 dark:border-neutral-800/80 rounded-2xl p-2.5 flex items-end gap-2 focus-within:border-slate-300 dark:focus-within:border-neutral-700 transition-all duration-200">
          <textarea
            rows={1}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Chia sẻ suy nghĩ của bạn..."
            className="flex-1 resize-none bg-transparent border-0 focus:ring-0 text-slate-700 dark:text-neutral-200 text-sm py-1 px-2 focus:outline-none placeholder-slate-400 dark:placeholder-neutral-600 max-h-28 overflow-y-auto"
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                handleSubmitComment(e);
              }
            }}
          />
          <button
            type="submit"
            disabled={isSubmitting || !content.trim()}
            className="w-8 h-8 rounded-xl bg-slate-900 dark:bg-white text-white dark:text-slate-900 flex items-center justify-center transition-all duration-200 hover:scale-105 active:scale-95 disabled:opacity-50 disabled:scale-100 disabled:cursor-not-allowed shrink-0"
          >
            {isSubmitting ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Send className="w-4 h-4 stroke-[2px]" />
            )}
          </button>
        </div>
      </form>

      {/* 2. COMMENTS LIST */}
      <div className="flex flex-col gap-4 mt-2">
        {loading ? (
          <div className="flex justify-center items-center py-8">
            <Loader2 className="w-6 h-6 animate-spin text-slate-400 dark:text-neutral-600" />
          </div>
        ) : comments.length === 0 ? (
          /* Empty State */
          <div className="flex flex-col items-center justify-center py-10 px-4 text-center">
            <MessageCircle className="w-8 h-8 text-slate-300 dark:text-neutral-700 stroke-[1.5px] mb-2" />
            <p className="text-sm font-medium text-slate-500 dark:text-neutral-500">Chưa có bình luận nào</p>
            <p className="text-xs text-slate-400 dark:text-neutral-600 mt-0.5">Hãy là người đầu tiên chia sẻ cảm nghĩ của bạn!</p>
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            {comments.map((comment) => (
              <CommentNode
                key={comment.id}
                postId={postId}
                comment={comment}
                depth={0}
                currentUser={currentUser}
                onCommentDeleted={(deletedId) => {
                  setComments((prev) => prev.filter((c) => c.id !== deletedId));
                  setCmtCount((prev) => Math.max(0, prev - 1));
                }}
                onCommentCountDelta={(delta) => {
                  setCmtCount((prev) => Math.max(0, prev + delta));
                }}
              />
            ))}
          </div>
        )}

        {/* LOAD MORE BUTTON */}
        {hasMore && !loading && (
          <button
            onClick={handleLoadMore}
            disabled={loadingMore}
            className="py-2.5 px-4 text-xs font-semibold text-slate-600 dark:text-neutral-400 bg-slate-50 hover:bg-slate-100 dark:bg-neutral-900/50 dark:hover:bg-neutral-900 rounded-xl transition-all duration-200 flex items-center justify-center gap-2 border border-slate-100 dark:border-neutral-800/40 mt-2 hover:scale-[1.01]"
          >
            {loadingMore && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            <span>Tải thêm bình luận</span>
          </button>
        )}
      </div>
    </div>
  );
}
